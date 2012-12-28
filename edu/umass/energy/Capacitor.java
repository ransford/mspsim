package edu.umass.energy;
import java.lang.Math;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.IOUnit;

public class Capacitor extends IOUnit {
    private double capacitance;
    private double voltage;
    private double effectiveMaxVoltage;
    private double inputVoltageDividerFactor;
    private double inputVoltageReferenceVoltage;
    private double A; // _The Art of Electronics_, 2nd Ed., p. 23
    private double lastATime = 0; // when was A last set?
    private static final double meaninglessStartTime = -1.0;
    private double startTime = meaninglessStartTime;
    private MSP430 cpu;
    private double initialVoltage;
    private long numSetVoltages = 0;
    private long numUpdateVoltages = 0;
    public long accumCycleCount = 0;
    private long numLifecycles = 1;
    private long printCounter = 0;
    private int powerMode;
    private boolean suppressVoltagePrinting = false;
    private boolean suppressEnergyPrinting = false;
    public EnergyFairy eFairy;

    private boolean enabled = true;

    public static final int POWERMODE_ACTIVE = 0;
    public static final int POWERMODE_LPM0 = 1;
    public static final int POWERMODE_LPM1 = 2;
    public static final int POWERMODE_LPM2 = 3;
    public static final int POWERMODE_LPM3 = 4;
    public static final int POWERMODE_LPM4 = 5;
    public static final int POWERMODE_FLWRI = 100;
    public static final int POWERMODE_ADC = 101;
    
    public static final int ADC_CYCLES = 647;

    /* Spreadsheety-looking comments in this block refer to the Google
     * spreadsheet at
     * https://spreadsheets.google.com/ccc?key=0AjDLMKJ2t5rLdFFRME5CeTQ1WkNHMEV5UmVyTi1XVHc&hl=en
     * Furthermore, resistance numbers here refer to 4.5V numbers.  Units are
     * ohms.
     public static final double MSP430_RESISTANCE_ADC  =   14504; // E86
     public static final double MSP430_RESISTANCE_ACTIVE = 19007; // E16
     public static final double MSP430_RESISTANCE_FLWRT  = 21505; // E100
     public static final double MSP430_RESISTANCE_LPM0 =   82647; // E72
     public static final double MSP430_RESISTANCE_LPM1 =   82702; // E58
     public static final double MSP430_RESISTANCE_LPM2 =  219560; // E44
     public static final double MSP430_RESISTANCE_LPM3 = 1560000; // E2
     public static final double MSP430_RESISTANCE_LPM4 = 1801200; // E30
     */

    private CapClockSource clockSource;
    // private double defaultResistance = MSP430_RESISTANCE_ACTIVE;
    private double eFairyPrevVoltage = 0.0;

    public double resistanceADCRead (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9824
        return (3346.2 * voltage) + 1040.8;
    }

    double resistanceActive (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9958
        return (4010.6 * voltage) + 803.53;
    }

    public double resistanceFlashWrite (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9998
        return (4747.8 * voltage) + 152.98;
    }

    double resistanceLPM0 (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9999
        return (18232 * voltage) + 1017.9;
    }

    double resistanceLPM1 (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9999
        return (18230 * voltage) + 1014.3;
    }

    double resistanceLPM2 (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.999
        return (48202 * voltage) + 6229.5;
    }

    double resistanceLPM3 (double voltage) {
        // Cubic regression calculated from spreadsheet; R^2 = 0.994
        return
            (-66859 * Math.pow(voltage, 3))
            + (532699 * Math.pow(voltage, 2))
            - (979608 * voltage)
            + 1e6;
    }

    double resistanceLPM4 (double voltage) {
        // Cubic regression calculated from spreadsheet; R^2 = 0.9944
        return
            (-128337 * Math.pow(voltage, 3))
            + (1e6 * Math.pow(voltage, 2))
            - (2e6 * voltage)
            + 3e6;
    }

    /* unused on MSP430F2132, believed unused on MSP430F1611; should be safe to
       inhabit this address. */
    public static final int voltageReaderAddress = 0x01C0;

    /**
      * @param msp The MSP430 whose livelihood depends on this Capacitor
      * @param C Capacitance in farads, e.g. 10e-6 == 10uF.
      * @param initialVoltage Initial voltage in volts, e.g. 4.5 == 4.5V.
      */
    public Capacitor (MSP430 msp, double C, double initVoltage,
            double inputVoltageDividerFactor,
            double inputVoltageReferenceVoltage) {
        super("capacitor", msp, msp.memory, voltageReaderAddress);
        cpu = msp;
        capacitance = C;
        this.effectiveMaxVoltage =
            (inputVoltageDividerFactor * inputVoltageReferenceVoltage);
        this.setPowerMode(POWERMODE_ACTIVE);
        setClockSource(msp);
        setInitialVoltage(initVoltage);
    }

    public void setEnergyFairy (EnergyFairy ef) {
        this.eFairy = ef;
    }

    public void setInitialVoltage (double initVoltage) {
        this.voltage = initialVoltage = initVoltage;
        setA(initialVoltage, false);
    }

    public void reset () {
        setPowerMode(POWERMODE_ACTIVE);
        startTime = meaninglessStartTime;
        setVoltage(initialVoltage);
        setA(voltage, false);
        numSetVoltages = 0;
        numUpdateVoltages = 0;
        System.err.println("Capacitor.reset(): voltage=" + voltage +
                "; startTime=" + startTime);
    }

    public double getElapsedTimeMillis () {
        return cpu.getTimeMillis() - startTime;
    }

    /**
     * @return Voltage, in volts.
     */
    public double getVoltage () { return voltage; }

    /**
     * Subtracts the given <tt>amount</tt> of energy from this Capacitor,
     * setting and returning the Capacitor's voltage.
     * @param amount Positive amount of energy to subtract, in joules.
     * @return The new voltage value, in volts.
     */
    public void dockEnergy (double amount) {
        /*
        double initialEnergy = 0.5 * capacitance * voltage * voltage;
        double newEnergy = initialEnergy - amount;
        double newVoltage = Math.sqrt(2.0 * newEnergy / capacitance);
        setVoltage(newVoltage);
        */
        // or, more verbosely (and quickly):
        setVoltage(Math.sqrt(2.0 * ((0.5 * capacitance * voltage * voltage) - amount) / capacitance));
    }

    protected void setVoltage (double V) {
            ++numSetVoltages;
            voltage = V;
            if(voltage > effectiveMaxVoltage) {
                System.err.println("Voltage exceeds maximum!");
            }
    }

    public long getNumSetVoltages () {
        return numSetVoltages;
    }

    public long getNumUpdateVoltages () {
        return numUpdateVoltages;
    }

    public String toString () {
        return "<" + this.getClass().getName() + ", C=" + capacitance + "F>";
    }

    /**
     * Does nothing.
     */
    public void write (int address, int value, boolean word, long cycles) {
    }

    /**
     * @param address The address to read, must equal this.address for sanity
     * @param word Whether to interpret the result as a word; must be true for
     *             sanity
     * @param cycles Whatever
     * @return the <code>Capacitor</code>'s voltage as a fraction of its
     *         maximum voltage, multiplied by 65536 (the maximum value of an int
     *         on MSP430).  For example, if the cap's voltage is 5.0 V and its
     *         maximum voltage is 10.0, this method will return (5.0/10.0)
     *         65536 = 32768.  The consumer of this value should interpret this
     *         as saying that the voltage is at half its maximum level.
     */
    public int read (int address, boolean word, long cycles) {
//        System.err.println("Trapped read to voltage check");
        if ((address != voltageReaderAddress) || !word) {
            return 0;
        }

        double vfrac = voltage / effectiveMaxVoltage;
        int scaled_amt = (int)(Math.round(vfrac * 65536));

        setPowerMode(POWERMODE_ADC);
        cpu.cycles += ADC_CYCLES;
        updateVoltage(true /* assume we're in the checkpoint routine */);
        setPowerMode(POWERMODE_ACTIVE);
        return scaled_amt;
    }

    /**
     * Sets A, the initial condition.
     * @param newValue The new value for A
     */
    public void setA (double newValue, boolean dead) {
        A = newValue;
        lastATime = clockSource.getTimeMillis();
        if (!dead)
            lastATime += cpu.getOffset();
    }

    /**
     * Returns the load resistance to be used in voltage calculations.
     */
    public double getResistance () {
        switch (powerMode) {
            case POWERMODE_ACTIVE: return resistanceActive(voltage);
            case POWERMODE_LPM0:   return resistanceLPM0(voltage);
            case POWERMODE_LPM1:   return resistanceLPM1(voltage);
            case POWERMODE_LPM2:   return resistanceLPM2(voltage);
            case POWERMODE_LPM3:   return resistanceLPM3(voltage);
            case POWERMODE_LPM4:   return resistanceLPM4(voltage);
            case POWERMODE_FLWRI:  return resistanceFlashWrite(voltage);
            case POWERMODE_ADC:    return resistanceADCRead(voltage);
        }
        throw new RuntimeException("Unknown power mode " + powerMode);
    }

    /* Sets the power mode (e.g., active, LPM0, ...).  The constants are defined
     * in se.sics.mspsim.core.MSP430Constants.MODE_NAMES. */
    public void setPowerMode (int mode) {
        System.err.print("Capacitor.setPowerMode ");
        switch (powerMode) {
            case POWERMODE_ACTIVE: System.out.println("ACTIVE"); break;
            case POWERMODE_LPM0: System.out.println("LPM0"); break;
            case POWERMODE_LPM1: System.out.println("LPM1"); break;
            case POWERMODE_LPM2: System.out.println("LPM2"); break;
            case POWERMODE_LPM3: System.out.println("LPM3"); break;
            case POWERMODE_LPM4: System.out.println("LPM4"); break;
            case POWERMODE_FLWRI: System.out.println("FLWRI"); break;
            case POWERMODE_ADC: System.out.println("ADC"); break;
        }
        this.powerMode = mode;
    }

    public void toggleHush () {
        suppressVoltagePrinting = !suppressVoltagePrinting;
        suppressEnergyPrinting = !suppressEnergyPrinting;
    }

    /**
     * P = E/t = VI, so E=tVI.
     * @return true if it's time to die, false otherwise
     */
    public boolean updateVoltage (boolean inCheckpoint) {
        if (!enabled) return false;
        double RC = getResistance() * capacitance;
        boolean dead = (clockSource instanceof DeadTimer);
        double currentTime = clockSource.getTimeMillis();
        if (!dead)
            currentTime += cpu.getOffset();
        double dt = currentTime - lastATime;
        boolean shouldSetVoltage = true;
        boolean shouldDie = false;

        // Give the fairy a crack at the voltage first.
        if(eFairy != null) {
            double V_applied = eFairy.getVoltage(currentTime);
            if (V_applied > eFairyPrevVoltage) { // eFairy wants to add energy
                double V_initial = voltage; // V = Q/C
                double V_final = V_initial +
                    V_applied * (1 - Math.exp((-dt) / 800.0*RC));
                setVoltage(V_final);
                // setVoltage(V_applied); // possibly large jump!

                double deltaV = V_final - V_initial;
                System.err.println("Added: " + deltaV + "V");
                shouldSetVoltage = false;
            }
            eFairyPrevVoltage = V_applied;
        }
        if (shouldSetVoltage)
            setVoltage(
                    voltage * // initial condition
                    Math.exp(
                            (-1.0 * dt) // time
                            / (800.0 * RC)
                    )
            );
        this.setA(getVoltage(), dead);

        if (dead) {
            if (printCounter++ % 10 == 0)
                System.err.println("<dead>" + (currentTime) + "," + getVoltage());
            return false;
        }

        if (voltage <= cpu.deathThreshold) {
            // accumCycleCount += cpu.cycles;
            shouldDie = true;
        }

        if (!suppressVoltagePrinting && (printCounter++ % 10 == 0))
            System.err.println((inCheckpoint ? "<chk>" : "") +
                    (clockSource.getTimeMillis() + cpu.getOffset()) +
                    "," + voltage);

        return shouldDie;
    }

    public void setClockSource (CapClockSource c) {
        this.clockSource = c;
    }

    /**
     *
     * @return the energy in Joules
     */
    public double getEnergy () {
        return (0.5 * capacitance * voltage * voltage);
    }

    public double getEffectiveMaxVoltage () {
        return effectiveMaxVoltage;
    }

    public long getNumLifecycles () {
        return numLifecycles;
    }

    /* @param cyclesToAdd add this many cycles to the accumulated count too */
    public void incrementNumLifecycles (long cyclesToAdd) {
        accumCycleCount += cyclesToAdd;
        ++numLifecycles;
    }

    public String getName () {
        return "Capacitor";
    }

    public void interruptServiced (int vector) {
    }

    public int getPowerMode() {
        return powerMode;
    }

    public void disable () {
        suppressVoltagePrinting = true;
        suppressEnergyPrinting = true;
        enabled = false;
    }

    public boolean isEnabled () {
        return enabled;
    }
}
