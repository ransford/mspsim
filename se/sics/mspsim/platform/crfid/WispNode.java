/**
 * Copyright (c) 2009, University of Massachusetts.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of MSPSim.
 *
 * $Id$
 *
 * -----------------------------------------------------------------
 *
 * WispNode
 *
 * Author  : ransford@cs.umass.edu
 * Created : Fri Nov 13 10:27:50 2009
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.platform.crfid;
import java.io.IOException;

import se.sics.mspsim.config.MSP430f2132Config;
import se.sics.mspsim.core.ADC10;
import se.sics.mspsim.core.ADCInput;
import se.sics.mspsim.core.IOUnit;
// import se.sics.mspsim.extutil.jfreechart.DataChart;
// import se.sics.mspsim.extutil.jfreechart.DataSourceSampler;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.util.ArgumentManager;
import java.lang.Math;
import edu.umass.energy.Capacitor;

public class WispNode extends GenericNode implements ADCInput {
  public static final boolean DEBUG = false;
  public static final int MODE_MAX = 0; // ?

  public WispNode () {
      super("WISP", new MSP430f2132Config(),
              new Capacitor(10e-6 /* 10 uF */,
                  4.5 /* 4.5 V */,
                  3.0 /* voltage divider factor */,
                  2.5 /* voltage check ref voltage */));
  }

  public boolean getDebug () {
      return cpu.getDebug();
  }

  public void setDebug (boolean debug) {
      cpu.setDebug(debug);
  }

  public void setupNode () {
      IOUnit adc = cpu.getIOUnit("ADC10");
      if (adc instanceof ADC10) {
        ((ADC10) adc).setADCInput(this);
      }
  }

  public String getName () {
      return "WISP";
  }

  public int getModeMax () {
      return MODE_MAX;
  }

  public static void main (String[] args) throws IOException {
      ArgumentManager config = new ArgumentManager();
      config.handleArguments(args);

      WispNode node = new WispNode();
      node.setupArgs(config);
  }

  public void exitCleanup () {
      System.err.println("exitCleanup() called");
      System.err.println("Final voltage: " + cpu.getPowerSupply().getVoltage());
      System.err.println("Number of lifecycles: " +
              cpu.getPowerSupply().getNumLifecycles());
  }

  public int nextData() {
	  // sinusoid w/ 500 Hz frequency
	  double time_ms = cpu.getTimeMillis();
	  double sinval = Math.sin(time_ms * Math.PI);
	  return (int)(sinval * 127); // sinusoid within range of byte
  }
}
