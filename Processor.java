package lava4;
import lava4.db.Database;

/**
* In computing, a processor or processing unit is a digital circuit which performs operations on some external
* data source, usually memory or some other data stream.[1] It typically takes the form of a microprocessor,
* which can be implemented on a single metal–oxide–semiconductor integrated circuit chip.
*
* Arithmetic Logic Unit (ALU)
* In computing, an arithmetic logic unit (ALU) is a combinational digital circuit that performs arithmetic and bitwise
* operations on integer binary numbers.
*/
public class Processor {
	public static String MAIN = "main:([Ljava/lang/String;)V";
	public static String CLINIT = "<clinit>:()V";
	BlackBox box;
	boolean debug;

	public Processor(BlackBox box,boolean d) {
		this.box=box;
		debug = d;
	}

	public void log(String s) {
		if (debug) System.out.println(s);
	}

	//----------------------------------------
	//do comparison first and get it out of the way
	//This is very clever:
	//	GT = 1
	//	EQ = 2
	//	LT = 4
	//all the others are just combinations
	public static int compareCode(String compare) {
		if (compare==null) return 0;
		switch (compare) {
			case "EQ":
			case "==": return 2;
			case "NE":
			case "NEQ":
			case "!=": return 5;
			case "LT":
			case "<": return 4;
			case "LE":
			case "LTE":
			case "<=": return 6;
			case "GT":
			case ">": return 1;
			case "GE":
			case "GTE":
			case ">=": return 3;
			default: return 0;
		}
	}

	//code must be a number from 0..6 as given above
	public static boolean ICMP(int a, int code,int b) {
		//System.out.println("ICMP: a="+a+", code="+code+", b="+b);
		int c = a - b;
		int t = 0;
		if (c>0) t=1;
		else if (c==0) t=2;
		//c must be less than 0, but no need to check
		else t=4;

		int d = t & code;
		return d > 0;
	}

	public void IFNULL(short rel) {
		Cell a = box.POP();
		if (a==null || a.toChar()==(char)0) {
			IF(true,rel);
		}
	}

	//do a relative branch
	//do the adjustment here
	//This is also used for a straight GOTO, just pass it a TRUE
	public void IF(boolean b,short rel) {
		if (b) box.JUMP((short)(rel - 3));
	}

	//cmp is the comparison.  Use "EQ" or other similar symbols
	public void IF_ICMP(String cmp,short relativeJump) {
		//System.out.println("IF_ICMP: cmp="+cmp);
		int b = box.IPOP();
		int a = box.IPOP();
		int cc = compareCode(cmp);
		IF( ICMP(a,cc,b), relativeJump);
	}

	/**
	* IF_ICMPZ
	* same as ICMP but you compare to zero
	*/
	public void IF_ICMPZ(String cmp,short relativeJump) {
		int b = 0;
		int a = box.IPOP();
		int cc = compareCode(cmp);
		IF( ICMP(a,cc,b), relativeJump);
	}

	//----------------------------------------------
	/**
	* LDC: Load Constant (0x12).  This is usually an int or String
	* but this actually could be a long or double or something else
	*/
	public void LDC(byte index) {
		int cid = box.getClassId();
		Cell c = Database.load(cid,(int)index);
		box.PUSH(c);
	}
	//-------------------------------------------

	//(byte)0x78;			//120
	public void ISHL() {
		int b = box.IPOP();
		int a = box.IPOP();
		box.IPUSH(a << b);
	}

	//(byte)0x7A;			//122
	public void ISHR() {
		int b = box.IPOP();
		int a = box.IPOP();
		box.IPUSH(a >> b);
	}

	//(byte)0x7C;			//124
	public void IUSHR() {
		int b = box.IPOP();
		int a = box.IPOP();
		box.IPUSH(a >>> b);
	}

	//(byte)0x7E;			//126
	public void IAND() {
		int b = box.IPOP();
		int a = box.IPOP();
		box.IPUSH(a & b);
	}

	//(byte)0x80;			//128
	public void IOR() {
		int b = box.IPOP();
		int a = box.IPOP();
		box.IPUSH(a | b);
	}

	//logical exclusive or
	//(byte)0x82;			//130
	public void IXOR() {
		int b = box.IPOP();
		int a = box.IPOP();
		box.IPUSH(a ^ b);
	}

	/**
	* IADD: 0x60
	*/
	public void IADD() {
		int b = box.IPOP();
		int a = box.IPOP();
		box.IPUSH(a + b);
	}

	public void ISUB() {
		int b = box.IPOP();
		int a = box.IPOP();
		box.IPUSH(a - b);
	}

	public void IMUL() {
		int b = box.IPOP();
		int a = box.IPOP();
		box.IPUSH(a * b);
	}

	public void IDIV() {
		int b = box.IPOP();
		int a = box.IPOP();
		box.IPUSH(a / b);
	}

	public void IREM() {
		int b = box.IPOP();
		int a = box.IPOP();
		box.IPUSH(a % b);
	}

	public void INEG() {
		int a = box.IPOP();
		box.IPUSH( 0 - a);
	}

	/**
	* IINC (0x84)
	* increment local variable #index by signed byte const k
	* k is usually 1, but it could be any byte value including negative numbers.
	*/
	public void IINC(byte localn,byte k) {
		//we actually can handle any number of locals, but I don't know why you would need more than 8
		if (localn<0 || localn>7) throw new IllegalStateException("local variable "+localn+" requested");
		Cell c=box.LOAD((int)localn);
		//increment it in place, we don't need to restore it
		if(k==1) {
			c.increment();
		} else {
			c.add((short)k);
		}
	}


	//load a reference onto the stack from local variable n
	public void ALOAD(int localn) {
		//log("aload "+localn);
		Cell data = box.LOAD(localn);
		box.PUSH( data );
		//log("pushing "+data+" on to stack");
	}

	//store a reference into local variable n
	public void ASTORE(int localn) {
		//log("astore "+localn);
		box.STORE( localn, box.POP());
	}

	//static values are stored with the classid and nameid
	public void GETSTATIC(char index) {
		//get the classid and nameid
		int cid = box.getClassId();
		Cell cnid = Database.load(cid, (int)index);

		//now get the value
		Cell d = Database.getStatic(cid,cnid.toInt());
		//log("trying to get the static value of "+ cnid.toString());
		if (d==null) {
			log(" the value is null");
		} else {
			log("the value is "+d.toString());
		}
		//and push it on the stack
		box.PUSH( d );
	}

	public void PUTSTATIC(char index) {
		//get the classid and nameid
		int cid = box.getClassId();
		Cell cnid = Database.load(cid,(int)index);

		//now get the value from the stack
		Cell d = box.POP();

		//and store it
		//we don't know the type
		Database.putStatic(cid,cnid.toInt(),0,d);
		log("the static value of "+cnid.toInt()+" is now "+d.toString());
	}

	public void GETFIELD(char index) {
		int cid = box.getClassId();
		Cell cnid = Database.load(cid, (int)index);

		//get the value
		Cell oref = box.POP();
		Cell val = Database.load(oref.toInt(),cnid.toInt());

		//and push it on the statck
		box.PUSH( val );
	}

	public void PUTFIELD(char index) {
		int cid = box.getClassId();
		Cell cnid = Database.load(cid, (int)index);

		//get the value from the stack
		Cell d = box.POP();

		//store it
		Cell oref = box.POP();
		Database.store(oref.toInt(),cnid.toInt(),0,d);
	}

	//arrayref, index -> value
	//this also works with IALOAD
	public void AALOAD() {
		int index = box.IPOP();
		Cell aref = box.POP();
		//log("Machine.AALOAD: aref = "+aref+"; index = "+index);
		Cell val=Database.arrayLoad(aref.toInt(),index);
		//log("Machine.AALOAD: pushing '"+val+"' on to stack");
		box.PUSH(val);
	}

	//arrayref, index, value -> nil
	//store a value into an array
	public void AASTORE() {
		Cell val = box.POP();
		int index = box.IPOP();
		Cell aref = box.POP();
		Database.arrayStore(aref.toInt(),index,0,val);
	}

	public void ALEN() {
		Cell aref = box.POP();
		//length is purposely crippled in the version.  max of 255
		int len = Database.Item.getLength(aref.toInt());
		box.IPUSH( len);
	}

	//create a new array of references of length count and component type identified
	//by the class reference index (indexbyte1 << 8 | indexbyte2) in the constant pool
	public void ANEWARRAY(char index) {
		//get the class
		int cid = box.getClassId();
		Cell arrayClassId = Database.load(cid, (int)index);
		//put debug here
		//get count
		int count = box.IPOP();

		//create new array.  We ignore the array type for now
		int aref= Database.Item.newArray(  Database.T_OBJ_ARRAY,count);
		log("Processor: created item "+aref+" for array type "+arrayClassId+" with length "+count);

		//push it
		box.PUSH(new Cell(aref));
	}

	//count -> arrayref	create new array with count elements of primitive type identified by atype
	//I ignore the type. this always makes an int array
	public void NEWARRAY(byte atype) {
		int count = box.IPOP();
		int aref = Database.Item.newArray( Database.T_INT_ARRAY,count);
		log("Processor: created item "+aref+" for primitive array type "+(int)atype+" with length "+count);
		box.PUSH( new Cell(aref));
	}

	/**
	* This needs additional debugging.
	*/
	public void NEWOBJ(char index) {
		log("Processor.NEWOBJ: index = "+(int)index);
		int cid = box.getClassId();
		//debug - get classname of frame, not necessarily the same class name of the object
		String frameClassName = Database.Item.getClassName(cid);
		log("Processor.NEWOBJ: frame class name = "+frameClassName);
		//get the method name
		String frameMethodName = box.getMethodName();
		log("Processor.NEWOBJ: frame method name = "+frameMethodName);

		Cell objClass = Database.load(cid, index);
		if (objClass==null) {
			log("Processor.NEWOBJ: no such class at "+cid+" #"+index);
		}
		String objClassName = Database.Item.getClassName(objClass.toInt());
		log("Processor.NEWOBJ: object class name = "+objClassName);
		int oref = Database.Item.newObject(objClass.toInt());
		log("Processor.NEWOBJ: created new object "+oref);
		box.PUSH( new Cell(oref));
	}

	//returns the current level
	//if this is 0 then the program is completed
	public int RETURNV() {
		return box.RETURNV();
	}

	//return a value.  Also works with IRETURN
	public int ARETURN() {
		return box.ARETURN();
	}

	/**
	* invoke_static, invoke_virtual, and invoke_special all operate the same way.
	* The only difficulty is determining if we have the code for it or if we emulate it.
	*
	*/
	public void INVOKE(char index) {
		int cid = box.getClassId();
		Cell cmid = Database.load(cid,(int)index);
		MethodInfo m= Database.MethodTable.getMethod( cmid.toInt());
		if (m!=null) {
			box.INVOKE(m);
		} else {
			boolean b = Native.emulate(cmid.toInt(),box);
			//try to load it?
		}
	}

	//this has the code to start the main frame
	//the args are stored in a string array and loaded into param1
	public void MAIN_FRAME(String className,String[] args) {
		if (args!=null) {
			//create a new array - the type doesn't matter
			int saref = Database.Item.newArray(Database.T_STR_ARRAY, args.length);
			//store the strings in it
			for (int i=0;i<args.length;i++) {
				Cell cstr = new Cell(args[i]);
				Database.store(saref,i,Database.T_STRING,cstr);
			}
			//put it on the stack
			box.PUSH( new Cell(saref));
			//it will be taken off the stack and put into a param later
		} else {
			box.PUSH(Cell.NIL);
		}

		//get the method name
		String cn = ClassLoader.canonicalName(className);
		String mainName = cn + ":" + MAIN;
		int mainid = Database.Name.getNameId(mainName);
		if (mainid==0) {
			System.out.println("unable to find method "+mainName);
		} else {
			MethodInfo m = Database.MethodTable.getMethod(mainid);
			box.INVOKE(m);
		}
	}

	/**
	* Run the static class initializer.  This has the name <clinit>
	* Return true if the static initializer is found and loaded on to the framestack.
	* Return false if not found.
	*/
	public boolean CLASS_STATIC_INIT(String className) {
		String cn = ClassLoader.canonicalName(className);
		String clinitName = cn + ":" + CLINIT;
		int clinitid = Database.Name.getNameId(clinitName);
		if (clinitid==0) {
			System.out.println("unable to find method "+clinitName);
			return false;
		} else {
			MethodInfo m = Database.MethodTable.getMethod(clinitid);
			if (m==null || m.getNameId()==0) {
				return false;
			} else {
				box.INVOKE(m);
				return true;
			}
		}
	}

	/**
	* checks whether an objectref is of a certain type, the class reference of which is in the constant pool at index
	*
	* This is a NOP because we don't do anything. But we do look at the classes.
	*/
	public void CHECKCAST(char index) {
		Cell oref = box.POP();
		//what is the class of this object?
		int objcid = Database.Item.getClassId(oref.toInt());
		//what is the class name
		String objClassName = Database.Item.getClassName(objcid);

		//now what class do we want to check
		int cid = box.getClassId();
		Cell xcid = Database.load(cid,(int)index);
		String checkClassName = Database.Item.getClassName(xcid.toInt());

		log("CHECKCAST: object is of type "+objClassName+", checking to see if it is of type "+checkClassName);
		box.PUSH(oref);
	}
}