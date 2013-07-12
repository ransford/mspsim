package edu.umass.energy;

import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MSP430;

public class PowerSupplyIO extends IOUnit {
    public int read(int address, boolean word, long cycles) {
        return 0;
    }

    public void write(int address, int value, boolean word, long cycles) {
        return;
    }
    
    public PowerSupplyIO(String id, MSP430 msp) {
        super(id, msp, msp.memory, PowerSupply.getVoltageAddress);
    }
    
    public void interruptServiced (int vector) {
        return;
    }
}
