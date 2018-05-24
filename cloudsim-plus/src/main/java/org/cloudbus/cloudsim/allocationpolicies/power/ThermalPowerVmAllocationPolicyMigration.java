package org.cloudbus.cloudsim.allocationpolicies.power;

import org.cloudbus.cloudsim.hosts.power.PowerHost;

/**
 * An interface to be implemented by VM allocation policy for power and thermal aware VMs
 * that detects Power Host over CPU utilization or over threshold temperature.
 */
public interface ThermalPowerVmAllocationPolicyMigration extends PowerVmAllocationPolicyMigration {
    /**
     * Checks if host is over threshold temperature
     */
    boolean isHostOverThresholdTemperature(PowerHost host);

    /**
     * Gets the host CPU threshold temperature to detect overheat.
     */
    double getThresholdTemperature(PowerHost host);

    /**
     * Sets the host CPU threshold temperature to detect over heat.
     */
    void setThresholdTemperature(double thresholdTemperature);
}

