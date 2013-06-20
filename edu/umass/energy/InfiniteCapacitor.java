package edu.umass.energy;
import java.lang.Math;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.IOUnit;

public class InfiniteCapacitor extends IOUnit {
    int powerMode;
    double lastModeChangeTime; // time at which power mode last changed
    double energyUsed; // total energy used so far
    public static final double voltage = 1.8;

    public InfiniteCapacitor (MSP430 msp) {
        super("infinite-capacitor", msp, msp.memory, Capacitor.voltageReaderAddress);
        powerMode = Capacitor.POWERMODE_ACTIVE;
        energyUsed = 0.0;
        lastModeChangeTime = 0.0;
    }

    public void setPowerMode (int newMode) {
        /* On every power mode change, calculate how much energy was used in the
         * previous mode for however much time it was in that mode. */
        double resistance = Capacitor.getResistance(powerMode, voltage);
        double power = (voltage * voltage) / resistance;
        double now = cpu.getTimeMillis();
        double timeInLastMode = now - lastModeChangeTime;
        energyUsed += power * (timeInLastMode * 1e3);

        lastModeChangeTime = now;
        powerMode = newMode;
    }

    public double getEnergyUsed () {
        return energyUsed;
    }

    public int read (int address, boolean word, long cycles) {
        return 0;
    }

    public void write (int address, int value, boolean word, long cycles) {
    }

    public void interruptServiced (int vector) {
    }
}
