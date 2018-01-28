package org.cloudbus.cloudsim.examples.power.util;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.power.*;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.power.PowerDatacenter;
import org.cloudbus.cloudsim.hosts.power.PowerHost;
import org.cloudbus.cloudsim.selectionpolicies.power.*;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.vms.Vm;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public abstract class ThermalRunnerAbstract {
    private static boolean enableOutput;
    protected static DatacenterBroker broker;
    protected static List<Cloudlet> cloudletList;
    protected static List<Vm> vmList;
    protected static List<PowerHost> hostList;
    private CloudSim simulation;

    private final long startTime;
    private long finishTime;

    public ThermalRunnerAbstract(
        boolean enableOutput,
        boolean outputToFile,
        String inputFolder,
        String outputFolder,
        String workload,
        String vmAllocationPolicy,
        String vmSelectionPolicy,
        double utilizationThreshold,
        double temperatureThreshold)
    {
        this.startTime = System.currentTimeMillis()/1000;
        initLogOutput(
            enableOutput, outputToFile, outputFolder, workload,
            vmAllocationPolicy, vmSelectionPolicy, utilizationThreshold, temperatureThreshold);

        init(inputFolder + "/" + workload);
        start(
            getExperimentName(
                workload, vmAllocationPolicy, vmSelectionPolicy,
                String.valueOf(utilizationThreshold), String.valueOf(temperatureThreshold)),
            outputFolder,
            getVmAllocationPolicy(vmAllocationPolicy, vmSelectionPolicy, utilizationThreshold, temperatureThreshold));
        this.finishTime = System.currentTimeMillis()/1000;
        System.out.printf("Total execution time: %.2f minutes\n", getActualExecutionTimeInMinutes());
    }

    /**
     * Inits the log output.
     */
    protected void initLogOutput(
        boolean enableOutput,
        boolean outputToFile,
        String outputFolder,
        String workload,
        String vmAllocationPolicy,
        String vmSelectionPolicy,
        double utilizationThreshold,
        double temperatureThreshold)
    {
        try{
            setEnableOutput(enableOutput);
            Log.setDisabled(!isEnableOutput());
            if (isEnableOutput() && outputToFile) {
                File folder = new File(outputFolder);
                if (!folder.exists()) {
                    folder.mkdir();
                }

                File folder2 = new File(outputFolder + "/log");
                if (!folder2.exists()) {
                    folder2.mkdir();
                }

                File file = new File(outputFolder + "/log/"
                    + getExperimentName(workload, vmAllocationPolicy, vmSelectionPolicy,
                    String.valueOf(utilizationThreshold), String.valueOf(temperatureThreshold)) + ".txt");
                file.createNewFile();
                Log.setOutput(new FileOutputStream(file));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Inits the simulation.
     */
    protected void init(final String inputFolder){
        this.simulation = new CloudSim();
    }

    /**
     * Starts the simulation.
     */
    protected void start(final String experimentName, final String outputFolder, VmAllocationPolicy vmAllocationPolicy) {
        System.out.println("Starting " + experimentName);

        try {
            Helper helper = new Helper(simulation, experimentName, Constants.OUTPUT_CSV, outputFolder);
            PowerDatacenter datacenter = (PowerDatacenter) helper.createDatacenter(
                PowerDatacenter.class,
                hostList,
                vmAllocationPolicy);

            datacenter.setMigrationsEnabled(true);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            simulation.terminateAt(Constants.SIMULATION_LIMIT);
            double lastClock = simulation.start();

            List<Cloudlet> newList = broker.getCloudletFinishedList();
            Log.printLine("Received " + newList.size() + " cloudlets");

            helper.printResults(datacenter, vmList, lastClock);
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }

        Log.printLine("Finished " + experimentName);
    }

    /**
     * Gets the experiment name.
     */
    protected String getExperimentName(String... args) {
        StringBuilder experimentName = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (args[i].isEmpty()) {
                continue;
            }
            if (i != 0) {
                experimentName.append("_");
            }
            experimentName.append(args[i]);
        }
        return experimentName.toString();
    }

    /**
     * Gets the vm allocation policy.
     */
    protected VmAllocationPolicy getVmAllocationPolicy(
        String vmAllocationPolicyName,
        String vmSelectionPolicyName,
        double utilizationThreshold,
        double temperatureThreshold)
    {
        VmAllocationPolicy vmAllocationPolicy = null;
        PowerVmSelectionPolicy vmSelectionPolicy = null;

        if (!vmSelectionPolicyName.isEmpty()) {
            vmSelectionPolicy = getVmSelectionPolicy(vmSelectionPolicyName);
        }

        switch (vmAllocationPolicyName) {
            case "thermal_thr":
                vmAllocationPolicy = new ThermalPowerVmAllocationPolicyMigrationStaticThreshold(
                    vmSelectionPolicy, utilizationThreshold, temperatureThreshold
                );
                break;
            default:
                System.out.println("Unknown VM allocation policy: " + vmAllocationPolicyName);
                System.exit(0);
        }
        return vmAllocationPolicy;
    }

    /**
     * Gets the vm selection policy.
     *
     * @param vmSelectionPolicyName the vm selection policy name
     * @return the vm selection policy
     */
    protected PowerVmSelectionPolicy getVmSelectionPolicy(String vmSelectionPolicyName) {
        PowerVmSelectionPolicy vmSelectionPolicy = null;
        switch (vmSelectionPolicyName) {
            case "mc":
                vmSelectionPolicy = new PowerVmSelectionPolicyMaximumCorrelation(
                    new PowerVmSelectionPolicyMinimumMigrationTime());
                break;
            case "mmt":
                vmSelectionPolicy = new PowerVmSelectionPolicyMinimumMigrationTime();
                break;
            case "mu":
                vmSelectionPolicy = new PowerVmSelectionPolicyMinimumUtilization();
                break;
            case "rs":
                vmSelectionPolicy = new PowerVmSelectionPolicyRandomSelection();
                break;
            default:
                System.out.println("Unknown VM selection policy: " + vmSelectionPolicyName);
                System.exit(0);
        }
        return vmSelectionPolicy;
    }

    /**
     * Sets the enable output.
     *
     * @param enableOutput the new enable output
     */
    public void setEnableOutput(boolean enableOutput) {
        ThermalRunnerAbstract.enableOutput = enableOutput;
    }

    /**
     * Checks if is enable output.
     *
     * @return true, if is enable output
     */
    public boolean isEnableOutput() {
        return enableOutput;
    }


    public CloudSim getSimulation() {
        return simulation;
    }

    /**
     * Gets the time the  experiment started executing.
     * It isn't the simulation start time, but the actual
     * computer time (in seconds).
     * @return
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Gets the time the  experiment spent executing.
     * It isn't the total simulation time, but the actual
     * time the experiment spent running (in seconds).
     * For instance, we can simulate a cloud scenario
     * for applications running in a time interval of 24 hours,
     * however, this simulation may take just few seconds or minutes
     * to run in CloudSim Plus. This is the time this function returns.
     * @return
     */
    public double getActualExecutionTimeInMinutes(){
        return (finishTime - startTime)/60;
    }
}
