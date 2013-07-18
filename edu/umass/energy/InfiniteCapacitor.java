package edu.umass.energy;
import java.lang.Math;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.StopExecutionException;

public class InfiniteCapacitor extends PowerSupply {
    double lastModeChangeTime; // time at which power mode last changed
    double energyUsed; // total energy used so far
    private double voltage = 3.0;

    public InfiniteCapacitor (double voltage) {
        super("infinite-capacitor");
        this.voltage = voltage;
    }

    public void initialize () {
        energyUsed = 0.0;
        lastModeChangeTime = 0.0;
        setPowerMode(PowerSupply.POWERMODE_ACTIVE);
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
        super.setPowerMode(newMode);
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
    
    public void reset () {
    }
    
    public double getVoltage () {
        return voltage;
    }

    public String getStatus () {
        return "InfiniteCapacitor: " + getVoltage() + " V; energy used=" +
                getEnergyUsed() + " J";
    }
    
    public void updateVoltage () throws StopExecutionException {
        // no-op (voltage never changes)
        return;
    }
    
    public double recover () {
        return 0;
    }
}
