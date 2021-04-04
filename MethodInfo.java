package lava4;

/**
* MethodInfo consists of the bytecode and other surrounding information which
* is enough to invoke the code
*/
public interface MethodInfo {

	/**
	* get the name id of this method.  This is the primary key
	*/
	public int getNameId();

	/**
	* Get the class id this method is a part of
	*/
	public int getClassId();

	/**
	* get the number of params. Params must be in the range 0..3
	*/
	public int params();

	/**
	* Return whether the method is static.
	*/
	public boolean isStatic();
	//return 1 if true, 0 if false
	public int getStatic();
	/**
	* Get the byte code. This is the same code that is stored in the classfile, I don't
	* prepend the number of params to it.
	*/
	public byte[] getCode();
}