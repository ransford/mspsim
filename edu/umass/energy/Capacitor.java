package edu.umass.energy;
import java.lang.Math;
import se.sics.mspsim.core.ADC12;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MSP430Constants;
import se.sics.mspsim.core.StopExecutionException;

public class Capacitor extends PowerSupply {
    private double capacitance;
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
    private long printCounter = 0;
    private boolean suppressVoltagePrinting = false;
    private boolean suppressEnergyPrinting = false;
    public EnergyTrace energyTrace;

    private boolean enabled = true;

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

    // private double defaultResistance = MSP430_RESISTANCE_ACTIVE;
    private double eTracePrevV = 0.0;

    public static double resistanceADCRead (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9824
        return (3346.2 * voltage) + 1040.8;
    }

    public static double resistanceActive (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9958
        return (4010.6 * voltage) + 803.53;
    }

    public static double resistanceFlashWrite (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9998
        return (4747.8 * voltage) + 152.98;
    }

    public static double resistanceLPM0 (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9999
        return (18232 * voltage) + 1017.9;
    }

    public static double resistanceLPM1 (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.9999
        return (18230 * voltage) + 1014.3;
    }

    public static double resistanceLPM2 (double voltage) {
        // Linear regression calculated from spreadsheet; R^2 = 0.999
        return (48202 * voltage) + 6229.5;
    }

    public static double resistanceLPM3 (double voltage) {
        // Cubic regression calculated from spreadsheet; R^2 = 0.994
        return
            (-66859 * Math.pow(voltage, 3))
            + (532699 * Math.pow(voltage, 2))
            - (979608 * voltage)
            + 1e6;
    }

    public static double resistanceLPM4 (double voltage) {
        // Cubic regression calculated from spreadsheet; R^2 = 0.9944
        return
            (-128337 * Math.pow(voltage, 3))
            + (1e6 * Math.pow(voltage, 2))
            - (2e6 * voltage)
            + 3e6;
    }

    /**
      * @param msp The MSP430 whose livelihood depends on this Capacitor
      * @param C Capacitance in farads, e.g. 10e-6 == 10uF.
      * @param initialVoltage Initial voltage in volts, e.g. 4.5 == 4.5V.
      */
    public Capacitor (double C, double initVoltage,
            double inputVoltageDividerFactor,
            double inputVoltageReferenceVoltage) {
        super("capacitor");
        capacitance = C;
        effectiveMaxVoltage =
            (inputVoltageDividerFactor * inputVoltageReferenceVoltage);
        setPowerMode(POWERMODE_ACTIVE);
        setInitialVoltage(initVoltage);
    }

    public void setEnergyTrace (EnergyTrace enTrace) {
        this.energyTrace = enTrace;
        this.traceDriven = (enTrace != null);
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
    
    public double recover() {
        DeadTimer deadt = cpu.getDeadTimer();
        int oldPowerMode = this.powerMode;
        
        deadt.setCurrentTime(cpu.getTimeMillis() + PowerSupply.getVoltageAddress);
        this.setClockSource(deadt);
        setPowerMode(MSP430Constants.MODE_LPM4);

        // consume a voltage trace until we can resurrect the CPU
        while (voltage < cpu.resurrectionThreshold) {
            updateVoltage();
        }
        setA(voltage, false);

        setPowerMode(oldPowerMode);
        setClockSource(cpu);
        double totalTimeToConvalesce = deadt.getTimeMillis();
        return totalTimeToConvalesce;
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
    /*
    public void dockEnergy (double amount) {
        setVoltage(Math.sqrt(2.0 * ((0.5 * capacitance * voltage * voltage) - amount) / capacitance));
    }
    */

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
        if ((address != getVoltageAddress) || !word) {
            return 0;
        }

        double vfrac = voltage / effectiveMaxVoltage;
        int scaled_amt = (int)(Math.round(vfrac * 65536));

        setPowerMode(POWERMODE_ADC);
        cpu.cycles += ADC12.ADC12_CYCLES;
        updateVoltage();
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

    public static double getResistance (int powmode, double V) {
        switch (powmode) {
            case POWERMODE_ACTIVE: return Capacitor.resistanceActive(V);
            case POWERMODE_LPM0:   return Capacitor.resistanceLPM0(V);
            case POWERMODE_LPM1:   return Capacitor.resistanceLPM1(V);
            case POWERMODE_LPM2:   return Capacitor.resistanceLPM2(V);
            case POWERMODE_LPM3:   return Capacitor.resistanceLPM3(V);
            case POWERMODE_LPM4:   return Capacitor.resistanceLPM4(V);
            case POWERMODE_FLWRI:  return Capacitor.resistanceFlashWrite(V);
            case POWERMODE_ADC:    return Capacitor.resistanceADCRead(V);
        }
        throw new RuntimeException("Unknown power mode " + powmode);
    }

    /**
     * Returns the load resistance to be used in voltage calculations.
     */
    public double getResistance () {
        return Capacitor.getResistance(this.powerMode, this.voltage);
    }

    public void toggleHush () {
        suppressVoltagePrinting = !suppressVoltagePrinting;
        suppressEnergyPrinting = !suppressEnergyPrinting;
    }

    /**
     * P = E/t = VI, so E=tVI.
     * @return true if it's time to die, false otherwise
     */
    public void updateVoltage () throws StopExecutionException {
        if (!enabled) return;
        boolean inCheckpoint = cpu.inCheckpoint;
        double RC = getResistance() * capacitance;
        boolean dead = (clockSource instanceof DeadTimer);
        double currentTime = clockSource.getTimeMillis();
        if (!dead)
            currentTime += cpu.getOffset();
        double dt = currentTime - lastATime;
        boolean shouldSetVoltage = true;
        boolean shouldDie = false;

        // Give the energy trace provider a crack at the voltage first.
        if(energyTrace != null) {
            double V_applied = energyTrace.getVoltage(currentTime);
            if (V_applied > eTracePrevV) { // trace wants to add energy
                double V_initial = voltage; // V = Q/C
                double V_final = V_initial +
                    V_applied * (1 - Math.exp((-dt) / 800.0*RC));
                setVoltage(V_final);
                // setVoltage(V_applied); // possibly large jump!

                double deltaV = V_final - V_initial;
                System.err.println("Added: " + deltaV + "V");
                shouldSetVoltage = false;
            }
            eTracePrevV = V_applied;
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
                System.err.format("<dead>%1.3f,%1.3f%n", currentTime, getVoltage());
            return;
        }

        if (!suppressVoltagePrinting && (printCounter++ % 10 == 0))
            System.err.format("%s%1.3f,%1.3f%n", (inCheckpoint ? "<chk>" : ""),
                    (clockSource.getTimeMillis() + cpu.getOffset()),
                    voltage);

        if (voltage <= cpu.deathThreshold)
            throw new StopExecutionException(cpu.readRegister(15),
                    "Voltage is too low");
    }

    /**
     * @return the energy stored in this Capacitor, in joules
     */
    public double getEnergy () {
        return (0.5 * capacitance * voltage * voltage);
    }

    public double getEffectiveMaxVoltage () {
        return effectiveMaxVoltage;
    }

    public String getName () {
        return "Capacitor";
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
