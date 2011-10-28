CC=clang
CFLAGS=-S -O3 -ccc-host-triple msp430-elf -I../../build/msp430/include
CFILES=$(wildcard *.c)

all: $(CFILES:.c=.s)

%.s: %.c
	$(CC) $(CFLAGS) -o $@ $<

%.exe: %.s
	msp430-gcc -mmcu=msp430x2132 -o $@ $<

.PHONY: clean
clean:
	rm -f *.exe *.s
