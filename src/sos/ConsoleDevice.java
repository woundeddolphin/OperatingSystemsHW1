package sos;

import java.util.*;

/**
 * This class simulates a simple, sharable write-only device.  
 *
 * @see Sim
 * @see CPU
 * @see SOS
 * @see Device
 */
public class ConsoleDevice implements Device
{
    private int m_id = -999;           // the OS assigned device ID

    /**
     * getId
     *
     * @return the device id of this device
     */
    public int getId()
    {
        return m_id;
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
        m_id = id;
    }
    
    /**
     * isSharable
     *
     * This device can be used simultaneously by multiple processes
     *
     * @return true
     */
    public boolean isSharable()
    {
        return true;
    }
    
    /**
     * isAvailable
     *
     * this device is available if no requests are currently being processed
     */
    public boolean isAvailable()
    {
        return true;
    }
    
    /**
     * isReadable
     *
     * @return whether this device can be read from (true/false)
     */
    public boolean isReadable()
    {
        return false;
    }
     
    
    /**
     * isWriteable
     *
     * @return whether this device can be written to (true/false)
     */
    public boolean isWriteable()
    {
        return true;
    }
     
    /**
     * read
     *
     * not implemented
     * 
     */
    public int read(int addr /*not used*/)
    {
        //This method should never be called
        return -1;
    }//read
    
    
    /**
     * write
     *
     * method records a request for service from the device and as such is
     * analagous to setting a value in a register on the device's controller.
     * As a result, the function does not check to make sure that the
     * device is ready for this request (that's the OS's job).
     */
    public void write(int addr /*not used*/, int data)
    {
        System.out.println("CONSOLE: " + data);
    }
    
};//class ConsoleDevice
