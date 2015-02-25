OperatingSystems HW
===================
University of Portland CS 446 Nuxoll

This repo contains a basic SOS for the entirety of University of Portlands OS class

The commits are generally organized by step in homework assignments


Homework 1
----------

This assignment established the foundation for the OS and its hardware 

Homework 2
----------

This assigment added support for system calls including
* SYSCALL_EXIT
* SYSCALL_OUTPUT
* SYCALL_GETPID
* SYSCALL_COREDUMP

Homework 3 
----------

This assignment added basic I/O support adding
* SYSCALL_OPEN
* SYSCALL_CLOSE
* SYSCALL_WRITE
* SYSCALL_READ

and devices
* keyboardDevice
* consoleDevice

Homework 4
----------
This assignment added basic multiprogramming support 

meaing now multiple processes can be spawned and closed in ram through
* processControlBlocks
* SYSCALL_YIELD
* SYSCALL_EXEC

Homework 5
----------
This assignment added thread support creating 4 threads for
* main
* SOS
* console 
* keyboard
