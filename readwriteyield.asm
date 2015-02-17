####################################################
#This program prints 20 keystrokes to the console
#The keyboard device should have id 0.  The console
#device should have id 1.
# Add each iteration this program also yields
# the CPU to other processes.
###################################################

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
SET r0 0       #Success code
BNE r0 r4 exit #exit program on error

#Reserve the console device
SET r0 1       #device #1 (console output)
PUSH r0        #push argument on stack
SET r4 3       #OPEN sys call id
PUSH r4        #push sys call id on stack
TRAP           #open the device

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 exit #exit program on error

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
BNE r0 r4 exit #exit program on error

#save the keystroke
POP r4         #save the value in r4

#yield the CPU to other processes
SET r0 8       #YIELD sys call id
PUSH r0        #push the sys call id onto the stack
TRAP           #make the system call
    
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
SET r0 0       #Success code
BNE r0 r4 exit #exit program on error

#close the keyboard device
SET r4 0       #keyboard device id
PUSH r4        #push device number 0 (keyboard)
SET r4 4       #CLOSE sys call id
PUSH r4        #push the sys call id onto the stack
TRAP           #close the device

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Success code
BNE r0 r4 exit #exit program on error

#close the console device
SET r4 1       #keyboard device id
PUSH r4        #push device number 1 (console output)
SET r4 4       #CLOSE sys call id
PUSH r4        #push the sys call id onto the stack
TRAP           #close the device

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Success code
BNE r0 r4 exit #exit program on error

#yield the CPU to other processes
SET r4 8       #YIELD sys call id
PUSH r4        #push the sys call id onto the stack
TRAP           #make the system call

#loop test
BNE r1 r3 loop

#exit syscall
:exit
SET  r4 0      #EXIT system call id
PUSH r4        #push sys call id on stack
TRAP           #exit the program
