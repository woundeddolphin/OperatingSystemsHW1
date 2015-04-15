package sos;

import java.util.*;

/**
 * This class simulates a memory management unit.  Typically this is on
 * the same chip as the CPU but it is logically distinct.
 *
 * The operating system is responsible for initializing and
 * maintaining a page table for this MMU.  The MMU assumes that the
 * page table is stored in the bottom of RAM so that page numbers
 * correspond directly to the physical address of the page table entry
 * correponding to that page.  Each entry should contain a frame
 * number (if valid) plus a state (see the constants defined below).
 *
 * @see CPU
 * @see SOS
 * @see RAM
 *
 */
public class MMU
{
    //======================================================================
    // Constants
    //----------------------------------------------------------------------
    public static final int DEFAULT_PAGE_SIZE = 256; //8 bits
    public static final int MINIMUM_PAGE_SIZE = 64;  //6 bits

    //======================================================================
    // Member Variables
    //----------------------------------------------------------------------
    
    /**
     * specifies whether the MMU should output details of its work
     **/
    private boolean m_verbose = false;

    /**
     * a reference to the system RAM
     */
    private RAM m_RAM = null;
    
    /**
     * the size of the the virtual memory
     **/
    private int m_size = 0;

    /**
     * the page size
     */
    private int m_pageSize = 0;

    /**
     * the number of page frames
     */
    private int m_numFrames = 0;

    /**
     * the number of pages
     */
    private int m_numPages = 0;

    /**
     * the number of bits in the offset
     */
    private int m_offsetSize = -1;
    
    /**
     * This int will contain a 1 in every bit that corresponds to the
     * offset of the virtual address and a 0 everywhere else.
     */
    private int m_offsetMask = -1;
    
    /**
     * This int contains a 1 in every bit that corresponds to the status of a
     * page table entry.
     */
    private int m_statusMask = 7;  
    
    /**
     * This int will contain a 1 in every bit that corresponds to the
     * page number of the virtual address and a 0 everywhere else.
     */
    private int m_pageMask = -1;
    
    /**
     * a reference to the trap handler for page faults.
     */
    private CPU.TrapHandler m_TH = null;

    //======================================================================
    // Constructors
    //----------------------------------------------------------------------
    /**
     * the constructor initializes instance variables and initializes
     * a page table
     *
     * CAVEAT: The OS is responsible for initializing and maintaining
     * the page table in RAM (@see MMU)
     *
     * @param ram      a reference to the physical RAM
     * @param size     the number of integers ("words") in virtual ram.
     *                 If this is not a power of 2, the value will be
     *                 adjusted to the next power of 2.  If this number is less
     *                 than the size of RAM it will be adjusted to the size of
     *                 RAM. 
     * @param pageSize the number of integers ("words") in single page.  If this
     *                 number is not reasonable or not a power of 2 then it will
     *                 adjusted
     */
    public MMU(RAM ram, int size, int pageSize)
    {
        m_RAM = ram;

        //Make sure the virtual memory is at least as big as actual RAM
        if (size < ram.getSize())
        {
            size = ram.getSize();
        }
        
        //Check for an out-of-range page size parameter
        if ((pageSize < MINIMUM_PAGE_SIZE) || (pageSize > m_RAM.getSize() / 4))
        {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        //Adjust page size to a power of 2 while simultaneously
        //calculating the number offset bits
        m_offsetSize = -1;
        while(pageSize > 0)
        {
            pageSize = pageSize >> 1;
            m_offsetSize++;
        }
        m_pageSize = 1 << m_offsetSize;

        //Calculate the offset mask and page mask
        m_offsetMask = m_pageSize - 1;
        m_pageMask = ~m_offsetMask;

        //Calculate the number of frames.  If RAM's size is not a power of 2 the
        //remainder will be wasted
        m_numFrames = m_RAM.getSize() / m_pageSize;

        //Calculate the number of pages.  If the virtual memory's size
        //is not a power of 2 then it is adjusted due to the integer
        //division. 
        m_numPages = size / m_pageSize;
        m_size = m_numPages * m_pageSize;

    }//ctor

    /**
     * This constructor uses default values for pageSize and a virtual
     * memory size of quadruple the RAM's size.
     *
     * @param ram      a reference to the physical RAM
     */
    public MMU(RAM ram)
    {
        this(ram, ram.getSize() * 4, DEFAULT_PAGE_SIZE);
    }
    
    //======================================================================
    // Accessor Methods
    //----------------------------------------------------------------------
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
     * getPageSize
     *
     */
    public int getPageSize()
    {
        return m_pageSize;
    }

    /**
     * getNumFrames
     *
     */
    public int getNumFrames()
    {
        return m_numFrames;
    }

    /**
     * getNumPages
     */
    public int getNumPages()
    {
        return m_numPages;
    }

    /**
     * getOffsetSize
     */
    public int getOffsetSize()
    {
        return m_offsetSize;
    }
    
    /**
     * getStatusMask
     *
     */
    public int getStatusMask()
    {
        return m_statusMask;
    }
    /**
     * getOffsetMask
     *
     */
    public int getOffsetMask()
    {
        return m_offsetMask;
    }

/**
     * getPageMask
     *
     */
    public int getPageMask()
    {
        return m_pageMask;
    }

    //======================================================================
    // Methods
    //----------------------------------------------------------------------

    /**
     * registerTrapHandler
     *
     * allows SOS to register itself as the trap handler 
     */
    public void registerTrapHandler(CPU.TrapHandler th)
    {
        m_TH = th;
    }

    /**
     * getStatus
     *
     * gets the status of an entry in the table specified via a given virtual
     * address
     *
     * @param virtAddr    the virtual address whose page's status is to be
     *                    changed
     * @return            the status value
     *
     */
    public int getStatus(int virtAddr)
    {
        //Retrieve the entry in the page table corresponding to the
        //given virtual address
        int page   = (virtAddr & m_pageMask) >> m_offsetSize;
        int entry = m_RAM.read(page);

        //Return the status
        return entry & m_statusMask;
        
    }//setStatus
    
    /**
     * setStatus
     *
     * sets the status of an entry in the table specified via a given virtual
     * address
     *
     * @param virtAddr    the virtual address whose page's status is to be
     *                    changed
     * @param newStatus   the new status value
     *
     */
    public void setStatus(int virtAddr, int newStatus)
    {
        //Retrieve the entry in the page table corresponding to the
        //given virtual address
        int page   = (virtAddr & m_pageMask) >> m_offsetSize;
        int oldEntry = m_RAM.read(page);

        //Construct the new value for this page table entry and write it to RAM
        int newEntry = (oldEntry & m_pageMask) + newStatus;
        m_RAM.write(page, newEntry);
    }//setStatus


    //<method header needed>
    private int translate(int virtAddr)
    {
        //%%%You will implement this method
        return -1;
    }//translate

    /**
     * write
     *
     * translates a given virtual address to a physical one and then writes data
     * to that address.  
     *
     * @param virtAddr   the virtual address to write the data to
     * @param data       the data to write
     */
    public void write(int virtAddr, int data)
    {
        //Perform the write
        int physAddr = translate(virtAddr);
        m_RAM.write(physAddr, data);
    }//write

    /**
     * read
     *
     * translates a given virtual address to a physical one and then reads the
     * data at that address.
     *
     * @param virtAddr   the virtual address to read data from
     * @return           the value at that location
     */
    public int read(int virtAddr)
    {
        int physAddr = translate(virtAddr);
        return m_RAM.read(physAddr);
    }//read

    /**
     * fetch
     * 
     * retrieves an entire program instruction from the simulated RAM.  
     *
     * @param pc    the virtual address to load the instruction from (program
     *              counter)
     *
     * @return      an array of int containing the instruction.  The first entry
     *              in the array is the opcode followed by the arguments in order.
     *
     * @see CPU#INSTRSIZE
     */
    public int[] fetch(int pc)
    {
        int physPC  = translate(pc);
        int instr[] = new int[CPU.INSTRSIZE];
        for(int i = 0; i < CPU.INSTRSIZE; i++)
        {
            instr[i] = m_RAM.read(physPC + i);
        }

        return instr;
        
    }//fetch

     

}//class MMU
