package lava4;
/**
* BlackBox controls the stacks.  This has 3 stacks.
*	data stack
*	local variable stack
*	frame stack
* The basic unit of information is a Cell
*
* There could also be an input stack and a float stack, but those are for a future version
*/
public class BlackBox  {
	public static Cell LID = new Cell("----");

	//data stack
	Cell[] data = new Cell[256];
	int dataPointer = 0;		//points to next free slot

	//local stack
	Cell[] local = new Cell[256];
	int localBase = 0;
	int localTop = 0;			//point to next free slot

	//frame stack
	//the bottom frame will be null and 1 is the main frame
	//this is so when the main frame returns that framePointer is 0
	Frame[] frameStack = new Frame[256];
	int framePointer = 0;		//the main frame will be 1
	boolean debug = true;

	public BlackBox(boolean debug) {
		this.debug = debug;
	}

	public void log(String s) {
		if (debug) System.out.println(s);
	}

	//--------------------------------------
	//first, we have operations that directly affect the stacks.
	//data stack
	public void IPUSH(int i) {
		data[dataPointer++] = new Cell(i);
	}

	public void PUSH(Cell d) {
		if (d==null) throw new IllegalStateException("BlackBox.PUSH: Cell is null");
		data[dataPointer++] = d;

		//log("===================");
		//log("after BlackBox.PUSH");
		//log(dumpStack());
		//log("===================");
	}

	public Cell POP() {
		dataPointer--;
		Cell ret = data[dataPointer];
		data[dataPointer] = null;	//clear it
		return ret;
	}

	public int IPOP() {
		return POP().toInt();
	}

	//look at what is on top of the stack
	public Cell PEEK() {
		return data[dataPointer-1];
	}

	public void DUP() {
		Cell d = data[dataPointer-1];
		PUSH(d);
	}

	//enter a new frame
	public void dataStackEnter() {
		PUSH(LID);
	}

	//we expect the data stack to have LID at the top
	public void dataStackLeave() {
		Cell d = POP();
		while (!d.equals(LID)) {
			System.out.println("data stack contains unexpected value "+d);
			d = POP();
		}
	}

	public String dumpStack() {
		StringBuffer sb = new StringBuffer("== DATA STACK ==\n");
		for (int i=0;i<dataPointer;i++) {
			sb.append(i+": "+data[i]+"\n");
		}
		return sb.toString();
	}
	//--------------------------------------
	//local variable stack
	public void localPush(Cell d) {
		local[localTop++] = d;
	}

	public Cell localPop() {
		localTop--;
		Cell ret = local[localTop];
		local[localTop] = null;	//clear it
		return ret;
	}

	public void STORE(int n,Cell v) {
		local[localBase+n]=v;
		//adjust localTop
		if (localTop <= localBase + n) {
			//remember, localTop points to the next free spot
			localTop = localBase + n + 1;
		}
	}

	public Cell LOAD(int n) {
		return local[localBase+n];
	}

	//get the local variable and increment it by 1
	//this isn't very efficient because you have to parseInt every time
	public void INC(int n) {
		Cell c = LOAD(n);
		c.increment();
		//we don't need to put this back, it is incremented in place
	}

	public void localEnter() {
		local[localTop] = new Cell(localBase);
		local[localTop+1] = LID;
		localBase = localTop+2;
		localTop = localBase;
	}

	//clear out the trash when leaving
	public void localLeave() {
		Cell d = localPop();
		while (d==null || !d.equals(LID)) {
			d = localPop();
		}
		//now get the next item down which is the prior local base
		d = localPop();
		//reset localBase to prior
		localBase = d.toInt();
	}

	public String dumpLocal() {
		StringBuffer sb = new StringBuffer("== LOCAL STACK ==\n");
		for (int i=0;i<localTop;i++) {
			sb.append(i+"="+local[i]+"\n");
		}
		return sb.toString();
	}

	//--------------------------------------
	//frame stack

	/**
	* Invoke a new frame.  This is actually quite simple.  There is no
	* real difference between invoke_static, invoke_virtual and invoke_special.
	* In invoke_static, you don't pass any object references, but the other
	* arguments are on the stack in reverse order.
	* In invoke_virtual, the top argument on the stack is the object reference.
	* In invoke_special, the name is funky "<init>", but otherwise this is identical
	* to invoke_virtual, with the top argument the object reference.
	*
	* This is tricky.  I am popping values off the data stack and putting them on the new
	* local stack at the same time.  So these are temporarily out of synch
	*/
	public void INVOKE(MethodInfo mi) {
		log("BlackBox.INVOKE: running method# "+mi.getNameId());
		if (mi.getNameId()==0) {
			throw new IllegalStateException("unable to invoke method #0");
		}
		//start new scope of locals, but keep old datastack scope open
		localEnter();
		Frame f = new Frame(framePointer,mi);
		frameStack[framePointer++]=f;
		int params = f.params();
		//log("BlackBox.INVOKE: params = "+params);
		//get the parameters from stack
		for (int i=0;i<params;i++) {
			Cell data = POP();
			//log("BlackBox.INVOKE: pushing '"+data+"' to local");
			localPush(data);
		}
		//now close old data stack scope
		dataStackEnter();
		//that's all it takes
	}

	/**
	* Return from the method, which did not have a return value.
	* Return the new frame level.  If this is level 0, then the program is complete
	*/
	public int RETURNV() {
		framePointer--;
		Frame f = frameStack[framePointer];
		//I know this doesn't do anything but a better gc would take this as a hint
		f.finalize();
		frameStack[framePointer] = null;	//clear it
		//now adjust the other stacks
		dataStackLeave();
		localLeave();
		return framePointer;
	}

	/**
	* Return from Method.  The return value goes on to the stack.
	* The number returned from this is the new frame level.
	* This also works with IRETURN
	*/
	public int ARETURN() {
		Cell ret = POP();		//get the return value
		int f = RETURNV();		//go down a level
		PUSH(ret);				//push it back on the stack
		return f;
	}

	public byte NEXT() {
		return frameStack[framePointer-1].NEXT();
	}

	//make a relative change to the framepointer
	//this requires an adjustment which I think is -3
	public void JUMP(short rel) {
		frameStack[framePointer-1].JUMP(rel);
	}

	public String getMethodName() {
		return frameStack[framePointer-1].getMethodName();
	}

	public String getClassName() {
		return frameStack[framePointer-1].getClassName();
	}

	public int getClassId() {
		return frameStack[framePointer-1].getClassId();
	}

	//=================================
	public static void main(String[] args) {
		//test local stack
		BlackBox m = new BlackBox(true);
		m.STORE(0,new Cell('a'));
		m.STORE(1,new Cell('b'));
		m.STORE(2,new Cell('c'));
		m.localEnter();
		m.STORE(0,new Cell('d'));
		m.STORE(1,new Cell('e'));
		m.STORE(2,new Cell('f'));
		m.localEnter();
		m.STORE(0,new Cell('g'));
		m.STORE(1,new Cell('h'));
		m.STORE(2,new Cell('i'));

		System.out.println(m.dumpLocal());
		m.localLeave();
		System.out.println(m.dumpLocal());
	}
}