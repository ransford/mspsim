#include <msp430x21x2.h>

int main (void) {
    WDTCTL = WDTPW + WDTHOLD;         // stop WDT
    __bis_SR_register(LPM3_bits);     // enter LPM3
}
