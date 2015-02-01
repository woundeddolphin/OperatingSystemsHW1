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
 *  
 * @author Zach Pearson
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
    
    //======================================================================
    //Member variables
    //----------------------------------------------------------------------

	
    /**
     * This flag causes the SOS to print lots of potentially helpful
     * status messages
     **/
    public static final boolean m_verbose = false;
    
    /**
     * The CPU the operating system is managing.
     **/
    private CPU m_CPU = null;
    
    /**
     * The RAM attached to the CPU.
     **/
    private RAM m_RAM = null;

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

    @Override
	public void interruptIllegalMemoryAccess(int addr) {
		// TODO Auto-generated method stub
		System.out.println("Illegal Memory Access Exception!");
        System.exit(0);
	}

	@Override
	public void interruptDivideByZero() {
		// TODO Auto-generated method stub
		System.out.println("Illegal Divide by Zero Exception!");
        System.exit(0);
	}

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
	 * call backs for handling trap from CPU
	 * 
	 */

	@Override
	public void systemCall()
	{
		//TODO
		int opCode = m_CPU.popFromStack();
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
		default:
			break;
		}
	}
	private void exit()
	{
		if(m_verbose)
		{
			System.out.println("Exit handled!");
		}
	    System.exit(0);
	}
	private void output()
	{
		int a = m_CPU.popFromStack();
		System.out.println("OUTPUT: " + a);
	}
	private void pid ()
	{
		int a = 42;
		m_CPU.pushToStack2(a);
		if(m_verbose)
		{
			System.out.println("PID = " + a);
		}

	}
	private void coreDump()
	{
		System.out.println("CORE DUMP: ");
		m_CPU.regDump();
		for (int i = 0; i < 3; i++)
		{
			System.out.println("Stack " + i + " " + m_CPU.popFromStack());;
		}
		exit();
	}
		
    
};//class SOS
