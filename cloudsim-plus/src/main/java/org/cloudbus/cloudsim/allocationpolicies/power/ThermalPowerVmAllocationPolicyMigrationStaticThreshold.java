package org.cloudbus.cloudsim.allocationpolicies.power;

import org.cloudbus.cloudsim.hosts.power.PowerHost;
import org.cloudbus.cloudsim.selectionpolicies.power.PowerVmSelectionPolicy;

public class ThermalPowerVmAllocationPolicyMigrationStaticThreshold extends ThermalPowerVmAllocationPolicyMigrationAbstract
{
    private double overUtilizationThreshold = 0.9;
    private double overTemperatureThreshold = 300.1;

    public ThermalPowerVmAllocationPolicyMigrationStaticThreshold(
        PowerVmSelectionPolicy vmSelectionPolicy,
        double overUtilizationThreshold,
        double thresholdTemperature
    ) {
        super(vmSelectionPolicy);
        setOverUtilizationThreshold(overUtilizationThreshold);
        setThresholdTemperature(thresholdTemperature);
    }

    @Override
    public double getThresholdTemperature(PowerHost host) {
        return overTemperatureThreshold;
    }

    @Override
    public void setThresholdTemperature(double thresholdTemperature) {
        overTemperatureThreshold = thresholdTemperature;
    }

    @Override
    public double getOverUtilizationThreshold(PowerHost host) {
        return overUtilizationThreshold;
    }

    public final void setOverUtilizationThreshold(double overUtilizationThreshold) {
        this.overUtilizationThreshold = overUtilizationThreshold;
    }
}
