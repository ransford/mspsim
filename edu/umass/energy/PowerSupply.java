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
public abstract class PowerSupply {
    protected double voltage;
    public boolean traceDriven = false;
    protected long numLifecycles = 1;
    protected PowerSupplyIO psio;
    protected String id;
    protected MSP430 cpu;

    protected int powerMode;
    public static final int POWERMODE_ACTIVE = 0;
    public static final int POWERMODE_LPM0 = 1;
    public static final int POWERMODE_LPM1 = 2;
    public static final int POWERMODE_LPM2 = 3;
    public static final int POWERMODE_LPM3 = 4;
    public static final int POWERMODE_LPM4 = 5;
    public static final int POWERMODE_FLWRI = 100;
    public static final int POWERMODE_ADC = 101;
    public static final int POWERMODE_NONE = 9999;

    // Read this (otherwise unused) memory address to get this PowerSupply's
    // current voltage.
    public static final int getVoltageAddress = 0x01C0;
    
    protected ClockSource clockSource;

    /* Sets the power mode (e.g., active, LPM0, ...).  The constants are defined
     * in se.sics.mspsim.core.MSP430Constants.MODE_NAMES. */
    public void setPowerMode(int mode) {
        System.err.print("setPowerMode ");
        switch (mode) {
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
            case POWERMODE_NONE:
                System.err.println("NONE");
                break;
            default:
                System.err.println("(Unknown)");
        }
        this.powerMode = mode;
    }
    
    public PowerSupply (String id) {
        this.id = id;
    }

    public void setCpu (MSP430 msp) {
        this.cpu = msp;
        setClockSource(this.cpu);
        System.out.println("this.cpu=" + this.cpu);
        psio = new PowerSupplyIO(this.id, this.cpu);
        cpu.setIORange(PowerSupply.getVoltageAddress, 2, psio);
    }
    
    public abstract void initialize();

    public abstract void reset();
    
    public abstract double getVoltage();

    /**
     * Do something to recover from a power failure, such as recharging to a
     * healthier voltage in the case of a rechargeable supply.  Probably a no-op
     * for a non-rechargeable supply.
     * @return the amount of time it took to recover
     */
    public abstract double recover();

    /**
     * Stop affecting the platform's operation.
     */
    public abstract void disable();
    
    public void incrementNumLifecycles() {
        numLifecycles++;
    }
    
    public long getNumLifecycles() {
        return numLifecycles;
    }
    
    public abstract String getStatus ();

    /**
     * Update the power supply's voltage if it is meaningful to do so.
     * @throws StopExecutionException if the new voltage state dictates a power
     * failure.
     */
    public abstract void updateVoltage() throws StopExecutionException;
    
    
    public void setClockSource (ClockSource c) {
        this.clockSource = c;
    }
}