package org.cloudbus.cloudsim.examples.power.thermal;

import org.cloudbus.cloudsim.examples.power.planetlab.NonPowerAware;
import org.cloudbus.cloudsim.examples.power.util.ThermalPlanetLabRunner;
import org.cloudbus.cloudsim.util.ResourceLoader;

import java.io.IOException;
import java.util.Objects;

public class ThrMu {

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void main(String[] args) throws IOException {
        String inputFolder = ResourceLoader.getResourcePath(NonPowerAware.class,"workload/planetlab");
        if(Objects.isNull(inputFolder)){
            inputFolder = "";
        }
        String outputFolder = "output";
        String workload = "20110303_100s"; // PlanetLab workload
        String vmAllocationPolicy = "thermal_thr"; // Static Threshold (THR) VM allocation policy
        String vmSelectionPolicy = "mu"; // Minimum Utilization (MU) VM selection policy
        double staticUtilizationThreshold = 0.8;

        double[] temperatureThresholds = { 340.0, 343.0, 346.0, 348.0, 350.0, 353.0, 356.0, 360.0, 363.0, 366.0, 370.0 };
        for(double staticTemperatureThreshold : temperatureThresholds ) {
            new ThermalPlanetLabRunner(
                true,
                true,
                inputFolder,
                outputFolder,
                workload,
                vmAllocationPolicy,
                vmSelectionPolicy,
                staticUtilizationThreshold,
                staticTemperatureThreshold);
        }
    }

}
