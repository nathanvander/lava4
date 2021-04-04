package lava4;
import java.io.IOException;

/**
* Run this instead of Java.  The difference is that this expects a system property 'dir' to be set on the command
* line.  Example: lava -Ddir=c:\java\projects\
* If this is not set, then it expects the classfile to be in the same directory and not in a package.
*/
public class Lava {
	public final static int version=4;

	public static void main(String[] args) throws IOException {
		System.out.println("Lava version: "+version);
		System.out.println("SQLite version: "+lava4.db.Connection.libversion_number());

		String classname = args[0];
		String[] args2=null;
		if (args.length>1) {
			args2 = new String[args.length-1];
			System.arraycopy(args,1,args2,0,args2.length);
		}
		ControlUnit control = new ControlUnit(true);
		control.start(classname, args2);
	}
}