#This program should create a
#divide by zero interrupt
SET r0 0
SET r1 11
DIV r2 r1 r0
PUSH r0
TRAP
