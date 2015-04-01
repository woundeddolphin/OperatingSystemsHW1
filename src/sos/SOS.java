package sos;

import java.util.*;

/**
 * This class contains the simulated operating system (SOS).  Realistically it
 * would run on the same processor (CPU) that it is managing but instead it uses
 * the real-world processor in order to allow a focus on the essentials of
 * operating system design using a high level programming language.
 *
 *
 *
 * @author Justice Nichols
 * @author Krismy Alfaro
 * @author Matthew Farr
 * @author Zak Pearson
 * @author Matt Wellnitz
 * @author Jeremy Cimfl
 * @author Bryce Matsuda
 * 
 */
   
public class SOS implements CPU.TrapHandler
{
    //======================================================================
    //Constants
    //----------------------------------------------------------------------

    //These constants define the system calls this OS can currently handle
    public static final int SYSCALL_EXIT     = 0;    /* exit the current program */
    public static final int SYSCALL_OUTPUT   = 1;    /* outputs a number */
    public static final int SYSCALL_GETPID   = 2;    /* get current process id */
    public static final int SYSCALL_COREDUMP = 9;    /* print process state and exit */
    public static final int SYSCALL_OPEN    = 3;    /* access a device */
    public static final int SYSCALL_CLOSE   = 4;    /* release a device */
    public static final int SYSCALL_READ    = 5;    /* get input from device */
    public static final int SYSCALL_WRITE   = 6;    /* send output to device */
    /// ERROR CODES
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_NO_DEVICE = -1;
    public static final int CODE_NOT_SHARABLE = -2;
    public static final int CODE_ALREADY_OPEN = -3;
    public static final int CODE_NOT_OPENED = -4;
    public static final int CODE_NOT_WRITEABLE = -5;
    public static final int CODE_NOT_READABLE = -6;
    /// MultiPrograming 
    public static final int SYSCALL_EXEC    = 7;    /* spawn a new process */
    public static final int SYSCALL_YIELD   = 8;    /* yield the CPU to another process */
    /// Threads
    public static final int IDLE_PROC_ID    = 999;  

    //======================================================================
    //Member variables
    //----------------------------------------------------------------------

    
    /**
     * This flag causes the SOS to print lots of potentially helpful
     * status messages
     **/
    public static final boolean m_verbose = true;
    
    /**
     * The CPU the operating system is managing.
     **/
    private CPU m_CPU = null;
    
    /**
     * The RAM attached to the CPU.
     **/
    private RAM m_RAM = null;
    
    /**
     * The Control block attached to the CPU
     **/
    private ProcessControlBlock m_currProcess = null;
    
    /**
     * The Control block attached to the CPU
     **/
    private Vector<DeviceInfo> m_devices = null;
    
    /**
     * a Vector of all the Program objects (not processes!) that are available 
     * to the operating system.
     **/
    Vector<Program> m_programs = null;
    
    
    /**
     * This variable specifies the id that will be 
     * assigned to the next process that is loaded
     */
    int m_nextProcessID;
    
    /**
     * This is a list of all the processes that are currently 
     * loaded into RAM and in one of the major states 
     * (Ready, Running or Blocked)
     */
    Vector<ProcessControlBlock> m_processes = null;
    
    Vector<MemBlock> m_freeList = null;
    
    /*======================================================================
     * Constructors & Debugging
     *----------------------------------------------------------------------
     */
    
    /**
     * The constructor does nothing special
     */
    public SOS(CPU c, RAM r)
    {
        //Init member list
        m_CPU = c;
        m_RAM = r;
        m_CPU.registerTrapHandler(this);
        m_currProcess = new ProcessControlBlock(42);
        m_devices = new Vector<DeviceInfo>(0);
        m_programs = new Vector<Program>();
        m_nextProcessID = 1001;
        m_processes = new Vector<ProcessControlBlock>();
        m_freeList = new Vector<MemBlock>();
        m_freeList.add(new MemBlock(0,m_RAM.getSize()-1));
        
    }//SOS ctor
    
    /**
     * Does a System.out.print as long as m_verbose is true
     **/
    public static void debugPrint(String s)
    {
        if (m_verbose)
        {
            System.out.print(s);
        }
    }
    
    /**
     * Does a System.out.println as long as m_verbose is true
     **/
    public static void debugPrintln(String s)
    {
        if (m_verbose)
        {
            System.out.println(s);
        }
    }
    
    /*======================================================================
     * Memory Block Management Methods
     *----------------------------------------------------------------------
     */

    //None yet!
    
    /*======================================================================
     * Device Management Methods
     *----------------------------------------------------------------------
     */

    //None yet!
    
    /*======================================================================
     * Process Management Methods
     *----------------------------------------------------------------------
     */

    /**
     * createIdleProcess
     *
     * creates a one instruction process that immediately exits.  This is used
     * to buy time until device I/O completes and unblocks a legitimate
     * process.
     *
     */
    public void createIdleProcess()
    {
        int progArr[] = { 0, 0, 0, 0,   //SET r0=0
                          0, 0, 0, 0,   //SET r0=0 (repeated instruction to account for vagaries in student implementation of the CPU class)
                         10, 0, 0, 0,   //PUSH r0
                         15, 0, 0, 0 }; //TRAP

        //Initialize the starting position for this program
        int baseAddr = allocBlock(progArr.length);
        if(baseAddr == -1)
        {
        	System.out.println("Failed to load idle Process! Exiting!!!");
        	System.exit(-1);
        }
        //Load the program into RAM
        for(int i = 0; i < progArr.length; i++)
        {
            m_RAM.write(baseAddr + i, progArr[i]);
        }

        //Save the register info from the current process (if there is one)
        if (m_currProcess != null)
        {
            m_currProcess.save(m_CPU);
        }
        
        //Set the appropriate registers
        m_CPU.setPC(baseAddr);
        m_CPU.setSP(baseAddr + progArr.length + 10);
        m_CPU.setBASE(baseAddr);
        m_CPU.setLIM(baseAddr + progArr.length + 20);

        //Save the relevant info as a new entry in m_processes
        m_currProcess = new ProcessControlBlock(IDLE_PROC_ID);  
        m_processes.add(m_currProcess);

    }//createIdleProcess

    /**
     * printProcessTable      **DEBUGGING**
     *
     * prints all the processes in the process table
     */
    private void printProcessTable()
    {
        debugPrintln("");
        debugPrintln("Process Table (" + m_processes.size() + " processes)");
        debugPrintln("======================================================================");
        for(ProcessControlBlock pi : m_processes)
        {
            debugPrintln("    " + pi);
        }//for
        debugPrintln("----------------------------------------------------------------------");

    }//printProcessTable

	/**
	 * removeCurrentProcess
	 * 
	 * removes the current processes from m_processes
	 * and calls scheduleNewProcess to pick a new one
	 */
    public void removeCurrentProcess()
    {
    	//printProcessTable();
    	m_processes.remove(m_currProcess);
    	freeCurrProcessMemBlock();
    }//removeCurrentProcess

    /**
     * getRandomProcess
     *
     * selects a non-Blocked process at random from the ProcessTable.
     *
     * @return a reference to the ProcessControlBlock struct of the selected process
     * -OR- null if no non-blocked process exists
     */
    ProcessControlBlock getRandomProcess()
    {
        //Calculate a random offset into the m_processes list
        int offset = ((int)(Math.random() * 2147483647)) % m_processes.size();
            
        //Iterate until a non-blocked process is found
        ProcessControlBlock newProc = null;
        for(int i = 0; i < m_processes.size(); i++)
        {
            newProc = m_processes.get((i + offset) % m_processes.size());
            if ( ! newProc.isBlocked())
            {
                return newProc;
            }
        }//for

        return null;        // no processes are Ready
    }//getRandomProcess
    
    /**
     * getFairProcess
     *
     * selects a non-Blocked process with the smallest average starve time.
     *
     * @return a reference to the ProcessControlBlock struct of the selected process
     * -OR- null if no non-blocked process exists
     */
    ProcessControlBlock getFairProcess()
    {
    	int index = -1; 
    	double avgStarve = Integer.MAX_VALUE;    	
    	for(int i = 0; i < m_processes.size(); i++)
    	{
    		if(avgStarve > m_processes.get(i).avgStarve && !m_processes.get(i).isBlocked())
    		{
    			avgStarve = m_processes.get(i).avgStarve;
    			index = i;
    		}
    	}
    	if (avgStarve == Integer.MAX_VALUE)
    	{
    		return null;
    	}
    	
		return m_processes.get(index);
    }
    
	/**
	 * scheduleNewProcess
	 * 
	 * Selects a new process to run and runs it
	 */
    public void scheduleNewProcess()
    {
    	if (m_processes.isEmpty())
    	{

    		//debugPrintln("No more processes to run. Stopping.");
    		System.exit(CODE_SUCCESS);
    	}
    	int i = 1;
    	ProcessControlBlock temp;
    	switch (i)
    	{
    	case 0: 
    		temp = getRandomProcess();
    		break;
    	case 1: 
    		temp = getFairProcess();
    		break;
		default:
	    	temp = getRandomProcess();
	    	break;
    	}
    	if(temp == null)
    	{
    		createIdleProcess();
    		return;
    	}	
    	if(!temp.equals(m_currProcess))
    	{
	    	m_currProcess.save(m_CPU);
	    	m_currProcess = temp;
	    	m_currProcess.restore(m_CPU);
    	}
    }//scheduleNewProcess

    /**
     * addProgram
     *
     * registers a new program with the simulated OS that can be used when the
     * current process makes an Exec system call.  (Normally the program is
     * specified by the process via a filename but this is a simulation so the
     * calling process doesn't actually care what program gets loaded.)
     *
     * @param prog  the program to add
     *
     */
    public void addProgram(Program prog)
    {
        m_programs.add(prog);
    }//addProgram  
    
    /*======================================================================
     * Program Management Methods
     *----------------------------------------------------------------------
     */

    /**
     * createProcess()
     * 
     * @param prog 
     * @param size total allocated ram for the program
     * 
     * Helper method to initialize system registers with appropriate values
     */
    public void createProcess(Program prog, int allocSize)
    {       
        //compile the prog into an array of int
        int[] programArray = prog.export(); 
        int location = allocBlock(allocSize);
        if(location == -1)
        {
            m_CPU.setPC(m_CPU.getPC() + CPU.INSTRSIZE);
        	System.out.println("Program installation failed");
        	return;
        }


        
        for(int i = 0; i < programArray.length; i++){ //move the program into ram
            m_RAM.write(location + i, programArray[i]);
        }
        if (m_currProcess != null)
        {
        	m_currProcess.save(m_CPU);
        }
        //printMemAlloc();
        
        m_currProcess = new ProcessControlBlock(m_nextProcessID);
        m_processes.add(m_currProcess);
        m_currProcess.save(m_CPU);
        getFree();

        m_nextProcessID++;  
        intializeRegisters(location, allocSize); // initialize registers
        
    }//createProcess
    
    /**
     * intializeRegisters()
     * 
     * @param loc location in ram to start the program
     * @param size total allocated ram for the program
     * 
     * Helper method to initialize system registers with appropriate values
     */
    private void intializeRegisters(int loc, int size){
        m_CPU.setPC(loc);
        m_CPU.setSP(0);
        m_CPU.setBASE(loc);
        m_CPU.setLIM(loc + size-1);
    }//initialize registers
    

    


    /*======================================================================
     * Interrupt Handlers
     *----------------------------------------------------------------------
     */
    
    /**
     * interruptIllegalMemoryAcess
     * Prints error message when useer touches memory that is not theirs.
     *
     * @param addr the address that was trying to be accessed
     * 
     * @return void
     */
    @Override
    public void interruptIllegalMemoryAccess(int addr) {
        System.out.println("Illegal Memory Access Exception!");
        System.exit(0);
    }//interuptIllegalMemoryAccess
    
    /**
     * interruptDivideByZero
     * Prints error message if division by zero is encountered
     * 
     * @param void
     * 
     * @return void
     */
    @Override
    public void interruptDivideByZero() {
        System.out.println("Illegal Divide by Zero Exception!");
        System.exit(0);
    }
    

    /**
     * interuptIllegalInstruction
     * Prints error message if there is something wrong with the fetched instruction
     * 
     * @param insr The bad instruction
     * 
     * @return void
     */
    @Override
    public void interruptIllegalInstruction(int[] instr) {
        System.out.println("Illegal Instruction Exception!");
        System.exit(0);
        
        
    }//interruptIllegalInstruction
    
    /**
     * interruptIOReadComplete
     * 
     * adds ability to wake up threads that were waiting for a read 
     * operation complete.
     * 
     * @param devID the ID of the device that completed its operation
     * @param addr of blocked process
     * @param data the data that were read from the device
     * 
     * @return void
     */
	@Override
	public void interruptIOReadComplete(int devID, int addr, int data) {
		
		DeviceInfo temp = null;
		for (DeviceInfo i : m_devices)
		{
			if (i.getId() == devID)
			{
				temp = i;
				break;
			}
		}
		if (temp == null)
		{
			m_CPU.pushToStack(CODE_NO_DEVICE);
		}
		else
		{
			ProcessControlBlock block = selectBlockedProcess(temp.device, SYSCALL_READ, addr);
			block.unblock();
			int location = block.getRegisterValue(CPU.LIM) - block.getRegisterValue(CPU.SP);
			m_RAM.write(location, data);
			m_RAM.write(location-1, CODE_SUCCESS);
	        block.setRegisterValue(CPU.SP, CPU.SP+2);
		}
	}//interruptIOReadComplete

    /**
     * interruptIOReadComplete
     * 
     * adds ability to wake up threads that were waiting for a read 
     * operation complete.
     * 
     * @param devID the ID of the device that completed its operation
     * @param addr of the blocked process
     * 
     * @return void
     */
	@Override
	public void interruptIOWriteComplete(int devID, int addr) {
		DeviceInfo temp = null;
		for (DeviceInfo i : m_devices)
		{
			if (i.getId() == devID)
			{
				temp = i;
				break;
			}
		}
		if (temp == null)
		{
			m_CPU.pushToStack(CODE_NO_DEVICE);
		}
		else
		{
			ProcessControlBlock block = selectBlockedProcess(temp.device, SYSCALL_READ, addr);
			block.unblock();
			int location = block.getRegisterValue(CPU.LIM) - block.getRegisterValue(CPU.SP);
			m_RAM.write(location, CODE_SUCCESS);
	        block.setRegisterValue(CPU.SP, CPU.SP+1);
		}		
	}//interruptIOWriteComplete
    
	/**
	 * interruptClock
	 * 
	 * Schedules a new process on a clock interrupt
	 */
	@Override
	public void interruptClock() {
		scheduleNewProcess();
	}//interruptClock()

	
    /*======================================================================
     * System Calls
     *----------------------------------------------------------------------
     */
        
    /**
     * systemCall
     * call backs for handling trap from CPU
     * 
     * @param void
     *
     * @return void
     * 
     */

    @Override
    public void systemCall()
    {
        int opCode;
        //error if nothign is on the stack when a trap is called
        if (m_CPU.getSP() == 0)
        {
            //for now just print error messaage:
            System.out.println("Illegal Instruction Exception!");
            System.exit(0);
        }
        opCode = m_CPU.popFromStack();
        switch (opCode)
        {
            case SYSCALL_EXIT:
                syscallExit();
                break;
            case SYSCALL_OUTPUT:
                syscallOutput();
                break;
            case SYSCALL_GETPID:
                syscallPid();
                break;
            case SYSCALL_COREDUMP:
                syscallCoreDump();
                break;
            case SYSCALL_OPEN:
            	syscallOpen();
            	break;
            case SYSCALL_CLOSE:
            	syscallClose();
            	break;
            case SYSCALL_READ:
            	syscallRead();
            	break;
            case SYSCALL_WRITE:
            	syscallWrite();
            	break;
            case SYSCALL_EXEC:
            	syscallExec();
            	break;
            case SYSCALL_YIELD:
            	syscallYield();   
            	break;
            default:
                break;
        }
    } //syscall
    
    /**
     * syscallExit
     * Current exits the simulation
     *
     * @param void
     * 
     * @return void
     */
    private void syscallExit()
    {
    	removeCurrentProcess();
    	scheduleNewProcess();
        
    }//syscallExit
    
    /**
     * syscallOutput
     * prints pops parameter from stack and prints to terminal
     *
     * @param void the parameter is the last thing pushed on the stack
     *
     * @return void
     */
    private void syscallOutput()
    {
        int a = m_CPU.popFromStack();
        System.out.println("OUTPUT: " + a);
    }//syscallOutput
    
    /**
     * syscallPid
     * pushes program id onto the stack
     *
     * @param void
     *
     * @return void return value is pushed onto the stack
     */
    private void syscallPid ()
    {
        int a = m_currProcess.getProcessId();
        m_CPU.pushToStack(a);

        //debugPrintln("PID = " + a);
    }//syscallPid
    
    /**
     * syscallCoreDump
     * prints current register states and last 3 things pushed on the stack to the terminal
     *
     * @param void
     *
     * @return void
     */
    private void syscallCoreDump()
    {
        System.out.println("CORE DUMP: ");
        m_CPU.regDump();
        int i = 0;
        while (i < 3 && m_CPU.getSP() > 0)
        {
            System.out.println("Stack " + (m_CPU.getSP()) + " " + m_CPU.popFromStack());
            i++;
        }
        syscallExit();
    }//syscallCoreDump
    
    /**
     * syscallHelper
     * checks to see if device exists in m_device and
     * returns that device
     * 
     * @param void
     * 
     * @return DeviceInfo
     */
    private DeviceInfo syscallHelper()
    {
	      int dNUM =  m_CPU.popFromStack();
	   	  DeviceInfo temp = null;
	   	  for (DeviceInfo d : m_devices)
	   	  {
	   		  if (d.getId() == dNUM )
	   		  {
	   			  temp = d;
	   			  break;
	   		  }
	   	  }
	   	  return temp;
   	  
    }//syscallhelper
    
    /**
     * syscallOpen
     * opens a device specified by the stack and handles 
     * some mistakes by pushing error codes
     * if it succeeds it pushes success to the stack
     * 
     * @param void
     * 
     * @return void
     */   
   private void syscallOpen()
   {
	  DeviceInfo d = syscallHelper();
	  if (d == null)
	  {
		  m_CPU.pushToStack(CODE_NO_DEVICE);
		  return;
	  }
	  boolean share = d.getDevice().isSharable();
	  boolean unused = d.unused();
	  if (!share && !unused)
	  {
		  if(d.containsProcess(m_currProcess))
		  {
			  m_CPU.pushToStack(CODE_ALREADY_OPEN);
			  return;
		  }
		  else
		  {
			  d.addProcess(this.m_currProcess);
			  m_currProcess.block(m_CPU, d.getDevice(), SYSCALL_OPEN,-1);
			  m_CPU.pushToStack(CODE_SUCCESS);
			  scheduleNewProcess();
			  return;
		  }
	  }
	  d.addProcess(this.m_currProcess);
	  m_CPU.pushToStack(CODE_SUCCESS);
   }//syscallOpen
   
   /**
    * syscallClose
    * closes a device specified by the stack and handles 
    * some mistakes by pushing error codes
    * if it succeeds it pushes success to the stack
    * 
    * @param void
    * 
    * @return void
    */ 
   private void syscallClose()
   {
	   DeviceInfo d = syscallHelper();
	   if (d == null)
	   {
		   m_CPU.pushToStack(CODE_NO_DEVICE);
		  return;
	   }
	   if(!d.containsProcess(m_currProcess))
	   {
		   m_CPU.pushToStack(CODE_NOT_OPENED);
		   return;
	   }
	d.removeProcess(this.m_currProcess);
	ProcessControlBlock temp = selectBlockedProcess(d.getDevice(), SYSCALL_OPEN, -1);
	if (temp != null)
	{
		temp.unblock();
	}
	m_CPU.pushToStack(CODE_SUCCESS);

   }//close
   
   /**
    * syscallWrite
    * writes to a device specified by the stack at the location and 
    * with the value specified by the stack and handles 
    * some mistakes by pushing error codes
    * it then blocks the process to wait for a result
    * 
    * @param void
    * 
    * @return void
    */ 
   private void syscallWrite()
   {
	   int value = m_CPU.popFromStack();
	   int address = m_CPU.popFromStack();
	   DeviceInfo d = syscallHelper();
	   if (d == null)
	   {
		   m_CPU.pushToStack(CODE_NO_DEVICE);
		  return;
	   }
	   if(!d.containsProcess(m_currProcess))
	   {
		   m_CPU.pushToStack(CODE_NOT_OPENED);
		   return;
	   }
	   if(!d.getDevice().isWriteable())
	   {
		   m_CPU.pushToStack(CODE_NOT_WRITEABLE);
		   return;
	   }
	   if(d.getDevice().isAvailable())
	   {
		   d.getDevice().write(address,value);
		   m_currProcess.block(m_CPU, d.device, SYSCALL_READ, address);
	   }
	   else //not available
	   {
		   m_CPU.setPC(m_CPU.getPC() - CPU.INSTRSIZE);   
		   m_CPU.pushToStack(d.getId());
		   m_CPU.pushToStack(address);
		   m_CPU.pushToStack(value);
		   m_CPU.pushToStack(SYSCALL_WRITE);
	   }
	   scheduleNewProcess();


   }
   
   /**
    * syscallOpen
    * reads from a device at a location
    * specified by the stack and handles 
    * some mistakes by pushing error codes
    * it then blocks the process to wait for IO
    * 
    * @param void
    * 
    * @return void
    */ 
   private void syscallRead()
   {
	   int address = m_CPU.popFromStack();
	   DeviceInfo d = syscallHelper();
	   if (d == null)
	   {
		   m_CPU.pushToStack(CODE_NO_DEVICE);
		  return;
	   }
	   if(!d.containsProcess(m_currProcess))
	   {
		   m_CPU.pushToStack(CODE_NOT_OPENED);
		   return;
	   }
	   if(!d.getDevice().isReadable())
	   {
		   m_CPU.pushToStack(CODE_NOT_READABLE);
		   return;
	   }
	   if(d.getDevice().isAvailable())
	   {
		   d.getDevice().read(address);
		   m_currProcess.block(m_CPU, d.device, SYSCALL_READ, address);
	   }
	   else //not available
	   {
		   m_CPU.setPC(m_CPU.getPC() - CPU.INSTRSIZE);
		   
		   m_CPU.pushToStack(d.getId());
		   m_CPU.pushToStack(address);
		   m_CPU.pushToStack(SYSCALL_READ);
	   }
	   scheduleNewProcess();

   }
   
   
   /**
    * syscallExec
    *
    * creates a new process.  The program used to create that process is chosen
    * semi-randomly from all the programs that have been registered with the OS
    * via {@link #addProgram}.  Limits are put into place to ensure that each
    * process is run an equal number of times.  If no programs have been
    * registered then the simulation is aborted with a fatal error.
    * 
    * @param void
    * 
    * @return void
    *
    */
   private void syscallExec()
   {
       //If there is nothing to run, abort.  This should never happen.
       if (m_programs.size() == 0)
       {
           System.err.println("ERROR!  syscallExec has no programs to run.");
           System.exit(-1);
       }
       
       //find out which program has been called the least and record how many
       //times it has been called
       int leastCallCount = m_programs.get(0).callCount;
       for(Program prog : m_programs)
       {
           if (prog.callCount < leastCallCount)
           {
               leastCallCount = prog.callCount;
           }
       }

       //Create a vector of all programs that have been called the least number
       //of times
       Vector<Program> cands = new Vector<Program>();
       for(Program prog : m_programs)
       {
           cands.add(prog);
       }
       
       //Select a random program from the candidates list
       Random rand = new Random();
       int pn = rand.nextInt(m_programs.size());
       Program prog = cands.get(pn);

       //Determine the address space size using the default if available.
       //Otherwise, use a multiple of the program size.
       int allocSize = prog.getDefaultAllocSize();
       if (allocSize <= 0)
       {
           allocSize = prog.getSize() * 2;
       }

       //Load the program into RAM
       createProcess(prog, allocSize);

       //Adjust the PC since it's about to be incremented by the CPU
       m_CPU.setPC(m_CPU.getPC() - CPU.INSTRSIZE);

   }//syscallExec


/**
 * syscallYield
 * 
 *    changes the state of m_currProcess from running to ready
 *    
 *    @param void
 *    
 *    @return void
 */
   private void syscallYield()
   {
	   scheduleNewProcess();
   }//syscallYield



   
   /**
    * selectBlockedProcess
    *
    * select a process to unblock that might be waiting to perform a given
    * action on a given device.  This is a helper method for system calls
    * and interrupts that deal with devices.
    *
    * @param dev   the Device that the process must be waiting for
    * @param op    the operation that the process wants to perform on the
    *              device.  Use the SYSCALL constants for this value.
    * @param addr  the address the process is reading from.  If the
    *              operation is a Write or Open then this value can be
    *              anything
    *
    * @return the process to unblock -OR- null if none match the given criteria
    */
   public ProcessControlBlock selectBlockedProcess(Device dev, int op, int addr)
   {
       ProcessControlBlock selected = null;
       for(ProcessControlBlock pi : m_processes)
       {
           if (pi.isBlockedForDevice(dev, op, addr))
           {
               selected = pi;
               break;
           }
       }//for

       return selected;
   }//selectBlockedProcess
   

    /**
     * registerDevice
     *
     * adds a new device to the list of devices managed by the OS
     *
     * @param dev     the device driver
     * @param id      the id to assign to this device
     * 
     */
    public void registerDevice(Device dev, int id)
    {
        m_devices.add(new DeviceInfo(dev, id));
    }//registerDevice
    
  //======================================================================
    // Inner Classes
    //----------------------------------------------------------------------

    /**
     * class MemBlock
     *
     * This class contains relevant info about a memory block in RAM.
     *
     */
    private class MemBlock implements Comparable<MemBlock>
    {
        /** the address of the block */
        private int m_addr;
        /** the size of the block */
        private int m_size;

        /**
         * ctor does nothing special
         */
        public MemBlock(int addr, int size)
        {
            m_addr = addr;
            m_size = size;
        }

        /** accessor methods */
        public int getAddr() { return m_addr; }
        public int getSize() { return m_size; }
        
        /**
         * compareTo              
         *
         * compares this to another MemBlock object based on address
         */
        public int compareTo(MemBlock m)
        {
            return this.m_addr - m.m_addr;
        }

    }//class MemBlock

    /*======================================================================
     * Memory Block Management Methods
     *----------------------------------------------------------------------
     */
 

    //<insert method header here>
    private int allocBlock(int size)
    {    	
    	int totalSpace = 0;
    	MemBlock finalLoc = null;
    	getFree();
    	for(MemBlock i : m_freeList)
    	{
    		totalSpace += i.m_size;
    		if(i.m_size >= size)
    		{
    			finalLoc = i;
    			break;
    		}
    	}
    	if(finalLoc == null)
    	{
    		if (totalSpace < size)
    		{
    			return -1;
    		}
    		m_currProcess.save(m_CPU);
    		defragment();
    		return(allocBlock(size));
    	}
        System.out.println("Allocating space at " + finalLoc.m_addr + " of size " + size);
    	return finalLoc.m_addr;
    }//allocBlock
    
    private void getFree()
    {
    	boolean[] used = new boolean[m_RAM.getSize()];
    	m_currProcess.save(m_CPU);
    	for (ProcessControlBlock i : m_processes)
    	{		
    		for(int j = i.registers[CPU.BASE]; j <= i.registers[CPU.LIM]; j++)
    		{
    			used[j] = true;
    		}
    	}
    	int start = 0;
    	int size = 0;
    	m_freeList.removeAllElements();

    	for(int i = 0; i < used.length; i++)
    	{
    		if(!used[i])
    		{
    			start = i;
    		}
    		while (!used[i])
    		{
    			size++;
    			i++;
    			if(i >= used.length)
    			{
    				break;
    			}
    		}
    		if(size > 0  )
        	{
        		m_freeList.addElement(new MemBlock(start,size));
        	}
        	size = 0;
    	}
    }
    
    private void defragment()
    {
    	int nextLoc = 0;
    	Vector<ProcessControlBlock> sortedProcesses = sort();
    	for(ProcessControlBlock i : sortedProcesses)
    	{
    		i.move(nextLoc);
    		nextLoc = i.getRegisterValue(CPU.LIM) + 1;
    	}
    	getFree();
    }
    private Vector<ProcessControlBlock> sort()
    {
    	Collections.sort(m_processes);
    	return m_processes;
//    	Vector<ProcessControlBlock> sorted = new Vector<ProcessControlBlock>();
//    	for(ProcessControlBlock i : m_processes)
//    	{
//    		sorted.add(i);
//    	}
//    	for(int i = 0; i < sorted.size(); i++)
//    	{
//    		ProcessControlBlock first = sorted.get(i);
//    		for(int j = i+1; j < sorted.size(); j++)
//    		{
//    			if(sorted.get(j).getRegisterValue(CPU.BASE) < first.getRegisterValue(CPU.BASE))
//    			{
//    				first = sorted.get(j);
//    			}
//    		}
//    		sorted.set(sorted.indexOf(first),sorted.get(i));
//    		sorted.set(i, first);
//    	}
//		return sorted;
    	
    }
    

    //<insert method header here>
    private void freeCurrProcessMemBlock()
    {
    	getFree();    	
    }//freeCurrProcessMemBlock
  
    
    /**
     * printMemAlloc                 *DEBUGGING*
     *
     * outputs the contents of m_freeList and m_processes to the console and
     * performs a fragmentation analysis.  It also prints the value in
     * RAM at the BASE and LIMIT registers.  This is useful for
     * tracking down errors related to moving process in RAM.
     *
     * SIDE EFFECT:  The contents of m_freeList and m_processes are sorted.
     *
     */
    private void printMemAlloc()
    {
        //If verbose mode is off, do nothing
        if (!m_verbose) return;

        //Print a header
        System.out.println("\n----------========== Memory Allocation Table ==========----------");
        
        //Sort the lists by address
        Collections.sort(m_processes);
        Collections.sort(m_freeList);

        //Initialize references to the first entry in each list
        MemBlock m = null;
        ProcessControlBlock pi = null;
        ListIterator<MemBlock> iterFree = m_freeList.listIterator();
        ListIterator<ProcessControlBlock> iterProc = m_processes.listIterator();
        if (iterFree.hasNext()) m = iterFree.next();
        if (iterProc.hasNext()) pi = iterProc.next();

        //Loop over both lists in order of their address until we run out of
        //entries in both lists
        while ((pi != null) || (m != null))
        {
            //Figure out the address of pi and m.  If either is null, then assign
            //them an address equivalent to +infinity
            int pAddr = Integer.MAX_VALUE;
            int mAddr = Integer.MAX_VALUE;
            if (pi != null)  pAddr = pi.getRegisterValue(CPU.BASE);
            if (m != null)  mAddr = m.getAddr();

            //If the process has the lowest address then print it and get the
            //next process
            if ( mAddr > pAddr )
            {
                int size = pi.getRegisterValue(CPU.LIM) - pi.getRegisterValue(CPU.BASE);
                System.out.print(" Process " + pi.processId +  " (addr=" + pAddr + " size=" + size + " words)");
                System.out.print(" @BASE=" + m_RAM.read(pi.getRegisterValue(CPU.BASE))
                                 + " @SP=" + m_RAM.read(pi.getRegisterValue(CPU.SP)));
                System.out.println();
                if (iterProc.hasNext())
                {
                    pi = iterProc.next();
                }
                else
                {
                    pi = null;
                }
            }//if
            else
            {
                //The free memory block has the lowest address so print it and
                //get the next free memory block
                System.out.println("    Open(addr=" + mAddr + " size=" + m.getSize() + ")");
                if (iterFree.hasNext())
                {
                    m = iterFree.next();
                }
                else
                {
                    m = null;
                }
            }//else
        }//while
            
        //Print a footer
        System.out.println("-----------------------------------------------------------------");
        
    }//printMemAlloc


    /**
     * class ProcessControlBlock
     *
     * This class contains information about a currently active process.
     */
    private class ProcessControlBlock implements Comparable<ProcessControlBlock>
    {
        /**
         * a unique id for this process
         */
        private int processId = 0;
        /**
         * These are the process' current registers.  If the process is in the
         * "running" state then these are out of date
         */
        private int[] registers = null;

        /**
         * If this process is blocked a reference to the Device is stored here
         */
        private Device blockedForDevice = null;
        
        /**
         * If this process is blocked a reference to the type of I/O operation
         * is stored here (use the SYSCALL constants defined in SOS)
         */
        private int blockedForOperation = -1;
        
        /**
         * If this process is blocked reading from a device, the requested
         * address is stored here.
         */
        private int blockedForAddr = -1;
        
        /**
         * the time it takes to load and save registers, specified as a number
         * of CPU ticks
         */
        private static final int SAVE_LOAD_TIME = 30;
        
        /**
         * Used to store the system time when a process is moved to the Ready
         * state.
         */
        private int lastReadyTime = -1;
        
        /**
         * Used to store the number of times this process has been in the ready
         * state
         */
        private int numReady = 0;
        
        /**
         * Used to store the maximum starve time experienced by this process
         */
        private int maxStarve = -1;
        
        /**
         * Used to store the average starve time for this process
         */
        private double avgStarve = 0;
        
        

        /**
         * save
         *
         * saves the current CPU registers into this.registers
         *
         * @param cpu  the CPU object to save the values from
         */
        public void save(CPU cpu)
        {
            //A context switch is expensive.  We simluate that here by 
            //adding ticks to m_CPU
            m_CPU.addTicks(SAVE_LOAD_TIME);
            
            //Save the registers
            int[] regs = cpu.getRegisters();
            this.registers = new int[CPU.NUMREG];
            for(int i = 0; i < CPU.NUMREG; i++)
            {
                this.registers[i] = regs[i];
            }

            //Assuming this method is being called because the process is moving
            //out of the Running state, record the current system time for
            //calculating starve times for this process.  If this method is
            //being called for a Block, we'll adjust lastReadyTime in the
            //unblock method.
            numReady++;
            lastReadyTime = m_CPU.getTicks();
            
        }//save
         
        /**
         * restore
         *
         * restores the saved values in this.registers to the current CPU's
         * registers
         *
         * @param cpu  the CPU object to restore the values to
         */
        public void restore(CPU cpu)
        {
            //A context switch is expensive.  We simluate that here by 
            //adding ticks to m_CPU
            m_CPU.addTicks(SAVE_LOAD_TIME);
            
            //Restore the register values
            int[] regs = cpu.getRegisters();
            for(int i = 0; i < CPU.NUMREG; i++)
            {
                regs[i] = this.registers[i];
            }

            //Record the starve time statistics
            int starveTime = m_CPU.getTicks() - lastReadyTime;
            if (starveTime > maxStarve)
            {
                maxStarve = starveTime;
            }
            double d_numReady = (double)numReady;
            avgStarve = avgStarve * (d_numReady - 1.0) / d_numReady;
            avgStarve = avgStarve + (starveTime * (1.0 / d_numReady));
        }//restore
         
        /**
         * unblock
         *
         * moves this process from the Blocked (waiting) state to the Ready
         * state. 
         *
         */
        public void unblock()
        {
            //Reset the info about the block
            blockedForDevice = null;
            blockedForOperation = -1;
            blockedForAddr = -1;
            
            //Assuming this method is being called because the process is moving
            //from the Blocked state to the Ready state, record the current
            //system time for calculating starve times for this process.
            lastReadyTime = m_CPU.getTicks();
            
        }//unblock
         
        /**
         * block
         *
         * blocks the current process to wait for I/O.  The caller is
         * responsible for calling {@link CPU#scheduleNewProcess}
         * after calling this method.
         *
         * @param cpu   the CPU that the process is running on
         * @param dev   the Device that the process must wait for
         * @param op    the operation that the process is performing on the
         *              device.  Use the SYSCALL constants for this value.
         * @param addr  the address the process is reading from (for SYSCALL_READ)
         * 
         */
        public void block(CPU cpu, Device dev, int op, int addr)
        {
            blockedForDevice = dev;
            blockedForOperation = op;
            blockedForAddr = addr;
            
        }//block
        
        /**
         * isBlocked
         *
         * @return true if the process is blocked
         */
        public boolean isBlocked()
        {
            return (blockedForDevice != null);
        }//isBlocked
         
        /**
         * isBlockedForDevice
         *
         * Checks to see if the process is blocked for the given device,
         * operation and address.  If the operation is not an open, the given
         * address is ignored.
         *
         * @param dev   check to see if the process is waiting for this device
         * @param op    check to see if the process is waiting for this operation
         * @param addr  check to see if the process is reading from this address
         *
         * @return true if the process is blocked by the given parameters
         */
        public boolean isBlockedForDevice(Device dev, int op, int addr)
        {
            if ( (blockedForDevice == dev) && (blockedForOperation == op) )
            {
                if (op == SYSCALL_OPEN)
                {
                    return true;
                }

                if (addr == blockedForAddr)
                {
                    return true;
                }
            }//if

            return false;
        }//isBlockedForDevice
         
        /**
         * getRegisterValue
         *
         * Retrieves the value of a process' register that is stored in this
         * object (this.registers).
         * 
         * @param idx the index of the register to retrieve.  Use the constants
         *            in the CPU class
         * @return one of the register values stored in in this object or -999
         *         if an invalid index is given 
         */
        public int getRegisterValue(int idx)
        {
            if ((idx < 0) || (idx >= CPU.NUMREG))
            {
                return -999;    // invalid index
            }
            
            return this.registers[idx];
        }//getRegisterValue
         
        /**
         * setRegisterValue
         *
         * Sets the value of a process' register that is stored in this
         * object (this.registers).  
         * 
         * @param idx the index of the register to set.  Use the constants
         *            in the CPU class.  If an invalid index is given, this
         *            method does nothing.
         * @param val the value to set the register to
         */
        public void setRegisterValue(int idx, int val)
        {
            if ((idx < 0) || (idx >= CPU.NUMREG))
            {
                return;    // invalid index
            }
            
            this.registers[idx] = val;
        }//setRegisterValue
         
        /**
         * compareTo              
         *
         * compares this to another ProcessControlBlock object based on the BASE addr
         * register.  Read about Java's Collections class for info on
         * how this method can be quite useful to you.
         */
        public int compareTo(ProcessControlBlock pi)
        {
            return this.registers[CPU.BASE] - pi.registers[CPU.BASE];
        }

        
        /**
         * 
         * @param newBase
         * Takes a location in ram and moves this process to that location
         * copying line by line and updates the register values for the
         * new location of the program
         *
         * @return boolean success of moving code
         */
        public boolean move(int newBase)
        {
        	if(this == m_currProcess)
        	{
        		save(m_CPU);
        	}
            int oBase = registers[CPU.BASE];
            int oLim = registers[CPU.LIM];
            int progSize = oLim - oBase;
        	if(newBase < 0 || newBase + progSize > m_RAM.getSize())
        	{
        		return false;
        	}
        	if(newBase > oBase)
        	{
        		for(int i = progSize; i >= 0; i--)
        		{
        			m_RAM.write(newBase+i, m_RAM.read(i+oBase));
        		}
        	}
        	else
        	{
        		for(int i = 0; i <= progSize; i++)
        		{
        			m_RAM.write(newBase+i, m_RAM.read(i+oBase));
        		}
        	}
        	registers[CPU.BASE] += newBase - oBase;
            registers[CPU.LIM] += newBase- oBase;
            registers[CPU.PC] += newBase - oBase;
            registers[CPU.SP] += newBase - oBase;	

        	if(this == m_currProcess)
        	{
        		restore(m_CPU);
        	} 
        	
        	debugPrintln("Process " + getProcessId() + " moved from " + oBase + " to " + newBase);
        	return true;
        }//move


        /**
         * constructor
         *
         * @param pid        a process id for the process.  The caller is
         *                   responsible for making sure it is unique.
         */
        public ProcessControlBlock(int pid)
        {
            this.processId = pid;
        }
        
        /**
         * copy constructor
         *
         * @param pid        a process id for the process.  The caller is
         *                   responsible for making sure it is unique.
         */
        public ProcessControlBlock(ProcessControlBlock temp)
        {
            this.processId = temp.processId;

            this.registers = temp.registers;

            this.blockedForDevice = temp.blockedForDevice;

            this.blockedForOperation = temp.blockedForOperation;

            this.lastReadyTime = temp.lastReadyTime;

            this.numReady = temp.numReady;
            
            this.maxStarve = temp.maxStarve;

            this.avgStarve = temp.avgStarve;
        }
        

        /**
         * @return the current process' id
         */
        public int getProcessId()
        {
            return this.processId;
        }
        

        /**
         * @return the last time this process was put in the Ready state
         */
        public long getLastReadyTime()
        {
            return lastReadyTime;
        }
        
        /**
         * overallAvgStarve
         *
         * @return the overall average starve time for all currently running
         *         processes
         *
         */
        public double overallAvgStarve()
        {
            double result = 0.0;
            int count = 0;
            for(ProcessControlBlock pi : m_processes)
            {
                if (pi.avgStarve > 0)
                {
                    result = result + pi.avgStarve;
                    count++;
                }
            }
            if (count > 0)
            {
                result = result / count;
            }
            
            return result;
        }//overallAvgStarve
         
        /**
         * toString       **DEBUGGING**
         *
         * @return a string representation of this class
         */
        public String toString()
        {
            //Print the Process ID and process state (READY, RUNNING, BLOCKED)
            String result = "Process id " + processId + " ";
            if (isBlocked())
            {
                result = result + "is BLOCKED for ";
                //Print device, syscall and address that caused the BLOCKED state
                if (blockedForOperation == SYSCALL_OPEN)
                {
                    result = result + "OPEN";
                }
                else
                {
                    result = result + "WRITE @" + blockedForAddr;
                }
                for(DeviceInfo di : m_devices)
                {
                    if (di.getDevice() == blockedForDevice)
                    {
                        result = result + " on device #" + di.getId();
                        break;
                    }
                }
                result = result + ": ";
            }
            else if (this == m_currProcess)
            {
                result = result + "is RUNNING: ";
            }
            else
            {
                result = result + "is READY: ";
            }

            //Print the register values stored in this object.  These don't
            //necessarily match what's on the CPU for a Running process.
            if (registers == null)
            {
                result = result + "<never saved>";
                return result;
            }
            
            for(int i = 0; i < CPU.NUMGENREG; i++)
            {
                result = result + ("r" + i + "=" + registers[i] + " ");
            }//for
            result = result + ("PC=" + registers[CPU.PC] + " ");
            result = result + ("SP=" + registers[CPU.SP] + " ");
            result = result + ("BASE=" + registers[CPU.BASE] + " ");
            result = result + ("LIM=" + registers[CPU.LIM] + " ");

            //Print the starve time statistics for this process
            result = result + "\n\t\t\t";
            result = result + " Max Starve Time: " + maxStarve;
            result = result + " Avg Starve Time: " + avgStarve;
        
            return result;
        }//toString
        
    }//class ProcessControlBlock

    /**
     * class DeviceInfo
     *
     * This class contains information about a device that is currently
     * registered with the system.
     */
    private class DeviceInfo
    {
        /** every device has a unique id */
        private int id;
        /** a reference to the device driver for this device */
        private Device device;
        /** a list of processes that have opened this device */
        private Vector<ProcessControlBlock> procs;

        /**
         * constructor
         *
         * @param d          a reference to the device driver for this device
         * @param initID     the id for this device.  The caller is responsible
         *                   for guaranteeing that this is a unique id.
         */
        public DeviceInfo(Device d, int initID)
        {
            this.id = initID;
            this.device = d;
            d.setId(initID);
            this.procs = new Vector<ProcessControlBlock>();
        }

        /** @return the device's id */
        public int getId()
        {
            return this.id;
        }

        /** @return this device's driver */
        public Device getDevice()
        {
            return this.device;
        }

        /** Register a new process as having opened this device */
        public void addProcess(ProcessControlBlock pi)
        {
            procs.add(pi);
        }
        
        /** Register a process as having closed this device */
        public void removeProcess(ProcessControlBlock pi)
        {
            procs.remove(pi);
        }

        /** Does the given process currently have this device opened? */
        public boolean containsProcess(ProcessControlBlock pi)
        {
            return procs.contains(pi);
        }
        
        /** Is this device currently not opened by any process? */
        public boolean unused()
        {
            return procs.size() == 0;
        }
        
    }//class DeviceInfo        
    
};//class SOS
