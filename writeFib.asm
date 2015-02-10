####################################################
#This program prints fib seq to the console
#The keyboard device should have id 0.  The console
#device should have id 1.
###################################################

#Reserve the keyboard device
SET r0 0       #device #0 (keyboard)
PUSH r0        #push argument on stack
SET r4 3       #OPEN sys call id
PUSH r4        #push sys call id on stack
TRAP           #open the device

#Check for failure

POP r4         #get return code from the system call
PUSH r0
SET r0 0       #Succes code
BNE r0 r4 exit #exit program on error
POP r0

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

#Initialize the variables
SET r1 0       #counter
SET r2 1       #increment amount

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
POP r0         #save the value in r4
set r1 0 # counter
set r2 1 # this means fibanacci sequence starts at 1
set r3 1
#########

#Main Loop
:loop

add r4 r2 r3	#calculate next number
copy r2 r3
copy r3	r4
set r4 1		#incrementer
add r1 r4 r1	#increment counter

#Write the value to the console
SET r4 1       #device #1 (console output)
PUSH r4        #push device number
PUSH r4        #push address (arg not used by this device so any val will do)
PUSH r3        #push value to send to device
SET r4 6       #WRITE system call
PUSH r4        #push system call id
TRAP           #system call to write the value

#Check for failure

POP r4         #get return code from the system call
PUSH r0
SET r0 0       #Success code
BNE r0 r4 exit #exit program on error
POP r0

#loop test
BNE r1 r0 loop

#close the keyboard device
SET r4 0       #keyboard device id
PUSH r4        #push device number 0 (keyboard)
SET r4 4       #CLOSE sys call id
PUSH r4        #push the sys call id onto the stack
TRAP           #close the device

#Retrieve but ignore success/error code (we're exiting anyway)
POP r4

#close the console device
SET r4 1       #keyboard device id
PUSH r4        #push device number 1 (console output)
SET r4 4       #CLOSE sys call id
PUSH r4        #push the sys call id onto the stack
TRAP           #close the device


#exit syscall
:exit
POP r0
SET  r4 0      #EXIT system call id
PUSH r4        #push sys call id on stack
TRAP           #exit the program
