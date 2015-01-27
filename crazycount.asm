#This program is designed to test all the Pidgin instructions
#If the program is executed correctly, it will execute for 102
#instructions and stop.  When it completes, the registers should
#have these values:    r0=0 r1=3 r2=0 r3=3 r4=0

SET r0 77777
PUSH r0
SET r0 0
SET r3 0
SAVE r3 r0
BRANCH skip
SET r3 0
BRANCH end
:skip
POP r0
SET r4 99
DIV r0 r0 r4 
ADD r0 r0 r0
SET r2 2
PUSH r0
MUL r0 r2 r0
PUSH r0
SET r1 54321
POP r0
PUSH r1
SET r0 99999
POP r0
COPY r4 r0
SUB r1 r0 r4
SET r2 1       #increment amount
SET r3 3       #limit
POP r4

:loop
ADD r1 r2 r1
BNE r1 r3 loop
SET r0 0
LOAD r3 r0
ADD r3 r2 r3
SAVE r3 r0
COPY r2 r4
PUSH r2
BLT r1 r4 skip

:end

TRAP
