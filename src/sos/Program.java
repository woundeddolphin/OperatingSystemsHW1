package src.sos;

import java.util.*;
import java.io.*;

/**
 * This class stores a program in the pidgin assembly language used by the
 * simulated CPU.  It's primary function is to parse a flat text file
 * containing the code into an array of integers that can be used by
 * the CPU.
 *
 * @see CPU
 * @see SOS
 *
 */
public class Program
{
    /**
     * This class contains the name and location of a label found in the pidgin
     * assembly code.
     */
    private class Label
    {
        int addr;               // the address of the label
        String name;            // the name of the label
    };
    
    /**
     * a Vector of Integer used to store the program as it is parsed
     **/
    private Vector<Integer> m_prog = null;

    /**
     * an Vector of Label used to store all the labels found in the code
     **/
    private Vector<Label> m_labels = null;

    /**
     * an Vector of Label used to store all the forward references to
     * as-yet-unparsed labels in the code.  These are resolved after the entire
     * program has been parsed.
     **/
    private Vector<Label> m_orphans = null;

    /**
     * identifies which line of a file is currently being parsed (handy for
     * syntax error messages).
     **/
    private int m_lineNum = 0;

    /**
     * identifies which line of a file is currently being parsed (handy for
     * syntax error messages).
     **/
    private int m_defaultAllocSize = 0;

    /**
     * specifies whether the parser should output details of its work
     **/
    private boolean m_verbose = false;

    /**
     * when this program is being used by the simulation, this variable tracks
     * how many times it has been used to create a process
     */
    public int callCount = 0;
     

    /**
     * contructor does nothing special
     * 
     */
    public Program()
    {
        m_prog = new Vector<Integer>();
        m_labels = new Vector<Label>();
        m_orphans = new Vector<Label>();
    }

    /**
     * setDefaultAllocSize
     *
     * sets the default alloc size for this program
     */
    public void setDefaultAllocSize(int das)
    {
        m_defaultAllocSize = das;
    }//setDefaultAllocSize
    
    /**
     * getSize
     *
     * returns the number of integers that make up the program
     * 
     */
    public int getSize()
    {
        return m_prog.size();
    }

    /**
     * getDefaultAllocSize
     *
     * gets the default alloc size for this program
     */
    public int getDefaultAllocSize()
    {
        return m_defaultAllocSize;
    }//getDefaultAllocSize
    
    /**
     * skipWhite
     *
     * given a current position in a string, this funciton determines where the
     * next non-whitespace character is
     *
     * @param line  the string to find the non-whitepsace character in
     * @param i     where to begin searching
     * @return      the location of the non-whitespace character
     * 
     */
    private int skipWhite(String line, int i)
    {
        if (line.length() <= i) return i;
        while((line.charAt(i) == ' ') || (line.charAt(i) == '\t'))
        {
            i++;
            if (line.length() <= i) return i;
        }

        return i;
    }//skipWhite

    /**
     * skipToken
     *
     * given a current position in a string, this funciton determines where the
     * next token begins (skipping the current token if any)
     *
     * @param line  the string to find the next token in
     * @param i     where to begin searching
     * @return      the location of the next token
     * 
     */
    private int skipToken(String line, int i)
    {
        if (line.length() <= i) return i;
        while((line.charAt(i) != ' ') && (line.charAt(i) != '\t'))
        {
            i++;
            if (line.length() <= i) return i;
        }

        return skipWhite(line, i);
    }//skipToken

    /**
     * getToken
     *
     * returns the substring containing the next contiguous set of
     * non-whitespace characters in a given string
     *
     * @param line  the string to extract the substring from
     * @param i     where to begin extracting
     * @return      the extracted substring
     * 
     */
    private String getToken(String line, int i)
    {
        String result = "";     // the return value of this function
        if (line.length() <= i) return result;
        while((line.charAt(i) != ' ') && (line.charAt(i) != '\t'))
        {
            result += line.charAt(i);
            i++;
            if (line.length() <= i) return result;
        }

        return result;
    }//getToken

    /**
     * parseLabel
     *
     * parses a single label defintion found in the code and places it in the
     * m_labels list
     * 
     * @param line the entire line of text containing the instruction
     * @param i    the position in the line where the instruction begins.  This
     *             <b>must</b> be the location of the starting colon (':')
     *             character 
     * @return     a success/error code (0 is success; anything else is failure)
     * @see        #parseLine
     * @return     a success/error code (0 is success; anything else is
     *             failure)
     */
    private int parseLabel(String line, int i)
    {
        Label l = new Label();  // the parsed label is placed in here
        
        i++;                    // skip the ':'
        l.name = getToken(line, i);
        l.addr = m_prog.size();
        m_labels.add(l);

        if (m_verbose) System.out.print("parsed label '" + l.name + "' at address " + l.addr);

        return 0;
    }//parseLabel

    /**
     * instrToInt
     *
     * parses a single instruction code to its integer equivalent using the
     * constants defined in the CPU class
     *
     * @param instr the instruction code to parse
     * @return      the parsed instruction <b>or</b> a negative value indicating
     *              an error occurred during the parse
     * @see         CPU
     */
    private int instrToInt(String instr)
    {
        //An instruction must have at least two characters
        if (instr.length() < 2)
        {
            return -107;
        }

        //Opcode parsing
        switch(instr.charAt(0))
        {
            case 'A':
                return CPU.ADD;
            case 'B':
                if (instr.charAt(1) == 'L')
                {
                    return CPU.BLT;
                }
                else if (instr.charAt(1) == 'N')
                {
                    return CPU.BNE;
                }
                else if (instr.charAt(1) == 'R')
                {
                    return CPU.BRANCH;
                }
                else
                {
                    return -106;
                }
            case 'C':
                return CPU.COPY;
            case 'D':
                return CPU.DIV;
            case 'L':
                return CPU.LOAD;
            case 'M':
                return CPU.MUL;
            case 'P':
                if (instr.charAt(1) == 'O')
                {
                    return CPU.POP;
                }
                else if (instr.charAt(1) == 'U')
                {
                    return CPU.PUSH;
                }
                else
                {
                    return -103;
                }
            case 'S':
                switch(instr.charAt(1))
                {
                    case 'A':
                        return CPU.SAVE;
                    case 'E':
                        return CPU.SET;
                    case 'U':
                        return CPU.SUB;
                   default:
                        return -102;
                }//switch
            case 'T':
                return CPU.TRAP;
            default:
                return -101;
        }
    }//instrToInt

    /**
     * parseArg
     *
     * parses a single instruction argument
     *
     * @param line the entire line of text containing the instruction
     * @param i    the position in the line where the instruction begins
     * @return     a success/error code (0 is success; anything else is failure)
     * @see        #parseInstruction
     * 
     */
    private int parseArg(String line, int i)
    {
        String arg = getToken(line, i); // get the argument substring

        // skip register indicator if present
        if ( (arg.length() > 1) && (arg.charAt(0) == 'R')
             && (arg.charAt(1) >= '0') && (arg.charAt(1) <= '9') )
        {
            arg = arg.substring(1);
        }

        Integer intArg = null;  // this will contain the return value

        //Check for an empty argument 
        if (arg.length() == 0)
        {
            //No more args so insert a flag number
            intArg = new Integer(99999);
        }

        //Check for a non-negative numeric argument
        else if ( (arg.charAt(0) >= '0') && (arg.charAt(0) <= '9') )
        {
            intArg = intArg.parseInt(arg);
        }

        //Check for a negative numeric argument
        else if ( (arg.charAt(0) == '-')
                  && (arg.length() > 1)
                  && (arg.charAt(1) >= '0') && (arg.charAt(1) <= '9') )
        {
            intArg = intArg.parseInt(arg);
        }

        //Assume that this argument is a label reference (e.g., the "foobar"
        //part of "BRANCH foobar")
        else
        {
            //Search the m_labels list to see if this label reference refers to
            //a label that's already been parsed.  If so, insert the
            //corresponding offset into the code
            boolean bFound = false; // set to true if found
            for(Label l : m_labels)
            {
                if (arg.equals(l.name))
                {
                    intArg = new Integer(l.addr);
                    bFound = true;
                }
            }//for

            //If the label does not already exist, record an orphan label
            //reference storing it's location offset in the addr field
            if (!bFound)
            {
                //This is probably a forward reference to an as-yet-unparsed label
                Label o = new Label();
                o.name = arg;
                o.addr = m_prog.size();
                m_orphans.add(o);
                if (m_verbose)
                {
                    System.out.println("  label '" + o.name + "' will be resolved post-parse.");
                }
                
                intArg = new Integer(42424); //put in a flag for now
            }
        }//else

        m_prog.add(intArg);
        if ( (m_verbose) && (arg.length() > 0) )
        {
            System.out.print("\t" + arg + "=" + intArg.intValue());
        }

        return 0;
    }//parseArg
    
    /**
     * parseInstruction
     *
     * parses a line of text that contains a instruction and its arguments.
     *
     * @param line the entire line of text containing the instruction
     * @param i    the position in the line where the instruction begins
     * @return     a success/error code (0 is success; anything else is failure)
     * @see        #instrToInt
     * @see        #parseArg
     * @see        #parseLine
     */
    private int parseInstruction(String line, int i)
    {
        //retrieve the instruction code from the string
        String instr = getToken(line, i);

        //Check for empty token (syntax error)
        if (instr.length() == 0)
        {
            System.out.println("\nERROR (line " + m_lineNum + "): Empty token");
            return -1;
        }

        //Convert the code to its integer form
        Integer intInstr = instrToInt(instr);
        if (intInstr.intValue() < 0)
        {
            // error during instr parse
            System.out.println("\nERROR:  Unknown opcode (" + instr  + ") on line " + m_lineNum);
            return intInstr.intValue(); 
        }

        
        if (m_verbose) System.out.print("" + instr + "=" + intInstr);

        //Add the code to the program
        m_prog.add(intInstr);

        //Read the arguments of the instruction.  Fill in zero values so that
        //all instructions are exactly CPU.INSTRSIZE ints
        for(int j = 0; j < CPU.INSTRSIZE - 1; j++)
        {
            i = skipToken(line, i);
            int err = parseArg(line, i);
            if (err != 0) return err;
        }//for

        return 0;
    }//parseInstruction
    
    /**
     * parseLine
     *
     * parses a single line of text from the file.  If the line contains an
     * instruction or a label then it is passed to the appropriate parse
     * routine.
     *
     * @param line the line of text to parse
     * @return a success/error code (0 is success; anything else is failure)
     * @see #parseInstruction
     * @see #parseLabel
     * @see #load
     */
    private int parseLine(String line)
    {
        //preprocessing: remove extra whitespace, comments, lowercase
        int commentPos = line.indexOf('#');
        if (commentPos >= 0)
        {
            line = line.substring(0, commentPos);
        }
        line = line.trim().toUpperCase();


        int i = 0;              // Current position in the string

        //If the line contains no code just skip it
        if (line.length() == 0)
        {
            return 0; // empty string
        }


        //Verbose output for the user if requested
        if (m_verbose)
        {
            System.out.print(line);
            for(int j = 0; j < (25 - line.length()); j++)
            {
                System.out.print(" ");
            }
        }

        //Check for a label and parse it if found
        if (line.charAt(i) == ':')
        {
            return parseLabel(line, i);
        }

        //Otherwise it must be an instruction
        return parseInstruction(line, i);

    }//parse

    /**
     * fixOrphans
     *
     * is called once the entire program has been parsed.  It resolves all
     * forward references to labels.
     * 
     * @return a success/error code (0 is success; anything else is failure)
     */
    private int fixOrphans()
    {
        //For each orphanned label reference...
        for(Label o : m_orphans)
        {
            boolean bFound = false; // was this orphan's label found?
            if (m_verbose)
            {
                System.out.print("Searching for orphan label: " + o.name + " among: ");
            }

            //...find the corresponding label
            for(Label l : m_labels)
            {
                if (m_verbose)
                {
                    System.out.print(l.name + " ");
                }
                
                if (o.name.equals(l.name))
                {
                    Integer intAddr = new Integer(l.addr);
                    m_prog.set(o.addr, intAddr);
                    bFound = true;
                }

            }//for

            if (m_verbose)
            {
                System.out.println();
            }

            //If the label wasn't found then report an error
            if (!bFound)
            {
                System.out.println("\nERROR: label " + o.name + " was referenced but never defined.");
                return -1;
            }
        }//for

        return 0;
    }//fixOrphans

    /**
     * load
     *
     * opens a given file and sends the pidgin assembly program found within to
     * the parse routines.
     *
     * @param fileName the filename of the file containing the code
     * @param verbose  if set 'true' this will print detailed output as it
     *                 parses
     * @return         0 is success; anthing else is a failure code
     * @see #parseLine
     * 
     */
    public int load(String fileName, boolean verbose)
    {
        int retVal = 0;         // return value (success is default)
        m_verbose = verbose;    // init verbose mode
        
        //Step 1:  Open the file
        BufferedReader file=null;    // contains the pidgin asm
        try
        {
        	File f = new File(fileName);
        	if (!f.exists())
        	{
        		System.out.println("ERROR:  File " + fileName + " was not found.");
        		String currDir = System.getProperty("user.dir");
        		System.out.println("        (If you specified a relative path the current working directory is: " + currDir);
        		return -6;
        	}
        	
        	file = new BufferedReader(new FileReader(fileName));
        }
        catch(java.security.AccessControlException ace)
        {
        	String s = "" + ace.getPermission();
        	System.out.println(s);
        }
        catch(IOException e)
        {
            String errMessage = "\nError opening file: " + fileName + "\n";
            errMessage += e.toString();
            System.out.println(errMessage);
            return -1;
        }

        //Step 2:  Parse the file into m_prog
        try
        {
            String line;        // one line of the file
            while((line = file.readLine()) != null)
            {
                m_lineNum++;
                if (m_verbose) System.out.print("\n" + m_lineNum + ": ");

                retVal = parseLine(line);
                if (retVal < 0) break;
            }
        }
        catch(IOException e)
        {
            System.out.println("\nError reading from file: " + fileName);
            return -2;
        }

        //Step 3:  Close the file
        try
        {
            file.close();
        }
        catch(IOException e)
        {
            System.out.println("\nError closing file: " + fileName);
            return -3;
        }

        //Step 4:  Check for empty file
        if (m_prog.size() == 0)
        {
            System.out.println("\nERROR: empty program file: " + fileName);
            return -4;
        }

        //Step 5:  Fix orphan label references
        if (fixOrphans() != 0)
        {
            return -5;
        }

        //Step 6:  Add an exit system call to the end of the program
        m_prog.add(new Integer(CPU.SET));
        m_prog.add(new Integer(0));
        m_prog.add(new Integer(0));
        m_prog.add(new Integer(0));
        m_prog.add(new Integer(CPU.PUSH));
        m_prog.add(new Integer(0));
        m_prog.add(new Integer(0));
        m_prog.add(new Integer(0));
        m_prog.add(new Integer(CPU.TRAP));
        m_prog.add(new Integer(0));
        m_prog.add(new Integer(0));
        m_prog.add(new Integer(0));


        return retVal;
        
    }//load
 

    /**
     * print
     *
     * outputs the program in integer format to the console.  (Used for
     * debugging.)
     * 
     */
    public void print()
    {
        int i = 0;              // counter variable
        for(Integer intNum : m_prog)
        {
            int num = intNum.intValue(); // extract int from Integer
            System.out.print("\t" + num);
            if ( (i > 0) && ((i+1) % CPU.INSTRSIZE == 0) )
            {
                System.out.println("");
            }
            i++;
        }//for
        
    }//print

    /**
     * export
     *
     * converts the current program from an Vector of Integer to int[] that
     * can be loaded into RAM
     *
     * @return the converted program
     * @see RAM
     */
    public int[] export()
    {
        if (m_prog.size() == 0) return null;
        
        int result[] = new int[m_prog.size()]; // the return value
        int i = 0;                             // counter/index
        for(Integer intTmp : m_prog)
        {
            result[i] = intTmp.intValue();
            i++;
        }

        return result;
    }//export
};//class Program
