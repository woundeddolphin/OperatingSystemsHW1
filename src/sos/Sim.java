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
     * runSimple
     *
     * runs a single counting program that prints to the console
     *
     *
     */
    public static void runSimple()
    {
        //Create the simulated hardware and OS
        RAM ram = new RAM(1000, 10);
        InterruptController ic = new InterruptController();
        ConsoleDevice cd = new ConsoleDevice(ic);
        cd.setId(1);
        CPU cpu = new CPU(ram, ic);
        SOS os  = new SOS(cpu, ram);

        //Register the device drivers with the OS
        os.registerDevice(cd, 1);

        //Load the program into RAM
        Program prog = new Program();
        if (prog.load("print40.asm", false) != 0)
        {
            //Error loading program so exit
            return;
        }
        os.createProcess(prog,  200);

        //Start up the devices
        Thread t = new Thread(cd);
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

        
    }//runSimple


    /**
     * runMultiple1
     *
     * runs one process that spawns five others.  Each spawned process should
     * run to completion without being interrupted.
     *
     *
     */
    public static void runMultiple1()
    {
        //Create the simulated hardware and OS
        RAM ram = new RAM(5000, 10);
        InterruptController ic = new InterruptController();
        ConsoleDevice cd = new ConsoleDevice(ic);
        cd.setId(1);
        CPU cpu = new CPU(ram, ic);
        SOS os  = new SOS(cpu, ram);

        //Register the device drivers with the OS
        os.registerDevice(cd, 1);

        //Load the program into RAM
        Program prog = new Program();
        if (prog.load("spawn5.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load spawn5.asm");
            return;
        }
        os.createProcess(prog,  200);

        //Register count40.asm as a program that can be run via an Exec system call
        Program prog2 = new Program();
        if (prog2.load("print40.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load print40.asm");
            return;
        }
        os.addProgram(prog2);

        //Start up the devices
        Thread t = new Thread(cd);
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

    }//runMultiple1

    /**
     * runMultiple2
     *
     * runs one process that spawns five others.  Each spawned process should
     * yield the CPU on a regular basis which will result in multiple
     * context switches.
     *
     */
    public static void runMultiple2()
    {
        //Create the simulated hardware and OS
        RAM ram = new RAM(5000, 10);
        InterruptController ic = new InterruptController();
        ConsoleDevice cd = new ConsoleDevice(ic, 50000, 100000);
        cd.setId(1);
        CPU cpu = new CPU(ram, ic);
        SOS os  = new SOS(cpu, ram);

        //Register the device drivers with the OS
        os.registerDevice(cd, 1);

        //Load the program into RAM
        Program prog = new Program();
        if (prog.load("spawn5.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load spawn5.asm");
            return;
        }
        os.createProcess(prog,  200);

        //Register count40.asm as a program that can be run via an Exec system call
        Program prog2 = new Program();
        if (prog2.load("print40yield.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load print40yield.asm");
            return;
        }
        os.addProgram(prog2);

        //Start up the devices
        Thread t = new Thread(cd);
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
        
    }//runMultiple2

    /**
     * runMultiple3
     *
     * runs one process that spawns five others.  Each spawned process will read
     * from the keyboard and write to the console.  The processes will also
     * yield the CPU on a regular basis
     *
     */
    public static void runMultiple3()
    {
        //Create the simulated hardware and OS
        RAM ram = new RAM(5000, 10);
        InterruptController ic = new InterruptController();
        KeyboardDevice kd = new KeyboardDevice(ic);
        ConsoleDevice cd = new ConsoleDevice(ic);
        kd.setId(0);
        cd.setId(1);
        CPU cpu = new CPU(ram, ic);
        SOS os  = new SOS(cpu, ram);

        //Register the device drivers with the OS
        os.registerDevice(kd, 0);
        os.registerDevice(cd, 1);

        //Load the program into RAM
        Program prog = new Program();
        if (prog.load("spawn5.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load spawn5.asm");
            return;
        }
        os.createProcess(prog,  200);

        //Register count40.asm as a program that can be run via an Exec system call
        Program prog2 = new Program();
        if (prog2.load("readwriteyield.asm", false) != 0)
        {
            System.out.println("ERROR: Could not load readwriteyield.asm");
            return;
        }
        os.addProgram(prog2);

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
        
    }//runMultiple3

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
            runSimple();

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
