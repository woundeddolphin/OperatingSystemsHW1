package sos;

import java.util.*;

/**
 * This class sets up the SOS simulation by creating the RAM, CPU and SOS
 * objects, loading appropriate programs, and calling {@link CPU#run} method on
 * the CPU.
 *
 * @see RAM
 * @see CPU
 * @see SOS
 * @see Program
 */
public class Sim
{
    /**  
     * the constructor does nothing
     *
     */
    public Sim()
    {
    }

    /**
     * main
     *
     * This function makes the simulation go.
     *
     */
    public static void main(String[] args)
    {
        RAM ram = new RAM(1000, 10);
        CPU cpu = new CPU(ram);
        SOS os = new SOS(cpu, ram);

        Program prog = new Program();
        if (prog.load("crazycount.asm", false) != 0)
        {
            //Error loading program so exit
            return;
        }

        os.createProcess(prog, 300);

        cpu.run();
        
        System.out.println("END OF SIMULATION");
        
    }//main
    
};//class Sim
