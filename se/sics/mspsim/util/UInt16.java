package se.sics.mspsim.util;

import java.lang.Math;

public class UInt16 {
    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 0xffff;

    /* Returns the result of performing unsigned 16-bit integer addition of the
     * two arguments.
     * @throws IllegalArgumentException if either a or b requires more than 16
     *         bits to represent. */
    public static int add (int a, int b) {
        if ((Math.abs(a) > MAX_VALUE) || (Math.abs(b) > MAX_VALUE))
            throw new IllegalArgumentException("Argument out of range");

        if (a < MIN_VALUE)
            a += (MAX_VALUE + 1);
        if (b < MIN_VALUE)
            b += (MAX_VALUE + 1);

        return (a + b) & MAX_VALUE;
    }

    /* Test whether a + b = expected.
     * Note: run java with assertions enabled (-ea) */
    public static void testAdd (int a, int b, int expected) {
        int ans = UInt16.add(a, b);
        assert (ans == expected) : "Wrong answer (expected " + expected +
                                    ", got " + ans + ")";
    }

    public static void testAddition () {
        testAdd(MIN_VALUE,  MIN_VALUE,  MIN_VALUE);
        testAdd(MAX_VALUE,  MIN_VALUE,  MAX_VALUE);
        testAdd(MAX_VALUE,  MAX_VALUE,  0xfffe);
        testAdd(-6,         0x220,      538);
        testAdd(100,        0xfffe,     98);
    }
}
