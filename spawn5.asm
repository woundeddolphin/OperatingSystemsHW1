####################################################
# This program prints the numbers between 100 and 500
# in incrments of 100 to the console device.
# It spawns another process at each iteration of the
# loop.
###################################################

#Initialize the variables
SET r1 0       #counter
SET r2 100     #increment amount
SET r3 500     #limit

#begin loop
:loop
ADD r1 r2 r1

#Reserve the console device
SET r0 1       #device #1 (console output)
PUSH r0        #push device id on stack
SET r4 3       #OPEN sys call id
PUSH r4        #push sys call id on stack
TRAP           #open the device

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 exit #exit program on error

#print the current value in the count to the console
SET r4 1       #device id 1 = console
PUSH r4        #push device number 
PUSH r0        #push address (arg not used by this device so any val will do)
PUSH r1        #push value to send to device
SET r4 6       #WRITE system call id
PUSH r4        #push the sys call id
TRAP           #system call to print the value

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 exit #exit program on error

#close the console device
SET r0 1       #device number 1 (console output)
PUSH r0        #push device number
SET r4 4       #CLOSE sys call id
PUSH r4        #push the sys call id onto the stack
TRAP           #close the device

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 exit #exit program on error

#spawn a new process
SET r4 7       #EXEC sys call id
PUSH r4        #push the sys call id onto the stack
TRAP           #make the system call

#end of loop
BNE r1 r3 loop #repeat 5 times

#exit syscall
:exit
SET  r4 0      #EXIT system call id
PUSH r4        #push sys call id on stack
TRAP           #exit the program


