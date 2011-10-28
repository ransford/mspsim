package tests;
import edu.umass.energy.Capacitor;
import edu.umass.energy.InstructionEnergy;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.platform.wisp.WispNode;
import se.sics.mspsim.util.ComponentRegistry;

public class CapacitorDockTest {
    private static boolean withinTolerance (double value, double target,
            double tolerance) {
        return (value >= target-tolerance && value <= target+tolerance);
    }

    private static boolean testFlashWrite (Capacitor cap, int numWords,
            double expected, double tolerance) {
        double V;
        double time = 0.0001; //This needs to come from new measurements.

        cap.reset();
        if (!withinTolerance(cap.getVoltage(), 4.5, 0.01)) {
            System.err.println("CAP FAILED TO INITIALIZE TO 4.5V!");
            return false;
        }
        
        for (int i = 0; i < numWords; ++i) {
        	V = cap.getVoltage();
        	//E = VIt, I = V/R
        	double I = V / cap.resistanceFlashWrite(V);
        	double eUsed = V * I * (time);//in J
            cap.dockEnergy(eUsed); //Updates cap.voltage
            System.err.println(eUsed);
        }
        V = cap.getVoltage();
        System.err.println("Voltage after " + numWords +
                " flash writes: " + V);
        if (!withinTolerance(V, expected, tolerance)) {
            System.err.println(numWords + " words: " + V +
                    " is out of acceptable range " + (expected - tolerance) +
                    "-" + (expected + tolerance));
            return false;
        }
        return true;
    }
    
    public static void main (String[] args) {
    	ComponentRegistry registry = new ComponentRegistry();
    	MSP430 cpu = new MSP430(0, registry);
        Capacitor cap = new Capacitor(cpu,
                10e-6 /* capacitance, farads */,
                4.5 /* initial voltage, volts */,
                10.0 /* maximum voltage, volts */);
        
        
        //All of these voltage expectations are based on old data.
        //They should be based on the "LPM current" measurements numbers.
        /* 8 words: spreadsheet 417:421 */
        testFlashWrite(cap, 8, 4.258, 0.2);

        /* 16 words: spreadsheet 424:428 */
        testFlashWrite(cap, 16, 4.054, 0.2);

        /* 32 words: spreadsheet 431:435 */
        testFlashWrite(cap, 32, 3.672, 0.2);

        /* 64 words: spreadsheet 438:442 */
        testFlashWrite(cap, 64, 2.91, 0.2);

        /* 96 words: spreadsheet 445:449 */
        testFlashWrite(cap, 96, 2.47, 0.2);

        /* 128 words: spreadsheet 452:456 */
        testFlashWrite(cap, 128, 1.741, 0.2);
    }
}
