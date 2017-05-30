package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
	this.message=0;
	this.speakers=0;
	this.listeners=0;
	this.activeSpeak=false;
	this.activeListen=false;
	this.cLock=new Lock();
	this.waitingSpeakers =  new Condition2(cLock);
	this.waitingListeners = new Condition2(cLock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
	cLock.acquire();
	speakers++;
	while(listeners==0 || activeSpeak)
		waitingSpeakers.sleep();
	activeSpeak=true;
	this.message = word;	
	waitingListeners.wakeAll();
	speakers--;

	activeListen=false;
	cLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
	cLock.acquire();
	listeners++;
	while(activeListen)
		waitingListeners.sleep();
	activeListen=true;
	if(!activeSpeak){	
		waitingSpeakers.wake();
		waitingListeners.sleep();}
	listeners--;
	int retVal = this.message;
	waitingSpeakers.wakeAll();
	activeSpeak=false;
	cLock.release();
	return retVal;
	//return 0;
    }
	public static class Speak implements Runnable{
		Speak(int thread, Communicator com) {
			this.threadNo = thread;
			this.c = com;
		}

		public void run() {
			System.out.println("Speaker "+threadNo+" speaking: "+threadNo*20);
			c.speak(threadNo*20);
			System.out.println(" Exit Speaker ");
		}

		private int threadNo;
		private Communicator c;
	}
	
	public static class Listen implements Runnable{
		Listen(int thread, Communicator com) {
			this.threadNo = thread;
			this.c = com;
		}

		public void run() {
			System.out.println("Listener "+threadNo+" listening");
			int res = c.listen();
			System.out.println("Listener "+threadNo+" listened "+res);
			System.out.println(" Exit Listener ");
		}

		private int threadNo;
		private Communicator c;
	}
	
	public static void selfTest(){

		Lib.debug(dbgComm, "Enter KThread.selfTest");
		Communicator c = new Communicator();
		KThread s1 = new KThread(new Speak(1,c));
		KThread l1 = new KThread(new Listen(1,c));
		KThread s2 = new KThread(new Speak(2,c));
		KThread l2 = new KThread(new Listen(2,c));
		KThread s3 = new KThread(new Speak(3,c));
		KThread l3 = new KThread(new Listen(3,c));

		int testType = 0;
		if(testType==0){
			s1.fork();
			s2.fork();
			s3.fork();
			l1.fork();
			l2.fork();
			l3.fork();
		}
		if(testType==1){
			l1.fork();
			l2.fork();
			l3.fork();
			s1.fork();
			s2.fork();
			s3.fork();
		}
		if(testType==2){
			s1.fork();
			l1.fork();
			s2.fork();
			l2.fork();
			s3.fork();
			l3.fork();
		}
	}


    private static final char dbgComm = 'c';

	private int message;
	private int speakers;
    	private int listeners;
	private boolean activeSpeak;
	private boolean activeListen;
	private Lock cLock;
	private Condition2 waitingSpeakers;
	private Condition2 waitingListeners;
	
}
