#include <msp430.h>
#include <stdlib.h> /* for rand() */

#define VOLTAGE_CHECK_ 0x01C0
sfrw(VOLTAGE_CHECK, VOLTAGE_CHECK_);

void inf (void) {
    int a;
    if (rand() < 16384) {
        a = VOLTAGE_CHECK;
    }
}

int main (void) {
    int a, b, c;
    unsigned d = 0xbebe;
    while (1) {
        inf();
    }
}
