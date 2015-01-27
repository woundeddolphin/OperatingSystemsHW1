#This program is designed to test all the Pidgin instructions
#If the program is executed correctly. When it completes, the registers
# should have these values:    r0=0 r1=10 r2=89 r3=144 r4=1
#@author Justice Nichols
#@author Krismy Alfaro
#@author Matthew Farr

set r0 10 # number of iterations
set r1 0 # counter
set r2 1 # this means fibanacci sequence starts at 1
set r3 1

:loop
add r4 r2 r3	#calculate next number
copy r2 r3
copy r3	r4
set r4 1		#incrementer
add r1 r4 r1	#increment counter
bne r1 r0 loop	#branch if fewer than r0 times

