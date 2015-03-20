package sos;

import java.util.*;

/**
 * This class represents the CPU's interrupt controller.  More abstractly is the
 * container object between a producer (device(s)) and consumer (CPU).
 *
 * Adapted from an example in the Concurrent Programming Tutorial written by Sun
 * Microsystems Inc.
 *
 * @see Device
 * @see CPU
 */
public class InterruptController
{
    //======================================================================
    // Constants
    //----------------------------------------------------------------------

    //Each interrupt that this controller handles has a unique ID
    public static final int INT_READ_DONE   = 100;
    public static final int INT_WRITE_DONE  = 101;
    
    //======================================================================
    // Variables
    //----------------------------------------------------------------------
    private int m_operation = 0;    // What interrupt is to be generated?
    private int m_devNum = 0;       // the id of the device that has data
    private int m_addr = 0;         // the address where data was read/wrote
    private int m_data = 0;         // the data 
    private boolean m_empty = true; // whether or not there is an interrupt
    
    //======================================================================
    // Methods
    //----------------------------------------------------------------------
    
    /**
     * the constructor does nothing
     *
     */
    public InterruptController()
    {
    }

    /**
     * Is there data available to take?
     *
     */
    public boolean isEmpty()
    {
        return m_empty;
    }

    /**
     * getData
     *
     * is used by the CPU to retrieve the data associated with the interrupt.
     */
    public synchronized int[] getData()
    {
        while(m_empty)
        {
            try
            {
                wait();
            }
            catch(InterruptedException e) {} // should never happen
        }

        //Fill an array with the device number and data
        int[] retVal = new int[4];
        retVal[0] = m_operation;
        retVal[1] = m_devNum;
        retVal[2] = m_addr;
        retVal[3] = m_data;

        //Make the interrupt controller available for other devices
        m_empty = true;
        notifyAll();

        return retVal;
        
    }//getData

    /**
     * putData
     *
     * is used by a device to signal that it has data for the CPU.
     */
    public synchronized void putData(int operation, int devNum, int addr, int data)
    {
        while(!m_empty)
        {
            try
            {
                wait();
            }
            catch(InterruptedException e) {} // should never happen
        }
        
        //Make the data on the bus available to the CPU
        m_empty = false;
        m_devNum = devNum;
        m_operation = operation;
        m_addr = addr;
        m_data = data;
        notifyAll();
            
    }//putData
    
};//class InterruptController
