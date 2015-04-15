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
    /*======================================================================-
     * Inner Classes
     *----------------------------------------------------------------------
     */
    
    /**
     * ExitCatcher
     *
     * is a security manager that prevents threads from calling System.exit().
     * This allows Sim.java to properly time the simulation.
     *
     */
    static class ExitCatcher extends SecurityManager
    {
        private boolean m_caught = false;

        public ExitCatcher()
        {
            super();
        }

        public boolean isExitCaught()
        {
            return m_caught;
        }

        public void checkExit(int status)
        {
            super.checkExit(status);
            if (!m_caught)
            {
                m_caught = true;
                throw new SecurityException();
            }
        }
        
        public void checkRead(String file) 
        {
        	//do nothing
        }
    }//ExitCatcher

    /**
     * DoNothingHandler
     *
     * needed to "handle" uncaught exceptions thrown by the device and CPU
     * threads (simulation will just end)
     */
    static class DoNothingHandler implements Thread.UncaughtExceptionHandler
    {
        public void uncaughtException(Thread t, Throwable th)
        {
            if (th instanceof SecurityException)
            {
                //do nothing (what, you thought I was kidding?)
            }
            else
            {
                //Report other exceptions to the user
                System.out.println("Exception in Current Thread:");
                th.printStackTrace();
            }
        }
    }//DoNothingHandler

    /*======================================================================-
     * Member Variables
     *----------------------------------------------------------------------
     */
    private static ExitCatcher m_EC = new ExitCatcher();
    private static DoNothingHandler m_DNH = new DoNothingHandler();
    
    /*======================================================================-
     * Methods
     *----------------------------------------------------------------------
     */
    

    /**
     * runAllocTest
     *
     * runs lots of programs of different sizes to create memory fragmentation
     *
     */
    public static void runAllocTest()
    {
        //Create the simulated hardware and OS
        RAM ram = new RAM(4096, 0);
        MMU mmu = new MMU(ram, 4096, 64);
        InterruptController ic = new InterruptController();
        KeyboardDevice kd = new KeyboardDevice(ic);
        kd.setId(0);
        ConsoleDevice cd = new ConsoleDevice(ic);
        cd.setId(1);
        CPU cpu = new CPU(ram, ic, mmu);
        SOS os  = new SOS(cpu, ram, mmu);

        //Register the device drivers with the OS
        os.registerDevice(kd, 0);
        os.registerDevice(cd, 1);

        //Load the program into RAM
        Program prog = new Program();
        if (prog.load("quickspawn20.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load quickspawn20.asm");
            return;
        }
        os.createProcess(prog,  1200);

        //Register other programs for Exec system calls.  These processes have
        //been designed to encourage memory fragmentation
        prog = new Program();
        if (prog.load("quickspawn1a.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load quickspawn1a.asm");
            return;
        }
        os.addProgram(prog);
        prog = new Program();
        if (prog.load("quickspawn1b.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load quickspawn1b.asm");
            return;
        }
        os.addProgram(prog);
        prog = new Program();
        if (prog.load("quickspawn1c.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load quickspawn1c.asm");
            return;
        }
        os.addProgram(prog);
        prog = new Program();
        if (prog.load("quickspawn1d.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load quickspawn1d.asm");
            return;
        }
        os.addProgram(prog);
        prog = new Program();
        if (prog.load("quickspawn1e.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load quickspawn1e.asm");
            return;
        }
        os.addProgram(prog);
        prog = new Program();
        if (prog.load("thinker.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load thinker.asm");
            return;
        }
        os.addProgram(prog);
        prog = new Program();
        if (prog.load("bigthinker.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load bigthinker.asm");
            return;
        }
        os.addProgram(prog);

        //Start up the devices
        Thread t = new Thread(cd);
        t.setUncaughtExceptionHandler(m_DNH);
        t.start();
        t = new Thread(kd);
        t.setUncaughtExceptionHandler(m_DNH);
        t.start();
        
        //Run the simulation
        t = new Thread(cpu);
        t.setUncaughtExceptionHandler(m_DNH);
        t.start();

        //Wait until System.exit() is called
        while(!m_EC.isExitCaught())
        {
            try
            {
                t.join(1000);
            }
            catch(InterruptedException ie)
            {
                System.out.println("Interrupted!");
                return;
            }
        }//while

    }//runAllocTest

    /**
     * main
     *
     * This function makes the simulation go.
     *
     */
    public static void main(String[] args)
    {
        //Start catching System.exit
        System.setSecurityManager(m_EC);

        //Delay for any threads that might be winding down
        //Do a timed run
        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis();
        try
        {
            //***********Run the simulation************
            runAllocTest();

            //Record the ending time
            endTime = System.currentTimeMillis();

            //Delay for any other threads that might be winding down
            Thread.sleep(1000);
        }
        catch(SecurityException se)
        {
            endTime = System.currentTimeMillis();
            se.printStackTrace();
        }
        catch(Exception e)
        {
            endTime = System.currentTimeMillis();
            System.out.println("EXCEPTION THROWN DURING SIMULATION:");
            e.printStackTrace();
        }

        //If System.exit was not called by any thread then bypass that
        //protection now
        if (! m_EC.isExitCaught())
        {
            try{ System.exit(-42); } catch (SecurityException se) { }
        }

        //Print the final timing info for the user
        System.out.println("");
        System.out.println("");
        System.out.println("END OF SIMULATION");
        System.out.println("Total Simulation Time: " + (endTime - startTime) + "ms");

        System.exit(0);
        
    }//main
    
};//class Sim
