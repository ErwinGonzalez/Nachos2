package nachos.userprog;

import java.util.LinkedList;
import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	console = new SynchConsole(Machine.console());
	//create physical pages array
	physicalPages = new Integer[Machine.processor().getNumPhysPages()];
	//start them as 'free' (0), when used change to 'in use' (1)
	for(int i =0;i<Machine.processor().getNumPhysPages();i++)
		physicalPages[i]=0;
	//start value as max free pages
	freePages = Machine.processor().getNumPhysPages();
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
	// start semaphore so only one process can assign pages at a time
	pagesSem = new Semaphore(1);
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();	
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }
    /**
    * returns the first unassigned page, then updates the array
    */
    public static int getFirstFreePhysPage(){
    	pagesSem.P();
    	int ret =-1;
 	for(int i =0;i<Machine.processor().getNumPhysPages();i++)
		if(physicalPages[i]==0){
			physicalPages[i]=1;
		    	freePages--;
			ret=1;
			break;}
	pagesSem.V();			
	return ret; 			   	
    }
    /**
    * Unassign an used page so it can be used by another process
    */
    public static void releasePhysPage(int numPage){
	pagesSem.P();
    	physicalPages[numPage]=0;
	pagesSem.V();
    }
    /* returns the number of free pages */
    public static int getFreePages(){
    	return freePages;
    }
	
    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;
    /** Array representing the physical memory pages. */
    private static Integer[] physicalPages;   //equal to Machine.processor.getNumPhysPages();
    /** Number of unassigned pages. */
    private static int freePages;
    /** Semaphore used to synchronize access. */
    private static Semaphore pagesSem;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}
