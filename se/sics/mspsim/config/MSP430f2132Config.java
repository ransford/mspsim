package se.sics.mspsim.config;

import java.util.ArrayList;
import se.sics.mspsim.core.DMA;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.InterruptMultiplexer;
import se.sics.mspsim.core.MSP430Config;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.Multiplier;
import se.sics.mspsim.core.Timer;
import se.sics.mspsim.core.USART;

public class MSP430f2132Config extends MSP430Config {
  /* offset from beginning of interrupt table, since the beginning of the
   * interrupt table is either 0xFFC0 or 0xFFE0, depending on how you
   * interpret the MSP430F2132 datasheet. */
  int ivStart = 0xFFE0; // or 0xFFC0?
  int o = 0;            // or 16?

  public MSP430f2132Config () {
    maxInterruptVector = (0xFFFE - ivStart) / 2; // 15 or 31

    TimerConfig timer0_A3 = new TimerConfig(
        9+o,   // interrupt vector for timer CCR0
        8+o,   // interrupt vector for other CCRx's
        3,     // number of CCRs
        0x160, // address of timer control register
        Timer.TIMER_Ax149, // XXX input map
        "Timer0_A3" // name
        );
    TimerConfig timer1_A2 = new TimerConfig(
        13+o,    // interrupt vector for timer CCR0
        12+o,    // interrupt vector for other CCRx's
        2,     // number of CCRs
        0x180, // address of timer control register
        Timer.TIMER_Ax149, // XXX input map
        "Timer1_A2" // name
        );
    timerConfig = new TimerConfig[] {timer0_A3, timer1_A2};

    infoMemConfig(0x1000, 128 * 2);
    mainFlashConfig(0xe000, 8 * 1024);
    ramConfig(0x200, 512);
  }

  public int setup (MSP430Core cpu, ArrayList<IOUnit> ioUnits) {
    // XXX no USARTs in this chip.  can we do USCI?

    Multiplier mp = new Multiplier(cpu, cpu.memory, 0);
    for (int i = 0x130, n = 0x13f; i < n; i++) {
      cpu.memOut[i] = mp;
      cpu.memIn[i] = mp;
    }

    // no DMA

    // IO ports.  Ports 1 and 2 can generate interrupts
    ioUnits.add(new IOPort(cpu, 1, 2+o, cpu.memory, 0x20)); // port 1
    ioUnits.add(new IOPort(cpu, 2, 3+o, cpu.memory, 0x28)); // port 2
    for (int i = 0, n = 8; i < n; i++) {
      cpu.memOut[0x20 + i] = io1;
      cpu.memOut[0x28 + i] = io2;
      cpu.memIn[0x20 + i] = io1;
      cpu.memIn[0x28 + i] = io2;
    }

    // XXX ports 3 and 4 cannot generate interrupts
    for (int i = 0, n = 2; i < n; i++) {
      IOPort p = new IOPort(cpu, (3 + i), 0, cpu.memory, 0x18 + i * 4);
      ioUnits.add(p);
      cpu.memOut[0x18 + i * 4] = p;
      cpu.memOut[0x19 + i * 4] = p;
      cpu.memOut[0x1a + i * 4] = p;
      cpu.memOut[0x1b + i * 4] = p;
      cpu.memIn[0x18 + i * 4] = p;
      cpu.memIn[0x19 + i * 4] = p;
      cpu.memIn[0x1a + i * 4] = p;
      cpu.memIn[0x1b + i * 4] = p;
    }

    return 3 + 6; // XXX why?
  }
}
