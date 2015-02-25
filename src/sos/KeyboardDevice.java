package sos;

import java.util.*;

/**
 * This class simulates a simple, non-sharable read-only device.  It always
 * returns a random number to the CPU via the data bus.  
 *
 * @see Sim
 * @see CPU
 * @see Device
 */
public class KeyboardDevice implements Device, Runnable
{
    private int m_Id = -1;             // The OS assigned device ID
    private boolean m_request = false; // is the device currently processing a request?
    private int m_addr = 0;            // address to read from
    private int m_maxLatency = 10000;  // maximum latency in ns
    private int m_minLatency = 500;    // minimum latnecy in ns
    private InterruptController m_IC = null; // reference to the interrupt controller

    /**
     * Verbose mode generates helpful debugging printlns
     **/
    public static final boolean m_verbose = false;
    
    /**
     * This constructor does nothing
     */
    public KeyboardDevice(InterruptController ic)
    {
        m_IC = ic;
    }

    /**
     * This constructor expects values for the minimum and maximum latency
     * of this device expressed as a number of nanoseconds
     */
    public KeyboardDevice(InterruptController ic, int min, int max)
    {
        //If latencies are out of order swap them
        if (min > max)
        {
            int tmp = max;
            max = min;
            min = tmp;
        }
        
        m_minLatency = min;
        m_maxLatency = max;
        m_IC = ic;
    }//ctor

    /**
     * getId
     *
     * @return the device id of this device
     */
    public int getId()
    {
        return m_Id;
    }
    
    /**
     * setId
     *
     * sets the device id of this device
     *
     * @param id the new id
     */
    public void setId(int id)
    {
        m_Id = id;
    }
    
    /**
     * isSharable
     *
     * @return true
     */
    public boolean isSharable()
    {
        return false;
    }
    
    /**
     * isAvailable
     *
     * @return true
     */
    public boolean isAvailable()
    {
        return !m_request;
    }
    
    /**
     * isReadable
     *
     * @return whether this device can be read from (true/false)
     */
    public boolean isReadable()
    {
        return true;
    }
     
    
    /**
     * isWriteable
     *
     * @return whether this device can be written to (true/false)
     */
    public boolean isWriteable()
    {
        return false;
    }
     
    
    /**
     * read
     *
     * method records a request for service from the device and as such is
     * analagous to setting a value in a register on the device's controller.
     */
    public int read(int addr)
    {
        m_addr = addr;
        m_request = true;

        return -9999;           // no longer used
    }//read
    
    /**
     * write
     *
     * not implemented
     */
    public void write(int addr, int data)
    {
        //This method should never be called
    }
    
    /**
     * run
     *
     * This method represents the device + controller.  It watches for reqeusts
     * (via m_request and m_data) and handles them.  It also inserts a random
     * latency to simulate the amount of time required.
     *
     */
    public void run()
    {
        //Device runs until program ends
        while(true)
        {
            //If there is no request to process, yield the CPU to another thread
            while (!m_request)
            {
                Thread.yield();
            }

            //generate a random multiple of 1000
            int rn = (int)(Math.random() * 999999) * 1000;

            //Sleep to simulate the latency
            try
            {
                int latency = (rn % (m_maxLatency - m_minLatency)) + m_minLatency;
                Thread.sleep(latency / 1000, latency % 1000);
            }
            catch(InterruptedException e) {} // should never happen
            
            //Notify the interrupt controller of the available data
            if (m_verbose)
            {
                System.out.println("Keyboard puts '" + rn + "' on the data bus.");
            }
            m_IC.putData(InterruptController.INT_READ_DONE, m_Id, m_addr, rn);

            //Make the device available for another request
            m_request = false;
        }//while
    }//run

}//class KeyboardDevice
