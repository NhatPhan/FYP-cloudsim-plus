/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.power.models;

import java.util.Random;

/**
 * The power model of an IBM server x3550 (2 x [Xeon X5670 2933 MHz, 6 cores], 12GB).<br/>
 * <a href="http://www.spec.org/power_ssj2008/results/res2010q2/power_ssj2008-20100315-00239.html">
 * http://www.spec.org/power_ssj2008/results/res2010q2/power_ssj2008-20100315-00239.html</a>
 *
 * <br/>If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:<br/>
 *
 * <ul>
 * <li><a href="http://dx.doi.org/10.1002/cpe.1867">Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012</a>
 * </ul>
 *
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class PowerModelSpecPowerCustomIncremental extends PowerModelSpecPower {
    /**
     * The power consumption according to the utilization percentage.
     * @see #getPowerData(int)
     */
    private final double[] power = { randomDouble(0, 50), randomDouble(50, 60), randomDouble(60, 70), randomDouble(70, 80),
        randomDouble(80, 90), randomDouble(90, 100), randomDouble(100, 110), randomDouble(110, 120), randomDouble(120, 130),
        randomDouble(130, 140), randomDouble(140, 150) };

    @Override
    public double getPowerData(int index) {
        return power[index];
    }

    public static double randomDouble(double min, double max) {
        Random r = new Random();
        return min + (max - min) * r.nextDouble();
    }
}
