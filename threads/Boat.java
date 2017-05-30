package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
//	System.out.println("\n ***Testing Boats with only 2 children***");
//	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	currAdultsOnMolokai = 0;	
	currKidsOnMolokai = 0;
	currAdultsOnOahu = 0;
	currKidsOnOahu = 0;
	boatOnOahu = true;
	childRowing=false;
	childRiding=false;
	boat = new Lock();
	kidsOnOahu = new Condition2(boat);
	adultsOnOahu = new Condition2(boat);
	kidsOnMolokai = new Condition2(boat);
	adultsOnMolokai = new Condition2(boat);
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	/*Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();*/
        
        //semaphore used to stop main thread until execution is finished
        s = new Semaphore(0);
        
        //both runnables only call Itinerary
	Runnable kid = new Runnable(){
		public void run(){
			ChildItinerary();
		}
	};
	Runnable adult = new Runnable(){
		public void run(){
			AdultItinerary();
		}
	};
	//for each thread increases the counters
	for(int i =0;i<adults;i++){
		new KThread(adult).fork();
		currAdultsOnOahu++;
	}
	for(int i =0;i<children;i++){
		new KThread(kid).fork();
		currKidsOnOahu++;
	}
	//locks main thread
	s.P();
    }

    static void AdultItinerary()
    {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
	boat.acquire();
	
	//adults never go first so, if its called before a child can go, it sleeps
        while(currKidsOnOahu>1 || !boatOnOahu){
        	adultsOnOahu.sleep();
	}
	//when notified, moves from one island to the other, changes boat status and wakes a child to go back, finally releases the lock
	currAdultsOnOahu--;
	boatOnOahu=false;
	bg.AdultRowToMolokai();
	currAdultsOnMolokai++;
	kidsOnMolokai.wake();
	boat.release();
	
    }

    static void ChildItinerary()
    {
    	//children will run until there's no one on Oahu
    	while(currKidsOnOahu>0 || currAdultsOnOahu>0){
	 	boat.acquire();
		if(boatOnOahu && (!childRowing || !childRiding)){
			currKidsOnOahu--;
		    	currKidsOnMolokai++;
		    	// one child rows, one is a passanger, if a thread finds both on 'true' it goes to sleep
			if(!childRowing){
				childRowing=true;
				bg.ChildRowToMolokai();
				//if one is rowing it wakes up a partner
				kidsOnOahu.wake();
				kidsOnOahu.sleep();
			}else{
				bg.ChildRideToMolokai();
				childRiding=true;
				//notifies the rower, and both leave, this child sleeps on Molokai
				kidsOnOahu.wake();
				kidsOnMolokai.sleep();
			}
			//changes all control variables
			boatOnOahu=false;
		    	childRowing=false;	
		    	childRiding=false;
		    	//once both are across, the rower always goes back
			bg.ChildRowToOahu();
			currKidsOnOahu++;
			currKidsOnMolokai--;
			// finish condition, is only child when it rows back and no adults remain
			if(currKidsOnOahu == 1 && currAdultsOnOahu==0){
				bg.ChildRowToMolokai();
			    	currKidsOnOahu--;
			    	currKidsOnMolokai++;
			        s.V();//frees main thread
			    	kidsOnMolokai.sleep();
			}
			//returns boat to Oahu, if is only child, wakes an Adult
			boatOnOahu=true;
			if(currKidsOnOahu == 1){
			    adultsOnOahu.wake();
			}
			//whether it's only child or not always tries to wake other childs
			kidsOnOahu.wakeAll();
			
		}
		//sleeps and, then releases lock before next iteration
		kidsOnOahu.sleep();
		boat.release();
	}

    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }

	private static int currAdultsOnMolokai;	// adults that have crossed	
	private static int currKidsOnMolokai;   // children that have crossed
	private static int currAdultsOnOahu;	// adults that have yet to cross
	private static int currKidsOnOahu;	// children that have yet to cross
	private static boolean boatOnOahu;	// wheter the boat is on Oahu or Molokai
	private static boolean childRowing;	// the child that rows the boat
	private static boolean childRiding;	// the child that rides as a passanger
	private static Lock boat;		// the boat acts as a lock in this problem
	private static Condition2 kidsOnOahu;	// the four condition variables used for synchronization
	private static Condition2 adultsOnOahu;
	private static Condition2 kidsOnMolokai;
	private static Condition2 adultsOnMolokai;// unused since adults never go back, no need to keep tabs on them
	private static Semaphore s;		// the semaphore used to block the main thread
    
}
