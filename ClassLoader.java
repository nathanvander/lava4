package lava4;
import java.io.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.Type;
import lava4.db.*;

/**
* This loads one class at a time.
* All the names, which are field and method names are stored in the name table,
* and associated with the class in the ItemNameHasValue table.
*
* External classes, fields and methods are saved in the Item and Name tables.
*/

public class ClassLoader {
	//public final static byte CONSTANT_Fieldref = (byte)9;
	//public final static byte CONSTANT_Methodref = (byte)10;
	public static String MAIN = "main:([Ljava/lang/String;)V";
	public static String CLINIT = "<clinit>:()V";
	public static String INIT= "<init>:()V";

	boolean debug;
	String dir;

	//for testing
	public static void main(String[] args) throws IOException {
		ClassLoader loader = new ClassLoader(true);
		loader.loadClass(args[0]);
	}

	public ClassLoader(boolean debug) {
		Database.init();
		this.debug=debug;
		//get dir
		dir = System.getProperty("dir");
	}

	public void log(String s) {
		if (debug) System.out.println(s);
	}

	//replace the periods with slashes
	public static String canonicalName(String className) {
		return className.replace('.', '/');
	}

	/**
	* loadClass.  pass in the class name, without the .class extension.
	* This must either be in the same directory, or relative to a directory specified
	* by the -Ddir system property.
	*/
	public void loadClass(String classFileName) throws IOException {
		String cf = null;
		if (dir!=null) {
			cf = dir + canonicalName(classFileName) + ".class";
		} else {
			cf = canonicalName(classFileName) + ".class";
		}

		log("loading class file "+cf);
		ClassParser classp = new ClassParser(cf);
		JavaClass jclass = null;
		try {
			jclass = classp.parse();
		} catch(ClassFormatException x) {
			throw new IOException("ClassFormatException: "+x.getMessage());
		}
		//get the classname
		String jcname = jclass.getClassName();
		String cname = canonicalName(jcname);
		int cid=Database.Item.newClass(cname);
		log("stored class "+cname+" as "+cid);

		//register the main method name
		String mainName = cname + ":" + MAIN;
		int nid=Database.Name.newName(Database.T_METHOD,cid,mainName,true);
		log("stored name "+mainName+" as "+nid);

		//register the clinit method
		String clinitName = cname + ":" + CLINIT;
		int nid2=Database.Name.newName(Database.T_METHOD,cid,clinitName,true);
		log("stored name "+clinitName+" as "+nid2);

		//register the init method name
		String initName = cname + ":" + INIT;
		int nid3=Database.Name.newName(Database.T_METHOD,cid,initName,false);
		log("stored name "+initName+" as "+nid3);

		//load the method and fields before the constant pool
		//because if they are local we have more information about them, like if they are static

		//register native names
		//add all of the native names
		Native.registerNames();

		//load methods
		loadMethods(cname,cid,jclass);

		loadFields(cname,cid,jclass);

		//load constantpool
		loadConstantPool(cname,cid,jclass.getConstantPool());

		//dump what we have
		System.out.println(Database.Item.dump() );
		System.out.println(Database.Name.dump() );
		System.out.println(Database.MethodTable.dump() );
		System.out.println(Database.ItemNameHasValue.dump() );
	}

	public void loadConstantPool(String claz,int cid,ConstantPool cpool) {
		String str = null;
		Cell cell = null;
		int class_index = 0;
		int natx = 0;
		String full_name = null;

		//start at 1
		for (int ix=1;ix<cpool.getLength();ix++) {
			Constant k = cpool.getConstant(ix);
			if (k==null) continue;
			byte tag=k.getTag();
			switch(tag) {
				case 3:		//int
					//store this as a class constant
					ConstantInteger ci = (ConstantInteger)k;
					int i = ci.getBytes();
					cell = new Cell(i);
					//store it
					Database.store(cid,ix,Database.T_INT,cell);
					break;
				case 4:		//float
					//it would be easy to handle this but I want to focus on other things first
					ConstantFloat cf = (ConstantFloat)k;
					float f = cf.getBytes();
					log(ix+": float ("+f+")- skipping");
					break;
				case 7:		//class
					ConstantClass cc=(ConstantClass)k;
					str=cc.getBytes(cpool);
					//this should have the slashes in it
					int ncid=Database.Item.newClass(str);
					cell = new Cell(ncid);
					//store it
					Database.store(cid,ix,Database.T_CLASS,cell);
					break;
				case 8:		//string
					ConstantString cs=(ConstantString)k;
					str=cs.getBytes(cpool);
					cell = new Cell(str);
					Database.store(cid,ix,Database.T_STRING,cell);
					break;
				case 9:		//field
					ConstantFieldref cfr = (ConstantFieldref)k;
					class_index = cfr.getClassIndex();
					natx = cfr.getNameAndTypeIndex();
					full_name = getFullName(cpool,class_index,natx);
					//1. assign a CID to the fieldname class, if it doesn't already exist
					ConstantClass myClass=(ConstantClass)cpool.getConstant(class_index);
					String fcname=myClass.getBytes(cpool);
					int nfcid=Database.Item.newClass(fcname);
					//2. create a name.  We don't know if it is static or not
					int nid = Database.Name.newName(Database.T_FIELD,nfcid,full_name,false);
					//3. put it in the pool
					cell = new Cell(nid);
					Database.store(cid,ix,Database.T_FIELD,cell);
					break;
				case 10:	//methodref
					ConstantMethodref cmr = (ConstantMethodref)k;
					class_index = cmr.getClassIndex();
					natx = cmr.getNameAndTypeIndex();
					full_name = getFullName(cpool,class_index,natx);
					//1. assign the cid, if it doesn't already exist
					ConstantClass myClass2=(ConstantClass)cpool.getConstant(class_index);
					String mcname=myClass2.getBytes(cpool);
					int nmcid=Database.Item.newClass(mcname);
					//2. create a name
					int nid2 = Database.Name.newName(Database.T_METHOD,nmcid,full_name,false);
					//3. put it in the pool
					cell = new Cell(nid2);
					Database.store(cid,ix,Database.T_METHOD,cell);
					break;
				case 11:	//ifacemethodref
					//store method name
					//ConstantInterfaceMethodref cimr = (ConstantInterfaceMethodref)k;
					//ignore for now
					//this would be easy to do
					log(ix+": iface method ref - skipping");
					break;
				case 1: //log(ix+": utf8");
					break;
				case 12: //log(ix+": cnat");
					break;
				default: log(ix+": "+tag);
			}
		}
	}

	public String getFullName(ConstantPool cpool,int cx,int natx) {
		//get the class of the reference - it might be this one or could be different
		ConstantClass myClass=(ConstantClass)cpool.getConstant(cx);
		String cname=myClass.getBytes(cpool);
		ConstantNameAndType myCnat=(ConstantNameAndType)cpool.getConstant(natx);
		String fname=myCnat.getName(cpool);
		String fsig=myCnat.getSignature(cpool);
		String complete_name = cname + ":" + fname + ":" + fsig;
		return complete_name;
	}

	//-------------------------------------------

	//claz is the class name with slashes
	public void loadMethods(String claz,int cid,JavaClass jclass) {
		Method[] ma = jclass.getMethods();
		for (int i=0;i<ma.length;i++) {
			Method m = ma[i];

			//get the name
			String mname = m.getName();

			//get the descriptor. BCEL calls it the signature
			String sig = m.getSignature();
			//similar to getFullName method above
			String complete_name = claz + ":" +mname + ":"+sig;
			log("creating method "+complete_name);

			//all we need is the name id
			int nid = Database.Name.getNameId(complete_name);
			if (nid==0) {
				nid = Database.Name.newName(Database.T_METHOD,cid,complete_name,m.isStatic());
				log("name "+complete_name+" added as "+nid);
			}

			int params = m.getArgumentTypes().length;

			//---------------
			//init has 1 param
			if (mname.equals("<init>")) {
				params = 1;
			}
			//--------------
			log(complete_name+" has "+params+" params");
			byte[] mcode = m.getCode().getCode();

			//now store it in the method table
			MethodRow mr = new MethodRow(nid, cid,m.isStatic(),(byte)params,mcode);
			int mid = Database.MethodTable.newMethod( (MethodInfo)mr);
			log("inserted method "+complete_name+" as mid "+mid);
		}
	}

	//all we want to do is load the field names into the names table
	public void loadFields(String claz,int cid,JavaClass jclass) {
		Field[] fa = jclass.getFields();
		for (int i=0;i<fa.length;i++) {
			Field f = fa[i];

			String fname = f.getName();
			String sig = f.getSignature();

			//similar to getFullName method above
			String complete_name = claz + ":" +fname + ":"+sig;
			log("creating method "+complete_name);

			//all we need is the name id
			int nid = Database.Name.getNameId(complete_name);
			if (nid==0) {
				nid = Database.Name.newName(Database.T_FIELD,cid,complete_name,f.isStatic());
				log("name "+complete_name+" added as "+(int)nid);
			}
		}
	}
}
