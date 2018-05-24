package org.cloudbus.cloudsim.examples.power.thermal;

import org.cloudbus.cloudsim.examples.power.planetlab.NonPowerAware;
import org.cloudbus.cloudsim.examples.power.util.ThermalPlanetLabRunner;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerCustomIncremental;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G5Xeon3075;
import org.cloudbus.cloudsim.util.ResourceLoader;

import java.io.IOException;
import java.util.Objects;

public class Thr {
    public static String run(int numberOfHosts,
                             int numberOfVMs,
                             double utilizationThreshold,
                             double temperatureThreshold,
                             double underUtilizationThreshold,
                             double weightUtilization,
                             PowerModel powerModel) throws IOException
    {
        String inputFolder = ResourceLoader.getResourcePath(NonPowerAware.class,"workload/planetlab");
        if(Objects.isNull(inputFolder)){
            inputFolder = "";
        }

        String outputFolder = "output";
        String workload = "20110303"; // PlanetLab workload
        String vmAllocationPolicy = "thermal_thr"; // Static Threshold (THR) VM allocation policy
        String vmSelectionPolicy = "mu"; // Minimum Utilization (MU) VM selection policy

        ThermalPlanetLabRunner runner = new ThermalPlanetLabRunner(
            true,
            true,
            inputFolder,
            outputFolder,
            workload,
            vmAllocationPolicy,
            vmSelectionPolicy,
            utilizationThreshold,
            temperatureThreshold,
            underUtilizationThreshold,
            weightUtilization,
            powerModel,
            numberOfHosts,
            numberOfVMs
        );

        String[] powerModelPaths = powerModel.toString().split("\\.");
        String powerModelName = powerModelPaths[powerModelPaths.length-1].split("@")[0];

        String experimentName = runner.getExperimentName(
            String.valueOf(numberOfHosts), String.valueOf(numberOfVMs), String.valueOf(utilizationThreshold),
            String.valueOf(temperatureThreshold), String.valueOf(underUtilizationThreshold),
            String.valueOf(weightUtilization), powerModelName
        ) + ".txt";

        return experimentName;
    }

    public static void main(String[] args) throws IOException {
        double[] temperatureThresholds = { 340.0, 343.0, 346.0, 348.0, 350.0, 353.0, 356.0, 360.0, 363.0, 366.0, 370.0 };
        for(double staticTemperatureThreshold : temperatureThresholds ) {
            run(120, 100, 0.8, staticTemperatureThreshold, 0.3, 1.0, new PowerModelSpecPowerCustomIncremental());
        }
    }
}
