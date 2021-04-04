package lava4.db;
import lava4.MethodInfo;

/**
* A Method is more than just code.  It has the method name and params.
* I could add more fields too if needed, like return type
* The primary key here is the nameId, so this can be seen as an extension of the Name table.
*
* I renamed this MethodRow to distinguish it from org.apache.bcel.classfile.Method
*/
public class MethodRow implements MethodInfo {

	//fields
	//I am not making these final to make it easier to use
	public int nid;				//this is the same as the name_id
	public int class_id;			//same as in name table
	public boolean is_static;		//same as in name table
	public int params;
	public byte[] code;

	public MethodRow() {}

	public MethodRow(int nid,int class_id,boolean is_static,byte params,byte[] code) {
		this.nid=nid;
		this.class_id=class_id;
		this.is_static=is_static;
		this.params=params;
		this.code=code;
	}

	/**
	* get the name id of this method
	*/
	public int getNameId() {return nid;}

	/**
	* Get the class id this method is a part of
	*/
	public int getClassId() {return class_id;}

	/**
	* get the number of params. Params must be in the range 0..3
	* We know the classid because it is associated with the name
	*/
	public int params() {return params;}

	public boolean isStatic() {return is_static;}

	public int getStatic() {
		return is_static? 1 : 0;
	}

	public void setStatic(int i) {
		if (i!=0) {is_static=true;} else {is_static=false;}
	}

	/**
	* Get the byte code. This is the same code that is stored in the classfile, I don't
	* prepend the number of params to it.
	*/
	public byte[] getCode() {return code;}

}