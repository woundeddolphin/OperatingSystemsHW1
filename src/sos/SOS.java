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
     * This variable contains the position where the next program will be
     * loaded (when the createProcess method is called)
     */
    int m_nextLoadPos;
    
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
        m_nextLoadPos = 0;
        m_nextProcessID = 1001;
        m_processes = new Vector<ProcessControlBlock>();
        
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
    	debugPrintln("removed process: " + m_currProcess.getProcessId());
    	m_processes.remove(m_currProcess);
    	scheduleNewProcess();
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
	 * scheduleNewProcess
	 * 
	 * Selects a new process to run and runs it
	 */
    public void scheduleNewProcess()
    {
    	
    	if (m_processes.isEmpty())
    	{

    		debugPrintln("No more processes to run. Stopping.");
    		System.exit(CODE_SUCCESS);
    	}
    	boolean allBlocked = true;
    	for(ProcessControlBlock i: m_processes)
    	{
    		if (!i.isBlocked())
    		{
    			allBlocked = false;
    			break;
    		}
    	}
    	if(allBlocked)
    	{
    		debugPrintln("All proccesses blocked! " + "This shouldn't happen! (yet)");
    		System.exit(-1);
    	}
    	
    	int sameCheck = m_currProcess.getProcessId(); // used to check if new pid is same as old
    	m_currProcess.save(m_CPU);
    	m_currProcess = getRandomProcess();
    	
    	if (m_currProcess.getProcessId() != sameCheck) // checks if new pid is same as old
    	{
    		debugPrintln("Switched to process " + m_currProcess.getProcessId());
    	}
    	m_currProcess.restore(m_CPU);
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
        
        //set the location for the allocation to be at the next location
        int location = m_nextLoadPos;
        
        //set the next location for next time
        m_nextLoadPos += allocSize;
        
        
        if (m_nextLoadPos > m_RAM.getSize())
        {
        	debugPrintln("ERROR: Not enough avaliable RAM: " + m_nextLoadPos + " out of " + m_RAM.getSize());
        	System.exit(-1);
        }
        
        for(int i = 0; i < programArray.length; i++){ //move the program into ram
            m_RAM.write(location + i, programArray[i]);
        }
        if (m_currProcess != null)
        {
        	m_currProcess.save(m_CPU);
        }
        
        m_currProcess = new ProcessControlBlock(m_nextProcessID);
        m_processes.add(m_currProcess);
        m_nextProcessID++;
        
        debugPrintln("Installed program of size " + allocSize + " with process id " + m_currProcess.getProcessId() 
        		+ " at position " + (m_nextLoadPos - allocSize) );
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
        m_CPU.setLIM(loc + size);
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

        debugPrintln("Removing process with id " + m_currProcess.getProcessId() + " at " + m_CPU.getBASE());
        m_processes.remove(m_currProcess);
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
        m_CPU.pushToStack2(a);

        debugPrintln("PID = " + a);
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
		  m_CPU.pushToStack2(CODE_NO_DEVICE);
		  return;
	  }
	  boolean share = d.getDevice().isSharable();
	  boolean unused = d.unused();
	  if (!share && !unused)
	  {
		  if(d.containsProcess(m_currProcess))
		  {
			  m_CPU.pushToStack2(CODE_ALREADY_OPEN);
			  return;
		  }
		  else
		  {
			  d.addProcess(this.m_currProcess);
			  m_currProcess.block(m_CPU, d.getDevice(), SYSCALL_OPEN,-1);
			  m_CPU.pushToStack2(CODE_SUCCESS);
			  scheduleNewProcess();
			  return;
		  }
	  }
	  d.addProcess(this.m_currProcess);
	  m_CPU.pushToStack2(CODE_SUCCESS);
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
		   m_CPU.pushToStack2(CODE_NO_DEVICE);
		  return;
	   }
	   if(!d.containsProcess(m_currProcess))
	   {
		   m_CPU.pushToStack2(CODE_NOT_OPENED);
		   return;
	   }
	d.removeProcess(this.m_currProcess);
	ProcessControlBlock temp = selectBlockedProcess(d.getDevice(), SYSCALL_OPEN, -1);
	if (temp != null)
	{
		temp.unblock();
	}
	m_CPU.pushToStack2(CODE_SUCCESS);

   }//close
   
   /**
    * syscallOpen
    * writes to a device specified by the stack at the location and 
    * with the value specified by the stack and handles 
    * some mistakes by pushing error codes
    * if it succeeds it pushes success to the stack
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
		   m_CPU.pushToStack2(CODE_NO_DEVICE);
		  return;
	   }
	   if(!d.containsProcess(m_currProcess))
	   {
		   m_CPU.pushToStack2(CODE_NOT_OPENED);
		   return;
	   }
	   if(!d.getDevice().isWriteable())
	   {
		   m_CPU.pushToStack2(CODE_NOT_WRITEABLE);
		   return;
	   }
	   d.getDevice().write(address, value);
	   m_CPU.pushToStack2(CODE_SUCCESS);
   }
   
   /**
    * syscallOpen
    * reads from a device at a location
    * specified by the stack and handles 
    * some mistakes by pushing error codes
    * if it succeeds it pushes success to the stack
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
		   m_CPU.pushToStack2(CODE_NO_DEVICE);
		  return;
	   }
	   if(!d.containsProcess(m_currProcess))
	   {
		   m_CPU.pushToStack2(CODE_NOT_OPENED);
		   return;
	   }
	   if(!d.getDevice().isReadable())
	   {
		   m_CPU.pushToStack2(CODE_NOT_READABLE);
		   return;
	   }
	   m_CPU.pushToStack2(d.getDevice().read(address));
	   m_CPU.pushToStack2(CODE_SUCCESS);
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
     * class ProcessControlBlock
     *
     * This class contains information about a currently active process.
     */
    private class ProcessControlBlock
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
         * save
         *
         * saves the current CPU registers into this.registers
         *
         * @param cpu  the CPU object to save the values from
         */
        public void save(CPU cpu)
        {
            int[] regs = cpu.getRegisters();
            this.registers = new int[CPU.NUMREG];
            for(int i = 0; i < CPU.NUMREG; i++)
            {
                this.registers[i] = regs[i];
            }
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
            int[] regs = cpu.getRegisters();
            for(int i = 0; i < CPU.NUMREG; i++)
            {
                regs[i] = this.registers[i];
            }

        }//restore
         
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
         * unblock
         *
         * moves this process from the Blocked (waiting) state to the Ready
         * state. 
         *
         */
        public void unblock()
        {
            blockedForDevice = null;
            blockedForOperation = -1;
            blockedForAddr = -1;
            
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
         * toString       **DEBUGGING**
         *
         * @return a string representation of this class
         */
        public String toString()
        {
            String result = "Process id " + processId + " ";
            if (isBlocked())
            {
                result = result + "is BLOCKED: ";
            }
            else if (this == m_currProcess)
            {
                result = result + "is RUNNING: ";
            }
            else
            {
                result = result + "is READY: ";
            }

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

            return result;
        }//toString
         
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
         * @return the current process' id
         */
        public int getProcessId()
        {
            return this.processId;
        }
        

        
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
