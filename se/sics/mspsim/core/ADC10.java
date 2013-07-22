/**
 * Copyright (c) 2007, Swedish Institute of Computer Science. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. Neither the name of the
 * Institute nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This file is part of MSPSim.
 *
 * $Id$
 *
 * -----------------------------------------------------------------
 *
 * ADC10
 *
 * Each time a sample is converted the ADC10 system will check for EOS flag and
 * if not set it just continues with the next conversion (x + 1). If EOS next
 * conversion is startMem. Interrupt is triggered when the IE flag are set!
 *
 *
 * Author : Joakim Eriksson Created : Sun Oct 21 22:00:00 2007 Updated : $Date$
 * $Revision$
 */
package se.sics.mspsim.core;

import java.util.Arrays;
import edu.umass.energy.PowerSupply;

public class ADC10 extends IOUnit {
    // ADC10 control registers
    public static final int ADC10AE0 = 0x004A;
    public static final int ADC10AE1 = 0x004B;
    public static final int ADC10CTL0 = 0x01B0;
    public static final int ADC10CTL1 = 0x01B2;
    public static final int ADC10MEM = 0x01B4;
    public static final int ADC10DTC0 = 0x0048;
    public static final int ADC10DTC1 = 0x0049;
    public static final int ADC10SA = 0x01BC;

    public static final int ADC10_CYCLES = 647; // # cycles for ADC read

    // sample-and-hold time.  SHTBITS[ADC10SHTx] = number of ADC10CLKs
    public static final int[] SHTBITS = new int[]{ 4, 8, 16, 64 };

    public static final int BUSY_MASK = 0x01;
    public static final int EOS_MASK = 0x80;
    public static final int CONSEQ_SINGLE = 0x00;
    public static final int CONSEQ_SEQUENCE = 0x01;
    public static final int CONSEQ_REPEAT_SINGLE = 0x02;
    public static final int CONSEQ_REPEAT_SEQUENCE = 0x03;
    public static final int CONSEQ_SEQUENCE_MASK = 0x01;
    private int adc10ctl0 = 0;
    private int adc10ctl1 = 0;
    private int adc10Mem = 0;
    private int shTime = 4;
    private boolean adc10On = false;
    private boolean enableConversion;
    private boolean startConversion;
    private boolean isConverting;
    private int shSource = 0;
    private int startMem = 0;
    private int adcDiv = 1;
    private ADCInput adcInput;
    private int conSeq;

    private boolean adc10ie = false;
    private boolean adc10ifg = false;
    private int adc10SSel = 0;
    private int adc10Vector = 21;
    private int adc10dtc0 = 0;
    private int adc10dtc1 = 0;
    private int adc10sa = 0;

    private TimeEvent adcTrigger = new TimeEvent(0) {
        public void execute (long t) {
            convert();
            cpu.getPowerSupply().setPowerMode(PowerSupply.POWERMODE_ACTIVE);
        }
    };

    public ADC10 (MSP430Core cpu) {
        super("ADC10", cpu, cpu.memory, 0);
    }

    public void reset (int type) {
        enableConversion = false;
        startConversion = false;
        isConverting = false;
        adc10ctl0 = 0;
        adc10ctl1 = 0;
        shTime = SHTBITS[0];
        adc10On = false;
        shSource = 0;
        adcDiv = 1;
        conSeq = 0;
        adc10ie = false;
        adc10ifg = false;
        adc10SSel = 0;
        adc10Mem = 0;
    }

    public void setADCInput (ADCInput inp) {
        adcInput = inp;
    }

    // write a value to the IO unit
    public void write(int address, int value, boolean word, long cycles) {
        switch (address) {
            case ADC10CTL0:
                if (enableConversion) {
                    // only some values can be changed if enableConversion set
                    adc10ctl0 = (adc10ctl0 & 0xfff0) | (value & 0xf);
                } else {
                    adc10ctl0 = value;
                    shTime = SHTBITS[(value >> 11) & 0x03]; // bits 11 & 12
                    adc10On = (value & 0x10) > 0;
                }
                enableConversion = (value & 0x02) > 0;
                startConversion = (value & 0x01) > 0;
                if (DEBUG) {
                    log("Set SHT=" + shTime + "; ENC=" + enableConversion
                            + "; SC=" + startConversion + "; ADC10ON=" + adc10On);
                }
                if (adc10On && enableConversion && startConversion && !isConverting) {
                    isConverting = true;
                    int delay = adcDiv * (shTime + 13);
                    cpu.scheduleTimeEvent(adcTrigger, cpu.getTime() + delay);
                }
                break;
            case ADC10CTL1:
                if (enableConversion) {
                    adc10ctl1 = (adc10ctl1 & 0xfff8) + (value & 0x6);
                } else {
                    adc10ctl1 = value & 0xfffe;
                    shSource = (value >> 10) & 0x3;
                    adcDiv = ((value >> 5) & 0x7) + 1;
                    adc10SSel = (value >> 3) & 0x03;
                }
                conSeq = (value >> 1) & 0x03;
                if (DEBUG) {
                    log("Set SHSource=" + shSource + "; Conseq-mode=" + conSeq
                            + "; Div=" + adcDiv + "; ADCSSEL=" + adc10SSel);
                }
                break;
            default:
                break;
        }
    }

    // read a value from the IO unit
    public int read(int address, boolean word, long cycles) {
        switch (address) {
            case ADC10CTL0:
                return adc10ctl0;
            case ADC10CTL1:
                return isConverting ? (adc10ctl1 | BUSY_MASK) : adc10ctl1;
            case ADC10MEM:
                adc10ifg = false;
                cpu.flagInterrupt(adc10Vector, this, false); // unflag
                return adc10Mem;
            case ADC10DTC0:
                return adc10dtc0;
            case ADC10DTC1:
                return adc10dtc1;
            case ADC10SA:
                return adc10sa;
            default:
                break;
        }
        return 0;
    }
    int smp = 0;

    private void convert() {
        // If off then just return...
        if (!adc10On) {
            isConverting = false;
            return;
        }
        boolean runAgain = enableConversion && conSeq != CONSEQ_SINGLE;

        // XXX ADC10...
        ADCInput input = adcInput;
        adc10Mem = input.nextData();
        /*
        // Some noise...
        ADCInput input = adcInput[adc12mctl[adc12Pos] & 0xf];
        adc12mem[adc12Pos] = input != null ? input.nextData() : 2048 + 100 - smp & 255;
        smp += 7;
        */
        
        adc10ifg = true;
        if (adc10ie) {
            cpu.flagInterrupt(adc10Vector, this, true);
        }

        // TODO: handle conseq
        if (!runAgain) {
            isConverting = false;
        } else {
            int delay = adcDiv * (shTime + 13);
            cpu.scheduleTimeEvent(adcTrigger, adcTrigger.time + delay);
        }
        int delay = adcDiv * (shTime + 13) + ADC10_CYCLES;
        System.err.println("cycles=" + cpu.cycles);

        cpu.scheduleTimeEvent(adcTrigger, adcTrigger.time + delay);
    }

    public void interruptServiced(int vector) {
    }
}
