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
 */

public class CPU {

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
                                           // args. (Set to a fixed value for
                                           // simplicity.)

    // ======================================================================
    // Member variables
    // ----------------------------------------------------------------------
    /**
     * specifies whether the CPU should output details of its work
     **/
    private boolean m_verbose = true;

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

    // ======================================================================
    // Methods
    // ----------------------------------------------------------------------

    /**
     * CPU ctor
     * 
     * Intializes all member variables.
     */
    public CPU(RAM ram) {
        m_registers = new int[NUMREG];
        for (int i = 0; i < NUMREG; i++) {
            m_registers[i] = 0;
        }
        m_RAM = ram;

    }// CPU ctor

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
    private void regDump() {
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
                // place value of arg2 into arg1
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
                m_registers[instruction[1]] = m_registers[instruction[2]]
                        / m_registers[instruction[3]];
                break;

            case COPY:
                //
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
                pushToStack(instruction[1]);
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
                return;

            default:
                break;
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
            System.out
                    .println("Attempting to access register that is less than" +
                            " base register");
 
            return false;
        } else if ((register + getBASE()) > getLIM()) {
            System.out
                    .println("Attempting to access register that is greater" + 
                            " than the limit register");
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
    private void pushToStack(int register) {

        setSP((getSP() + 1));
        m_RAM.write(getLIM() - getSP(), m_registers[register]);

    }

    /**
     * popFromStack()
     * 
     * @param register
     *            to pop information to Helper method to pop stuff from the
     *            stack
     */
    private void popFromStack(int register) {
        m_registers[register] = m_RAM.read(getLIM() - getSP());
        setSP((getSP() - 1));
    }

    /**
     * popFromStack()
     * 
     * @param location
     *            to be offset Helper method to give physical address
     */
    private int offset(int relLoc) {
        return relLoc + getBASE() - INSTRSIZE;
    }

};// class CPU
