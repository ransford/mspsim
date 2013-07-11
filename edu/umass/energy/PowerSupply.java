package edu.umass.energy;

import static edu.umass.energy.Capacitor.POWERMODE_ACTIVE;
import static edu.umass.energy.Capacitor.POWERMODE_ADC;
import static edu.umass.energy.Capacitor.POWERMODE_FLWRI;
import static edu.umass.energy.Capacitor.POWERMODE_LPM0;
import static edu.umass.energy.Capacitor.POWERMODE_LPM1;
import static edu.umass.energy.Capacitor.POWERMODE_LPM2;
import static edu.umass.energy.Capacitor.POWERMODE_LPM3;
import static edu.umass.energy.Capacitor.POWERMODE_LPM4;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.StopExecutionException;

/**
 * Abstraction of a power supply.
 */
public abstract class PowerSupply extends IOUnit {
    protected double voltage;
    public boolean traceDriven = false;
    private long numLifecycles = 1;

    protected int powerMode;
    public static final int POWERMODE_ACTIVE = 0;
    public static final int POWERMODE_LPM0 = 1;
    public static final int POWERMODE_LPM1 = 2;
    public static final int POWERMODE_LPM2 = 3;
    public static final int POWERMODE_LPM3 = 4;
    public static final int POWERMODE_LPM4 = 5;
    public static final int POWERMODE_FLWRI = 100;
    public static final int POWERMODE_ADC = 101;

    // Read this (otherwise unused) memory address to get this PowerSupply's
    // current voltage.
    public static final int getVoltageAddress = 0x01C0;

    /* Sets the power mode (e.g., active, LPM0, ...).  The constants are defined
     * in se.sics.mspsim.core.MSP430Constants.MODE_NAMES. */
    public void setPowerMode(int mode) {
        System.err.print("setPowerMode ");
        switch (this.powerMode) {
            case POWERMODE_ACTIVE:
                System.err.println("ACTIVE");
                break;
            case POWERMODE_LPM0:
                System.err.println("LPM0");
                break;
            case POWERMODE_LPM1:
                System.err.println("LPM1");
                break;
            case POWERMODE_LPM2:
                System.err.println("LPM2");
                break;
            case POWERMODE_LPM3:
                System.err.println("LPM3");
                break;
            case POWERMODE_LPM4:
                System.err.println("LPM4");
                break;
            case POWERMODE_FLWRI:
                System.err.println("FLWRI");
                break;
            case POWERMODE_ADC:
                System.err.println("ADC");
                break;
        }
        this.powerMode = mode;
    }

    public PowerSupply(String id, MSP430 msp) {
        super(id, msp, msp.memory, getVoltageAddress);
    }

    public abstract int read(int address, boolean word, long cycles);

    public abstract void write(int address, int value, boolean word,
            long cycles);

    public abstract void reset();
    
    public abstract double getVoltage();

    /**
     * Do something to recover from a power failure, such as recharging to a
     * healthier voltage in the case of a rechargeable supply.  Probably a no-op
     * for a non-rechargeable supply.
     * @return the amount of time it took to convalesce
     */
    public abstract double convalesce();
    
    public void incrementNumLifecycles() {
        numLifecycles++;
    }
    
    public long getNumLifecycles() {
        return numLifecycles;
    }
    
    /**
     * Update the power supply's voltage if it is meaningful to do so.
     * @throws StopExecutionException if the new voltage state dictates a power
     * failure.
     */
    public abstract void updateVoltage() throws StopExecutionException;
}