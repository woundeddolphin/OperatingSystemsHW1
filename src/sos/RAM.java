package sos;

import java.util.*;

/**
 * This class simulates a random access memory for the CPU class.
 *
 * @see CPU
 * @see SOS
 * @see Program
 * @see Sim
 *
 */
public class RAM
{
    //member veriables
    /**
     * The size of the RAM (expressed as a number of integers)
     **/
    private int m_size = 0;
    
    /**
     * This array contains the simulated RAM itself
     **/
    private int m_mem[] = null;
    
    /**
     * This describes how long it takes the simulated RAM to retrieve a given
     * value.  
     **/
    private int m_latency;
    
    /**
     * the constructor does nothing special
     *
     * @param size number of integers ("words") in ram
     * @param latency the number of nanoseconds to delay for RAM latency
     */
    public RAM(int size, int latency)
    {
        m_size = size;
        m_mem = new int[m_size];
        for(int i = 0; i < m_size; i++)
        {
            m_mem[i] = 0;
        }
        m_latency = latency;
    }//ctor

    /**
     * getSize
     *
     * @return the size of the RAM expressed as a number of integers
     *
     */
    public int getSize()
    {
        return m_size;
    }

    /**
     * getLatency
     *
     * @return the time in nanoseconds required to retrieve a value from RAM
     *
     */
    public int getLatency()
    {
        return m_latency;
    }

    /**
     * fetch
     * 
     * retrieves an entire instruction from the simulated RAM.  
     *
     * @see CPU#INSTRSIZE
     */
    public int[] fetch(int pc)
    {
        int instr[] = new int[CPU.INSTRSIZE];
        for(int i = 0; i < CPU.INSTRSIZE; i++)
        {
            instr[i] = m_mem[pc+i];
        }

        return instr;
        
    }//fetch

    /**
     * read
     *
     * loads an integer from the simulated RAM
     *
     * @param addr  the location to retrieve from
     * @return      the value at the given location
     */
    public int read(int addr)
    {
        //Simulate RAM latency
        if (m_latency > 0)
        {
            try
            {
                Thread.sleep(0, m_latency);
            }
            catch(InterruptedException ie)
            {/* do nothing*/ }
        }
        
        return m_mem[addr];
    }//read

    /**
     * write
     *
     * saves an integer to the simulated RAM
     *
     * @param addr  the addrss to write to
     * @param val   the value to write
     */
    public void write(int addr, int val)
    {
        //Simulate RAM latency 
        if (m_latency > 0)
        {
            try
            {
                Thread.sleep(0, m_latency);
            }
            catch(InterruptedException ie)
            {/* do nothing*/ }
        }
        
        m_mem[addr] = val;
    }//write

};
