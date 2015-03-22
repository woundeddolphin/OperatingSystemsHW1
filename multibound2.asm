####################################################
#This program prints 20 keystrokes to the console
#then operates in a tight loop that uses no devices.
#The keyboard device should have id 0.  The console
#device should have id 1.
###################################################

#========================================
#This is the I/O bound portion
#========================================
#Initialize the variables
SET r1 0       #counter
SET r2 1       #increment amount
SET r3 20      #limit

#Main Loop
:loop
ADD r1 r2 r1

#Reserve the keyboard device
SET r0 0       #device #0 (keyboard)
PUSH r0        #push argument on stack
SET r4 3       #OPEN sys call id
PUSH r4        #push sys call id on stack
TRAP           #open the device

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 err #exit program on error

#Reserve the console device
SET r0 1       #device #1 (console output)
PUSH r0        #push argument on stack
SET r4 3       #OPEN sys call id
PUSH r4        #push sys call id on stack
TRAP           #open the device

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 err #exit program on error

#Read a keystroke from the keyboard
SET r0 0       #device #0 (keyboard)
PUSH r0        #push device number
PUSH r0        #push address (arg not used by this device so any val will do)
SET r0 5       #READ system call
PUSH r0        #push system call id
TRAP           #system call to read the value

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 err #exit program on error

#save the keystroke
POP r4         #save the value in r4

#Write the value to the console
SET r0 1       #device #1 (console output)
PUSH r0        #push device number
PUSH r0        #push address (arg not used by this device so any val will do)
PUSH r4        #push value to send to device
SET r0 6       #WRITE system call
PUSH r0        #push system call id
TRAP           #system call to write the value

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 err #exit program on error

#close the keyboard device
SET r4 0       #keyboard device id
PUSH r4        #push device number 0 (keyboard)
SET r4 4       #CLOSE sys call id
PUSH r4        #push the sys call id onto the stack
TRAP           #close the device

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Success code
BNE r0 r4 err #exit program on error

#close the console device
SET r4 1       #keyboard device id
PUSH r4        #push device number 1 (console output)
SET r4 4       #CLOSE sys call id
PUSH r4        #push the sys call id onto the stack
TRAP           #close the device

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Success code
BNE r0 r4 err #exit program on error

#loop test
BNE r1 r3 loop

#========================================
#This is the CPU bound portion
#========================================
#Initialize the loop variables
SET r1 0       #counter
SET r2 1       #increment amount
SET r3 100      #limit

#Main Loop
:cpuloop
ADD r1 r2 r1

#Save the loop variables
PUSH r1
PUSH r2
PUSH r3

#This program calculates C(R0,R1) as per
#discrete mathematics.  Initialize R0 = R1 + 3
SET R2 3             
ADD R0 r1 r2

#STEP 1: Calculate m! and put it onto the stack
#Setup some initial variables
SET R2 1       #increment amount
SET R3 0       #counter (this will count backwards)
ADD R3 R3 R0
SET R4 1       #result

#Calculate m!
:factm
MUL R4 R4 R3          #multiply the counter into R4
SUB R3 R3 R2          #decrement the counter
BLT R2 R3 factm       #repeat until R3==0

#Push the result
PUSH R4        #push the result 

#STEP 2: Calculate n! and put it onto the stack
#Setup some initial variables
SET R2 1       #increment amount
SET R3 0       #counter (this will count backwards)
ADD R3 R3 R1
SET R4 1       #result

#Calculate
:factn
MUL R4 R4 R3          #multiply the counter into R4
SUB R3 R3 R2          #decrement the counter
BLT R2 R3 factn       #repeat until R3==0

#Push the result
PUSH R4        #push the result 

#STEP 3: Calculate (m-n)! and put it onto the stack
#Setup some initial variables
SET R2 1       #increment amount
SUB R3 R0 R1   #counter (this will count backwards)
SET R4 1       #result

#Calculate
:factmn
MUL R4 R4 R3          #multiply the counter into R4
SUB R3 R3 R2          #decrement the counter
BLT R2 R3 factmn      #repeat until R3==0

#Push the result
PUSH R4        #push the result

#STEP 4: Calculate the and print the result using the
#        formula:  m! / ((n!)(m-n!))
#Pop off our values into registers
POP R4        #R4 = (m-n)!
POP R3        #R3 = n!
POP R2        #R2 = m!

#Calculate the result
MUL R1 R3 R4
    
#Restore the main loop variables
POP r3
POP r2
POP r1

#loop test
BNE r1 r3 cpuloop

#========================================
#This is a normal exit from the program
#========================================
:exit
SET  r4 0      #EXIT system call id
PUSH r4        #push sys call id on stack
TRAP           #exit the program
    
#========================================
#flag that an ERROR occurred by printing 999999 ten times
#========================================
:err

#Initialize the loop variables
SET r1 0       #counter
SET r2 1       #increment amount
SET r3 10      #limit

#Main Loop
:errloop
ADD r1 r2 r1

#Print 999999
SET  r4 999999 #flag value
PUSH r4        #push flag on stack
SET  r4 1      #OUTPUT system call id
PUSH r4        #push sys call id on stack
TRAP           #OUTPUT 999999

#loop test
BNE r1 r3 errloop

BRANCH exit    #exit the program
