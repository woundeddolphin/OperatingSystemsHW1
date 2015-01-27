#This program counts from 1 to 10
SET R1 0       #counter
SET R2 1       #increment amount
SET R3 10      #limit

:loop
ADD R1 R2 R1   #increment R1
BNE R1 R3 loop #repeat until R1=R3



TRAP
