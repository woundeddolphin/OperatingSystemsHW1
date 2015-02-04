#This program calculates the current
#process id and prints it via a system call
#@author Zak Pearson
#@author Justice Nichols

#get process id system call
SET  r0 2   #r0 = SYSCALL_GETPID
PUSH r0
TRAP

#output system call
SET  r0 1    #r0 = SYSCALL_OUTPUT
PUSH r0
TRAP

#Though puzzle:  How could you make this 
#                program two lines shorter?


