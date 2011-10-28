#include <msp430x21x2.h>

#define VOLTAGE_CHECK_ 0x01C0
sfrw(VOLTAGE_CHECK, VOLTAGE_CHECK_);

int poo (void) {
    return 5;
}

#define NUMMEAS 100
int main (void) {
    int i,j,k;
    j = VOLTAGE_CHECK;
    int A[NUMMEAS];
    for (i = 0; i < NUMMEAS; ++i)
        A[i] = VOLTAGE_CHECK;
    j = poo();
    k = VOLTAGE_CHECK;
}
