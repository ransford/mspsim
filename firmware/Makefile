CC=msp430-gcc
CFLAGS=-mmcu=msp430f2132
CFILES=$(wildcard *.c)
EXECUTABLES=$(CFILES:.c=)

all: $(EXECUTABLES)

.PHONY: clean
clean:
	rm -f $(EXECUTABLES)
