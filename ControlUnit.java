package lava4;
import java.io.IOException;

/**
* The control unit (CU) is a component of a computer's central processing unit (CPU) that directs the operation
* of the processor. It tells the computer's memory, arithmetic logic unit and input and output devices how to
* respond to the instructions that have been sent to the processor.
*
* This is the brains of the unit. It does the Fetch, Decode, Execute cycle.
* The trick is that it asks BlackBox for the next opcode. BlackBox in turn gets the instruction pointer from the
* current Frame.
*/
public class ControlUnit implements OpCodes {
	ClassLoader cloader;
	BlackBox box;
	Processor m;							//for machine
	boolean debug;
	boolean running=false;

	public ControlUnit(boolean debug) {
		box = new BlackBox(debug);
		m = new Processor(box,debug);
		cloader = new ClassLoader(debug);
		this.debug=debug;
	}

	public void log(String s) {
		if (debug) System.out.println(s);
	}

	public BlackBox getBox() {return box;}

	public Processor getProcessor() {return m;}

	public ClassLoader getClassLoader() {return cloader;}

	public void setRunning(boolean b) {running=b;}

	/**
	* start. This loads the code and runs it.
	* The classname may be in a package. It must not end in a .class (which is added by the class loader)
	*/
	public void start(String className,String[] args) throws IOException {
		cloader.loadClass(className);

		//run the static class initializer if any
		running = m.CLASS_STATIC_INIT(className);
		run();
		//this program will end

		//now load the main method
		m.MAIN_FRAME(className,args);
		running = true;
		run();
	}

	/**
	* given 2 byte values, combine them and return an unsigned char.
	*/
	public static char charIndex(byte b1,byte b2) {
		return (char)(b1 << 8 | b2);
	}

	/**
	* given 2 byte values, combine them and return a signed short
	*/
	public static short shortIndex(byte b1,byte b2) {
		int i2 = b2;
		if (i2<0) i2 = i2+256;
		return (short)(b1 * 256 + i2);
	}

	public byte NEXT() { return box.NEXT();}

	public void run() {
		byte op = (byte)0;
		byte index1=(byte)0;
		byte index2=(byte)0;
		short sidx = (short)0;
		char cidx = (char)0;
		int lev = 0;

		while (running) {
			//fetch the next byte
			op = NEXT();
			//log("ControlUnit: op ="+ (int)op);
			switch(op) {
				//load numbers on to stack
				case BIPUSH: index1=NEXT(); box.IPUSH((int)index1); break;
				case SIPUSH:
					index1=NEXT(); index2=NEXT(); sidx = shortIndex(index1,index2);
					box.IPUSH((int)sidx);
					break;
				case LDC: index1=NEXT(); m.LDC(index1); break;
				case ICONST_M1: box.IPUSH(-1); break;
				case ICONST_0: box.IPUSH(0); break;
				case ICONST_1: box.IPUSH(1); break;
				case ICONST_2: box.IPUSH(2); break;
				case ICONST_3: box.IPUSH(3); break;
				case ICONST_4: box.IPUSH(4); break;
				case ICONST_5: box.IPUSH(5); break;
				case DUP: box.DUP(); break;

				//logic
				case ISHL: m.ISHL(); break;
				case ISHR: m.ISHR(); break;
				case IUSHR: m.IUSHR(); break;
				case IAND: m.IAND(); break;
				case IOR: 	m.IOR(); break;
				case IXOR: m.IXOR(); break;

				//math
				case IADD: m.IADD(); break;
				case ISUB: m.ISUB(); break;
				case IMUL: m.IMUL(); break;
				case IDIV: m.IDIV(); break;
				case IREM: m.IREM(); break;
				case IINC: index1=NEXT(); index2=NEXT(); m.IINC(index1,index2); break;

				//temporarily store and load local variables
				case ILOAD: index1=NEXT(); m.ALOAD((int)index1); break;
				case ILOAD_0: m.ALOAD(0); break;
				case ILOAD_1: m.ALOAD(1); break;
				case ILOAD_2: m.ALOAD(2); break;
				case ILOAD_3: m.ALOAD(3); break;
				case ALOAD: index1=NEXT(); m.ALOAD((int)index1); break;
				case ALOAD_0: m.ALOAD(0); break;
				case ALOAD_1: m.ALOAD(1); break;
				case ALOAD_2: m.ALOAD(2); break;
				case ALOAD_3: m.ALOAD(3); break;
				case ISTORE: index1=NEXT(); m.ASTORE((int)index1); break;
				case ISTORE_0: m.ASTORE(0); break;
				case ISTORE_1: m.ASTORE(1); break;
				case ISTORE_2: m.ASTORE(2); break;
				case ISTORE_3: m.ASTORE(3); break;
				case ASTORE: index1=NEXT(); m.ASTORE((int)index1); break;
				case ASTORE_0: m.ASTORE(0); break;
				case ASTORE_1: m.ASTORE(1); break;
				case ASTORE_2: m.ASTORE(2); break;
				case ASTORE_3: m.ASTORE(3); break;

				//static variables
				case GETSTATIC:
					index1=NEXT(); index2=NEXT(); cidx = charIndex(index1,index2);
					m.GETSTATIC(cidx);
					break;
				case PUTSTATIC: index1=NEXT(); index2=NEXT(); m.PUTSTATIC( charIndex(index1,index2)); break;

				//object fields
				case GETFIELD: index1=NEXT(); index2=NEXT(); m.GETFIELD( charIndex(index1,index2)); break;
				case PUTFIELD: index1=NEXT(); index2=NEXT(); m.PUTFIELD( charIndex(index1,index2)); break;

				//arrays and objects
				case IALOAD: m.AALOAD(); break;
				case IASTORE: m.AASTORE(); break;
				case AALOAD: m.AALOAD(); break;
				case AASTORE: m.AASTORE(); break;
				case ARRAYLENGTH: m.ALEN(); break;
				case ANEWARRAY: index1=NEXT(); index2=NEXT(); m.ANEWARRAY( charIndex(index1,index2)); break;
				case NEWARRAY: index1=NEXT(); m.NEWARRAY(index1); break;
				case NEWOBJ: index1=NEXT(); index2=NEXT(); m.NEWOBJ( charIndex(index1,index2)); break;

				//control flow
				case JMP: index1=NEXT(); index2=NEXT(); m.IF(true, shortIndex(index1,index2) ); break;
				case IF_ICMPEQ: index1=NEXT(); index2=NEXT();  m.IF_ICMP("EQ",shortIndex(index1,index2) ); break;
				case IF_ICMPGE: index1=NEXT(); index2=NEXT();  m.IF_ICMP("GE",shortIndex(index1,index2) ); break;
				case IF_ICMPGT: index1=NEXT(); index2=NEXT();  m.IF_ICMP("GT",shortIndex(index1,index2) ); break;
				case IF_ICMPLE: index1=NEXT(); index2=NEXT();  m.IF_ICMP("LE",shortIndex(index1,index2) ); break;
				case IF_ICMPLT: index1=NEXT(); index2=NEXT();  m.IF_ICMP("LT",shortIndex(index1,index2) ); break;
				case IF_ICMPNE: index1=NEXT(); index2=NEXT();  m.IF_ICMP("NE",shortIndex(index1,index2) ); break;
				case IFEQ: index1=NEXT(); index2=NEXT(); m.IF_ICMPZ("EQ",shortIndex(index1,index2) ); break;
				case IFGE: index1=NEXT(); index2=NEXT(); m.IF_ICMPZ("GE",shortIndex(index1,index2) ); break;
				case IFGT: index1=NEXT(); index2=NEXT(); m.IF_ICMPZ("GT",shortIndex(index1,index2) ); break;
				case IFLE: index1=NEXT(); index2=NEXT(); m.IF_ICMPZ("LE",shortIndex(index1,index2) ); break;
				case IFLT: index1=NEXT(); index2=NEXT(); m.IF_ICMPZ("LT",shortIndex(index1,index2) ); break;
				case IFNE: index1=NEXT(); index2=NEXT(); m.IF_ICMPZ("NE",shortIndex(index1,index2) ); break;
				case IFNULL: index1=NEXT(); index2=NEXT(); m.IFNULL( shortIndex(index1,index2)); break;

				//subroutines
				case RETURNV:
					//log("RETURNV");
					lev = m.RETURNV();
					//log ("lev="+lev);
					if (lev==0) { log("PROGRAM COMPLETE"); running = false;}
					break;
				case IRETURN: m.ARETURN(); break;
				case ARETURN: m.ARETURN(); break;
				case INVOKESTATIC:
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:  index1=NEXT(); index2=NEXT();  m.INVOKE( charIndex(index1,index2)); break;

				//other
				case CHECKCAST: index1=NEXT(); index2=NEXT(); m.CHECKCAST( charIndex(index1,index2)); break;

				default:
					log("unknown op "+op+" (0x"+Integer.toHexString(op)+")");
			}
		}
	}
}