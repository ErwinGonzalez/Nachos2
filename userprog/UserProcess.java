package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
	Lib.assertTrue(KThread.currentThread() instanceof UThread);
		currThread=(UThread)KThread.currentThread();
	processID=nextPID++;
	runningProcesses++;
	parentID=null;
	exitStatus=null;
	children= new LinkedList<UserProcess>();
	//instantiate open files for this process
	fileDescriptors = new OpenFile[16];
	//always reserves space for stdin and stdout
	fileDescriptors[0] = UserKernel.console.openForReading();
	fileDescriptors[1] = UserKernel.console.openForWriting();
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	/*// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;*/
	    
	int vpn = Machine.processor().pageFromAddress(vaddr);
	int addrOffset = Machine.processor().offsetFromAddress(vaddr);
	// get corresponding ppn from pageTable
	int ppn = pageTable[vpn].ppn;
	//obtains paddr
	int paddr = ppn*pageSize+offset;
	// updates entry 
	TranslationEntry entry = pageTable[vpn];
	entry.used=true;
	// checks if the paddr is valid 
	// might need to check if offset goes over the page
	if(entry.valid!=true || vpn<0 || vpn>numPages )
		return 0;

	int amount = Math.min(length, memory.length-paddr);
	System.arraycopy(memory, paddr, data, offset, amount);

	return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	/*// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;*/
	    
	// gets page number and offset for the vpn,
	int vpn = Machine.processor().pageFromAddress(vaddr);
	int addrOffset = Machine.processor().offsetFromAddress(vaddr);
	// get corresponding ppn from pageTable
	int ppn = pageTable[vpn].ppn;
	//obtains paddr
	int paddr = ppn*pageSize+offset;
	// updates entry 
	TranslationEntry entry = pageTable[vpn];
	entry.used=true;
	entry.dirty=true;
	// checks if the paddr is valid 
	// might need to check if offset goes over the page
	if(entry.readOnly || entry.valid!=true || vpn<0 || vpn>numPages )
		return 0;
	
	int amount = Math.min(length, memory.length-paddr);
	System.arraycopy(data, offset, memory, paddr, amount);

	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;
/*
	Change so that process has a limited page table?
	
*/

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	/*
	 a load fails if the process is bigger than the memory or if it's bigger than the 
	 free space
	*/
	if (numPages > Machine.processor().getNumPhysPages() || numPages>UserKernel.getFreePages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;

		//should code be synchronized?
		// get free page physicaal page from the kernel
		int ppn = UserKernel.getFirstFreePhysPage();
		// if there's none exit on error
		if(ppn==-1){
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
	                return false;
		}
		//Update Translation Entries, used and dirty remain on false
		TranslationEntry entry = pageTable[vpn];
		entry.ppn=ppn;
		entry.vpn=vpn;
		entry.valid=true;
		entry.readOnly=section.isReadOnly();
		section.loadPage(i, ppn);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	for(OpenFile file : fileDescriptors)
		file.close();		
	for(int i=0;i<numPages;i++){
		TranslationEntry entry = pageTable[i];
		UserKernel.releasePhysPage(entry.ppn);
		entry.valid=false;
		
	}
	coff.close();
		
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
	// TODO add unique process ID checking so that only the root process can call halt
		Machine.halt();
		Lib.assertNotReached("Machine.halt() did not halt machine!");

	return 0;
    }
    /*
	handleExit(int a0), syscall
	releases any resource held by this proccess
	an exiting process should set status to 0
	
	@param a0 is the exit status
	
	@return nothing exit() never returns
    */
    private int handleExit(int a0){
    	//TODO notify parent node, if any, of exit
    	//must notify children nodes, set parent to null or 0
	for(UserProcess child : children)
	    child.parentID=null;
	unloadSections();
	exitStatus=0;
	a0=exitStatus;
	//signal process waiting? might just use UThread join to handle
	if(--runningProcesses==0)	
	    UserKernel.terminate();
	//add assert so that caller is current thread
	currThread.finish();
	return exitStatus;
    }
    /*
	handleExec(int a0, int a1, int a2), syscall
	executes the program specified by a0 in a child process
	
    	@param a0 is a pointer to the filename to execute
    	@param a1 is the number of parameters, must be non negative
    	@param a2 is the array containing the parameters, arguments should be 4 bytes like the argv

	@return child process ID, -1 on error
    */
    private int handleExec(int a0, int a1, int a2){
	if(a0<0 || a1<0 || a2<0)
	    return -1;
	String filename = readVirtualMemoryString(a0,maxStringSize);
	String end = filename.substring(filename.length()-4,filename.length());
	if(!end.equals(".coff"))
	    return -1;
	    
	String[] args = new String[a1];
	int offset =  0;
	for(int i=0;i<a1;i++){
	    byte[] buffer = new byte[4];
	    int read = readVirtualMemory(a2+offset,buffer);
            int address = Lib.bytesToInt(buffer,0);
            args[i]=readVirtualMemoryString(address,maxStringSize);
            offset+=4;
	}
	UserProcess newChild = new UserProcess();
	newChild.parentID=this.processID;
	children.add(newChild);
	boolean success = newChild.execute(filename,args);
	if(success)
	    return newChild.processID;
	else
	    return -1;
    }
    /*
	a0 is the id of the child process to join
	a1 is a pointer to where the child exit status will be stored
	
    */
    private int handleJoin(int a0, int a1){
    	/*
    	procedure:
    		find process id(a0) in the process children list
	    		if not found return error
	        join the child, might need to add a UThread param to use that join
	        remove the child from the list 
	        read removed child exit status
	        store exit status in a1
    	*/
	int p=-1;
	UserProcess up;
	for(UserProcess child:children)
	    if(child.processID==a0){
		p=child.processID;
		up=children.remove(child);
		up.parentID=null;
		if(up.exitStatus==0)
		    return 1;
		break;
	    }
	if(p==-1)
	    return -1;
	//TODO join stuff here
	up.currThread.join();
	if(up.exitStatus==null)
	    return 0;
	return 1;
	
    }
    /*
	handleCreat(int a0), syscall
	creates a new file 

	@param a0 is the pointer to the file name

	@returns file descriptor if create is successful, -1 if there's an error
    */
    private int handleCreat(int a0){
	/*sanity checks, first address must be in range
	  second filename must be a valid value
	  finally there must be less than 16 files open for the process 
	  must change this one, after mem add checks*/
	
	String filename = readVirtualMemoryString(a0,maxStringSize);
	int openFD;
	for(openFD =2;openFD<16;openFD++){
	    if(fileDescriptors[openFD]==null)
		break;
	}
	// returns -1 if any condition is true, might need to change for 
	// debug purposes
	if (a0 < 0 || filename == null || openFD==16)
	    return -1;
	// calls filesystem to handle a new file creation
	OpenFile file  = ThreadedKernel.fileSystem.open(filename,true);
	if(file!=null)
		fileDescriptors[openFD]=file;
	else
		return -1;
	return openFD;
	
    }
    /*
	handleOpen(int a0), syscall
	opens specified file 

	@param a0 is the pointer to the file name

	@returns file descriptor if open is successful, -1 if there's an error
    */
    private int handleOpen(int a0){
	/*sanity checks, first address must be in range
	  second filename must be a valid value
	  finally there must be less than 16 files open for the process 
	  must change this one, after mem add checks*/
	
	String filename = readVirtualMemoryString(a0,maxStringSize);
	int openFD;
	for(openFD =0;openFD<16;openFD++){
	    if(fileDescriptors[openFD]==null)
		break;
	}
	// returns -1 if any condition is true, might need to change for debug purposes
	if (a0 < 0 || filename == null || openFD==16)
	    return -1;
	// calls filesystem to handle a new file opening
	OpenFile file  = ThreadedKernel.fileSystem.open(filename,false);
	if(file!=null)
		fileDescriptors[openFD]=file;
	else
		return -1;
	return openFD;
	
    }

    /*
	handleRead(int a0, int a1, int a2), syscall
	reads a0 file content into a1 buffer

	@param a0 is the file descriptor 
	@param a1 is the pointer to the buffer, in which file will be read
	@param a2 is the number of bytes to be read

	@returns number of bytes read if successful, -1 if there's an error
    */
    private int handleRead(int a0, int a1, int a2){
	
	// returns -1 if buffer is at invalid address
	if(a1<0)
	    return -1;
	// returns -1 if file descriptor is out of range or if size to be read is less than  0
	if(a0<0 || a0>15 || a2<0)	
	    return -1;
	// if file to be read isn't part of the process' fileDescriptors return -1 	
	if(fileDescriptors[a0] == null)
	    return -1;
	
	// reads file into buffer
	byte[] buffer =new byte[a2];
	int read = fileDescriptors[a0].read(a1,buffer,0,a2);
	return writeVirtualMemory(a1,buffer,0,read);
    }
    /*
	handleWrite(int a0, int a1, int a2), syscall
	writes a1 content to a0 file

	@param a0 is the file descriptor 
	@param a1 is the pointer to the buffer, which contains bytes to be written
	@param a2 is the number of bytes to be written

	@returns number of bytes written if successful, -1 if there's an error
    */
    private int handleWrite(int a0, int a1, int a2){
	// returns -1 if buffer is at invalid address
	if(a1<0)
	    return -1;
	// returns -1 if file descriptor is out of range or if size to be read is less than  0
	if(a0<0 || a0>15 || a2<0)	
	    return -1;
	// if file to be written isn't part of the process' fileDescriptors return -1 	
	if(fileDescriptors[a0] == null)
	    return -1;
	
	// write buffer into file
	byte[] buffer =new byte[a2];
	int write = readVirtualMemory(a1,buffer,0,a2);
	return fileDescriptors[a0].write(buffer,0,write);

    }
    /*
        handleClose(), syscall    
        closes file asociated with the fileDescriptor
        
        @param a0 is the file to be closed
        @returns 0 if successful, -1 on error
    */
    private int handleClose(int a0){
    	//TODO flush changes to file before closing
    	// checks if it's a valid file descriptor
	if(a0>15 || a0<0)
		return -1;
	// checks if it exists
	if(fileDescriptors[a0]==null)
		return -1;
	// if it's valid fd and exists, closes the fd
	fileDescriptors[a0].close();
	fileDescriptors[a0]=null;
	return 0;
    }
    /*
	handleUnlink(int a0), syscall
	deletes file or marks file to be deleted

	@param a0 is the file name

	@returns 0 if successful, -1 if there's an error
    */
    private int handleUnlink(int a0){
    	if(a0<0) 
    	    return -1;
    	String name = readVirtualMemoryString(a0,maxStringSize);
    	OpenFile file;
    	int i=-1;
    	for(int j=0;j<16;j++){
    	    file = fileDescriptors[j];
    	    if(file!=null && file.getName().equals(name)){
    	    	i=j;
    	    	break;
    	    }
    	}
    	if(i!=-1)
    	   return -1;
    	/*TODO keep global opened files list?
	   check that to see if any proccess has it open?
	*/
    	ThreadedKernel.fileSystem.remove(name);
    	return 0;
    }
    // Syscall constants 
    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {

	case syscallHalt://syscall 0
	    return handleHalt();
	case syscallExit:
	    return 0;
	case syscallExec:
	    return 0;
	case syscallJoin:
	    return 0;
	case syscallCreate://syscall 4
	    return handleCreat(a0);
	case syscallOpen://syscall 5
	    return handleOpen(a0);
	case syscallRead://syscall 6
	    return handleRead(a0,a1,a2);
	case syscallWrite://syscall 7
	    return handleWrite(a0,a1,a2);
	case syscallClose:// syscall 8
	    return handleClose(a0);
	case syscallUnlink://syscall 9
	    return handleUnlink(a0);
	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }


    // the files this process has opened, max 16
    protected OpenFile[] fileDescriptors;
    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;

    /** Max size of a string. */
    protected final int maxStringSize = 256;
    /** Program Counter and Stack Pointer. */ 	   
    private int initialPC, initialSP;
    /** Program Arguments. */
    private int argc, argv;
    /** Global process count. */
    private static int nextPID=0;
    private static int runningProcesses=0;
    /** Unique Process ID. */
    private int processID;
    private int parentID;
    private LinkedList<UserProcess> children;
    private UThread currThread;
    public int exitStatus;
    /** Page Size defined by the processor. */
    private static final int pageSize = Processor.pageSize;
    /** Process Debug Flag. */   
    private static final char dbgProcess = 'a';
}
