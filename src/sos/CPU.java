package sos;

import java.util.*;

/**
 * This class is the centerpiece of a simulation of the essential hardware of a
 * microcomputer. This includes a processor chip, RAM and I/O devices. It is
 * designed to demonstrate a simulated operating system (SOS).
 * 
 * @see RAM
 * @see SOS
 * @see Program
 * @see Sim
 * 
 * 
 * @author Justice Nichols
 * @author Krismy Alfaro
 * @author Matthew Farr
 * @author Zach Pearson
 * @author Matt Wellnitz
 * @author Bryce Matsuda
 */

public class CPU implements Runnable {

	// ======================================================================
	// Constants
	// ----------------------------------------------------------------------

	// These constants define the instructions available on the chip
	public static final int SET = 0; /* set value of reg */
	public static final int ADD = 1; // put reg1 + reg2 into reg3
	public static final int SUB = 2; // put reg1 - reg2 into reg3
	public static final int MUL = 3; // put reg1 * reg2 into reg3
	public static final int DIV = 4; // put reg1 / reg2 into reg3
	public static final int COPY = 5; // copy reg1 to reg2
	public static final int BRANCH = 6; // goto address in reg
	public static final int BNE = 7; // branch if not equal
	public static final int BLT = 8; // branch if less than
	public static final int POP = 9; // load value from stack
	public static final int PUSH = 10; // save value to stack
	public static final int LOAD = 11; // load value from heap
	public static final int SAVE = 12; // save value to heap
	public static final int TRAP = 15; // system call

	// These constants define the indexes to each register
	public static final int R0 = 0; // general purpose registers
	public static final int R1 = 1;
	public static final int R2 = 2;
	public static final int R3 = 3;
	public static final int R4 = 4;
	public static final int PC = 5; // program counter
	public static final int SP = 6; // stack pointer
	public static final int BASE = 7; // bottom of currently accessible RAM
	public static final int LIM = 8; // top of accessible RAM
	public static final int NUMREG = 9; // number of registers

	// Misc constants
	public static final int NUMGENREG = PC; // the number of general registers
	public static final int INSTRSIZE = 4; // number of ints in a single instr +
										  // args. (Set to a fixed value for simplicity.)
    public static final int CLOCK_FREQ = 5; /*defines how many CPU cycles elapse 
    										between clock interrupts*/
    

	// ======================================================================
	// Member variables
	// ----------------------------------------------------------------------
	/**
	 * specifies whether the CPU should output details of its work
	 **/
	private boolean m_verbose = false;

	/**
	 * This array contains all the registers on the "chip".
	 **/
	private int m_registers[];

	/**
	 * A pointer to the RAM used by this CPU
	 * 
	 * @see RAM
	 **/
	private RAM m_RAM = null;

	/**
	 * a reference to the trap handler for this CPU.  On a real CPU this would
	 * simply be an address that the PC register is set to.
	 */
	private TrapHandler m_TH = null;

	/**
	 * a reference to the CPUs interrupt controller. 
	 */
	private InterruptController m_IC = null;

	
    /**
     * This variable will count how many
     * CPU cycles have elapsed for the simulation
     */
    
    private int m_ticks;

	// ======================================================================
	// Methods
	// ----------------------------------------------------------------------

	/**
	 * CPU ctor
	 * 
	 * Intializes all member variables.
	 */
	public CPU(RAM ram, InterruptController IC) {
		m_registers = new int[NUMREG];
		for (int i = 0; i < NUMREG; i++) {
			m_registers[i] = 0;
		}
		m_RAM = ram;
		m_IC = IC;

	}// CPU ctor

	/**
	 * registerTrapHandler
	 *
	 * allows SOS to register itself as the trap handler 
	 */
	public void registerTrapHandler(TrapHandler th)
	{
		m_TH = th;
	}

	/**
	 * getPC
	 * 
	 * @return the value of the program counter
	 */
	public int getPC() {
		return m_registers[PC];
	}

	/**
	 * getSP
	 * 
	 * @return the value of the stack pointer
	 */
	public int getSP() {
		return m_registers[SP];
	}

	/**
	 * getBASE
	 * 
	 * @return the value of the base register
	 */
	public int getBASE() {
		return m_registers[BASE];
	}

	/**
	 * getLIMIT
	 * 
	 * @return the value of the limit register
	 */
	public int getLIM() {
		return m_registers[LIM];
	}

	/**
	 * getRegisters
	 * 
	 * @return the registers
	 */
	public int[] getRegisters() {
		return m_registers;
	}

	/**
	 * setPC
	 * 
	 * @param v
	 *            the new value of the program counter
	 */
	public void setPC(int v) {
		m_registers[PC] = v;
	}

	/**
	 * setSP
	 * 
	 * @param v
	 *            the new value of the stack pointer
	 */
	public void setSP(int v) {
		m_registers[SP] = v;
	}

	/**
	 * setBASE
	 * 
	 * @param v
	 *            the new value of the base register
	 */
	public void setBASE(int v) {
		m_registers[BASE] = v;
	}

	/**
	 * setLIM
	 * 
	 * @param v
	 *            the new value of the limit register
	 */
	public void setLIM(int v) {
		m_registers[LIM] = v;
	}

	/**
	 * regDump
	 * 
	 * Prints the values of the registers. Useful for debugging.
	 */
	public void regDump() {
		for (int i = 0; i < NUMGENREG; i++) {
			System.out.print("r" + i + "=" + m_registers[i] + " ");
		}// for
		System.out.print("PC=" + m_registers[PC] + " ");
		System.out.print("SP=" + m_registers[SP] + " ");
		System.out.print("BASE=" + m_registers[BASE] + " ");
		System.out.print("LIM=" + m_registers[LIM] + " ");
		System.out.println("");
	}// regDump

	/**
	 * checkForIOInterrupt
	 *
	 * Checks the databus for signals from the interrupt controller and, if
	 * found, invokes the appropriate handler in the operating system.
	 *
	 */
	private void checkForIOInterrupt()
	{
		//If there is no interrupt to process, do nothing
		if (m_IC.isEmpty())
		{
			return;
		}

		//Retreive the interrupt data
		int[] intData = m_IC.getData();

		//Report the data if in verbose mode
		if (m_verbose)
		{
			System.out.println("CPU received interrupt: type=" + intData[0]
					+ " dev=" + intData[1] + " addr=" + intData[2]
							+ " data=" + intData[3]);
		}

		//Dispatch the interrupt to the OS
		switch(intData[0])
		{
		case InterruptController.INT_READ_DONE:
			m_TH.interruptIOReadComplete(intData[1], intData[2], intData[3]);
			break;
		case InterruptController.INT_WRITE_DONE:
			m_TH.interruptIOWriteComplete(intData[1], intData[2]);
			break;
		default:
			System.out.println("CPU ERROR:  Illegal Interrupt Received.");
			System.exit(-1);
			break;
		}//switch

	}//checkForIOInterrupt

	/**
	 * printIntr
	 * 
	 * Prints a given instruction in a user readable format. Useful for
	 * debugging.
	 * 
	 * @param instr
	 *            the current instruction
	 */
	public static void printInstr(int[] instr) {
		switch (instr[0]) {
		case SET:
			System.out.println("SET R" + instr[1] + " = " + instr[2]);
			break;
		case ADD:
			System.out.println("ADD R" + instr[1] + " = R" + instr[2] + " + R"
					+ instr[3]);
			break;
		case SUB:
			System.out.println("SUB R" + instr[1] + " = R" + instr[2] + " - R"
					+ instr[3]);
			break;
		case MUL:
			System.out.println("MUL R" + instr[1] + " = R" + instr[2] + " * R"
					+ instr[3]);
			break;
		case DIV:
			System.out.println("DIV R" + instr[1] + " = R" + instr[2] + " / R"
					+ instr[3]);
			break;
		case COPY:
			System.out.println("COPY R" + instr[1] + " = R" + instr[2]);
			break;
		case BRANCH:
			System.out.println("BRANCH @" + instr[1]);
			break;
		case BNE:
			System.out.println("BNE (R" + instr[1] + " != R" + instr[2] + ") @"
					+ instr[3]);
			break;
		case BLT:
			System.out.println("BLT (R" + instr[1] + " < R" + instr[2] + ") @"
					+ instr[3]);
			break;
		case POP:
			System.out.println("POP R" + instr[1]);
			break;
		case PUSH:
			System.out.println("PUSH R" + instr[1]);
			break;
		case LOAD:
			System.out.println("LOAD R" + instr[1] + " <-- @R" + instr[2]);
			break;
		case SAVE:
			System.out.println("SAVE R" + instr[1] + " --> @R" + instr[2]);
			break;
		case TRAP:
			System.out.print("TRAP ");
			break;
		default: // should never be reached
			System.out.println("?? ");
			break;
		}// switch

	}// printInstr

	/**
	 * run()
	 * 
	 * this method runs
	 */
	public void run() {
		while (true) {
			// check for interrupt
			checkForIOInterrupt();

			// get next instruction from RAM
			int[] instruction = m_RAM.fetch(getPC());

			// check if in verbose mode
			if (m_verbose) {
				regDump();
				printInstr(instruction);
			}
			// format of instruction: opcode, arg1, arg2, arg3
			switch (instruction[0]) {
			case SET:
				m_registers[instruction[1]] = instruction[2];
				break;

			case ADD:
				// add arg2 and arg3
				m_registers[instruction[1]] = m_registers[instruction[2]]
						+ m_registers[instruction[3]];
				break;

			case SUB:
				// subtract arg2 and arg3
				m_registers[instruction[1]] = m_registers[instruction[2]]
						- m_registers[instruction[3]];
				break;

			case MUL:
				// multiply arg2 and arg3
				m_registers[instruction[1]] = m_registers[instruction[2]]
						* m_registers[instruction[3]];
				break;

			case DIV:
				// divide arg2 and arg3
				if (m_registers[instruction[3]]==0)
				{
					m_TH.interruptDivideByZero();
				}
				else
				{
					m_registers[instruction[1]] = m_registers[instruction[2]]
							/ m_registers[instruction[3]];
				}
				break;

			case COPY:
				//copy the contents of arg3 into arg 2
				m_registers[instruction[1]] = m_registers[instruction[2]];
				break;

			case BRANCH:
				if (checkAccess(instruction[1])) {
					setPC(offset(instruction[1])); // move pc to label then onto
					// next instruction
				}
				break;

			case BNE:
				if (m_registers[instruction[2]] != m_registers[instruction[1]]){
					if (checkAccess(instruction[3])) {
						setPC(offset(instruction[3])); // move pc to label then
						// onto next instruction
					}
				}
				break;

			case BLT:
				if (m_registers[instruction[1]] < m_registers[instruction[2]]) {
					if (checkAccess(instruction[3])) {
						setPC(offset(instruction[3])); // move pc to label then
						// onto next instruction
					}
				}
				break;

			case POP:
				popFromStack(instruction[1]);
				break;

			case PUSH:
				pushToStackR(instruction[1]);
				break;

			case LOAD:
				if (checkAccess((instruction[2] + getBASE()))) {
					m_registers[instruction[1]] = m_RAM.read(instruction[2]
							+ getBASE());
				}
				break;

			case SAVE:
				if (checkAccess((instruction[2] + getBASE()))) {
					m_RAM.write(m_registers[instruction[2]] + getBASE(),
							m_registers[instruction[1]]);
				}
				break;

			case TRAP:
				m_TH.systemCall();
				break;

			default:
				m_TH.interruptIllegalInstruction(instruction);
				break;
			}
			
			// Increment tick count and check for clock interrupt
			addTicks(1);
			if (m_ticks % CLOCK_FREQ == 0){
				m_TH.interruptClock();
			}
			
			// advance the PC register by the instruction size for next
			// instruction
			setPC(getPC() + INSTRSIZE);

		}
	}// run

	/**
	 * checkAccess()
	 * 
	 * @param register
	 *            contents that contain the attempted access location
	 * 
	 *            Helper method to check if process is attempting to access a
	 *            register that is less than the base register or greater than
	 *            the limit register
	 */

	private boolean checkAccess(int register) {
		if ((register + getBASE()) < getBASE()) {
			if (m_verbose)
			{
				System.out
				.println("Attempting to access register that is less than" +
						" base register");
			}
			m_TH.interruptIllegalMemoryAccess(register);
			return false;
		} else if ((register + getBASE()) > getLIM()) {
			if(m_verbose)
			{
				System.out
				.println("Attempting to access register that is greater" + 
						" than the limit register");
			}
			m_TH.interruptIllegalMemoryAccess(register);
			return false;
		}

		return true;
	}

	/**
	 * pushToStack()
	 * 
	 * @param register
	 *            contains information to push to stack Helper method to push
	 *            stuff onto the stack
	 */
	private void pushToStackR(int register) {
		m_RAM.write(getLIM() - getSP(), m_registers[register]);
		setSP((getSP() + 1));
	}

	public void pushToStack(int content)
	{
		m_RAM.write(getLIM() - getSP(), content);
		setSP((getSP() + 1));
	}


	/**
	 * popFromStack()
	 * 
	 * @param register
	 *            to pop information to Helper method to pop stuff from the
	 *            stack
	 */
	private void popFromStack(int register) {
		if(getSP() <= 0) // ensure there is something on the stack
		{
			//m_TH.interruptIllegalInstruction( );
			return;
		}
		setSP((getSP() - 1));
		m_registers[register] = m_RAM.read(getLIM() - getSP());

	}

	/**
	 * popFromStack()
	 * 
	 * @param location
	 *            to be offset Helper method to give physical address
	 */

	public int popFromStack()
	{
		if(getSP() <= 0)
		{
			return 0;
		}
		setSP((getSP() - 1));
		int toReturn = m_RAM.read(getLIM() - getSP());
		return toReturn;
	}

	private int offset(int relLoc) {
		return relLoc + getBASE() - INSTRSIZE;
	}

	/**
	 * addTicks
	 * 
	 * adds a given number of ticks to the value of m_ticks.
	 * This will be used to simulate the cost of some OS operations.
	 * 
	 * @param saveLoadTime amount of ticks to add
	 */
	public void addTicks(int saveLoadTime) {
		m_ticks = m_ticks + saveLoadTime;
	}

	/**
	 * getTicks
	 * 
	 * returns the amount of current CPU ticks
	 * 
	 * @return m_ticks
	 */
	public int getTicks() {
		return m_ticks;

	}
	//======================================================================
	//Callback Interface
	//----------------------------------------------------------------------
	/**
	 * TrapHandler
	 *
	 * This interface should be implemented by the operating system to allow the
	 * simulated CPU to generate hardware interrupts and system calls.
	 */
	public interface TrapHandler
	{
		void interruptIllegalMemoryAccess(int addr);
		void interruptDivideByZero();
		void interruptIllegalInstruction(int[] instr);
		void interruptClock();
		void systemCall();
		public void interruptIOReadComplete(int devID, int addr, int data);
		public void interruptIOWriteComplete(int devID, int addr);
	};//interface TrapHandler
};// class CPU


