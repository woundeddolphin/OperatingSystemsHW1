package sos;

import java.util.*;

/**
 * This interface defines the necessary methods for creating a simulated device
 * driver for the SOS simulation.  
 * device.
 *
 * @see Sim
 * @see CPU
 * @see SOS
 */
public interface Device
{
    /**
     * getId
     *
     * @return the device id of this device
     */
    public int getId();
    
    /**
     * setId
     *
     * sets the device id of this device
     *
     * @param id the new id
     */
    public void setId(int id);
    
    /**
     * isSharable
     *
     * @return true if multiple processes can use the device at once (e.g., a
     *         disk drive) or false otherwise (e.g., a printer)
     */
    public boolean isSharable();
    
    /**
     * isAvailable
     *
     * returns true if the device is available for use
     */
    public boolean isAvailable();
    
    /**
     * isReadable
     *
     * @return whether this device can be read from (true/false)
     */
    public boolean isReadable();
     
    
    /**
     * isWriteable
     *
     * @return whether this device can be written to (true/false)
     */
    public boolean isWriteable();
     
    
    /**
     * read
     *
     * method records a request for service from the device and as such is
     * analagous to setting a value in a register on the device's controller.
     */
    public int read(int addr);
    
    /**
     * write
     *
     * method records a request for service from the device and as such is
     * analagous to setting a value in a register on the device's controller.
     * As a result, the function does not check to make sure that the
     * device is ready for this request (that's the OS's job).
     */
    public void write(int addr, int data);
    
};//interface Device
