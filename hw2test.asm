#This program calculates the current
#process id and prints it via a system call
#@author Zak Pearson
#@author Justice Nichols    
#get process id system call
SET  r0 2   #r0 = SYSCALL_GETPID
PUSH r0
TRAP

#store the PID in r1
POP  r1

#output system call
SET  r0 1    #r0 = SYSCALL_OUTPUT
PUSH r1
PUSH r0
TRAP

#core dump system call

SET R1 3
PUSH R1
SET R0 9
PUSH R0
TRAP

#Though puzzle:  How could you make this 
#                program two lines shorter?


