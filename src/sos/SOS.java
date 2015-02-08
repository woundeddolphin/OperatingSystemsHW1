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

    //None yet!
    
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
        int location = 8;
        
        for(int i = 0; i < programArray.length; i++){ //move the program into ram
            m_RAM.write(location + i, programArray[i]);
        }
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
    }
    

    


    /*======================================================================
     * Interrupt Handlers
     *----------------------------------------------------------------------
     */
    
    /**
     * interruptIllegalMemoryAcess
     * Prints error message when useer touches memory that is not theirs.
     *
     * @param addr the address that was trying to be accessed
     * @return void
     */
    @Override
    public void interruptIllegalMemoryAccess(int addr) {
        // TODO Auto-generated method stub
        System.out.println("Illegal Memory Access Exception!");
        System.exit(0);
    }
    
    /**
     * interruptDivideByZero
     * Prints error message if division by zero is encountered
     * 
     * @param void
     * @return void
     */
    @Override
    public void interruptDivideByZero() {
        // TODO Auto-generated method stub
        System.out.println("Illegal Divide by Zero Exception!");
        System.exit(0);
    }

    /**
     * interuptIllegalInstruction
     * Prints error message if there is something wrong with the fetched instruction
     * 
     * @param insr The bad instruction
     * @return void
     */
    @Override
    public void interruptIllegalInstruction(int[] instr) {
        // TODO Auto-generated method stub
        System.out.println("Illegal Instruction Exception!");
        System.exit(0);
        
    }
    
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
                exit();
                break;
            case SYSCALL_OUTPUT:
                output();
                break;
            case SYSCALL_GETPID:
                pid();
                break;
            case SYSCALL_COREDUMP:
                coreDump();
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
            default:
                break;
        }
    }
    /**
     * exit
     * Current exits the simulation
     *
     * @param void
     * @return void
     */
    private void exit()
    {
        if(m_verbose)
        {
            System.out.println("Exit handled!");
        }
        System.exit(0);
    }
    /**
     * output
     * prints pops parameter from stack and prints to terminal
     *
     * @param void the parameter is the last thing pushed on the stack
     *
     * @return void
     */
    private void output()
    {
        int a = m_CPU.popFromStack();
        System.out.println("OUTPUT: " + a);
    }
    /**
     * pid
     * pushes program id onto the stack
     *
     * @param void
     *
     * @return void return value is pushed onto the stack
     */
    private void pid ()
    {
        int a = 42;
        m_CPU.pushToStack2(a);
        if(m_verbose)
        {
            System.out.println("PID = " + a);
        }

    }
    /**
     * coreDump
     * prints current register states and last 3 things pushed on the stack to the terminal
     *
     * @param void
     *
     * @return void
     */
    private void coreDump()
    {
        System.out.println("CORE DUMP: ");
        m_CPU.regDump();
        int i = 0;
        while (i < 3 && m_CPU.getSP() > 0)
        {
            System.out.println("Stack " + (m_CPU.getSP()) + " " + m_CPU.popFromStack());
            i++;
        }
        exit();
    }
    
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
   	  
    }
    
    
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
			  m_CPU.pushToStack2(CODE_NOT_SHARABLE);
			  return;
		  }
	  }
	  d.addProcess(this.m_currProcess);
	  m_CPU.pushToStack2(CODE_SUCCESS);
   }
   
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
	m_CPU.pushToStack2(CODE_SUCCESS);

   }
   
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
