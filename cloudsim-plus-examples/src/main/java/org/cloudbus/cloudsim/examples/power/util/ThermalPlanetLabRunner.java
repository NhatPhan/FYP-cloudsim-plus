package org.cloudbus.cloudsim.examples.power.util;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelPlanetLab;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The example runner for the PlanetLab workload.
 *
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 *
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 *
 * @author Anton Beloglazov
 * @since Jan 5, 2012
 */
public final class ThermalPlanetLabRunner extends ThermalRunnerAbstract {

    private final static int NUMBER_OF_HOSTS = 50;
    private final static int MAX_NUMBER_OF_WORLOAD_FILES_TO_READ = Integer.MAX_VALUE;

    /**
     * Instantiates a new thermal planet lab runner.
     */
    public ThermalPlanetLabRunner(
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
        super(
            enableOutput,
            outputToFile,
            inputFolder,
            outputFolder,
            workload,
            vmAllocationPolicy,
            vmSelectionPolicy,
            utilizationThreshold,
            temperatureThreshold);
    }

    /**
     * Creates the cloudlet list planet lab.
     */
    public static List<Cloudlet> createCloudletListPlanetLab(DatacenterBroker broker, String inputFolderName)
    {
        final long fileSize = 300;
        final long outputSize = 300;

        final File inputFolder = new File(inputFolderName);
        final File[] files = inputFolder.listFiles();

        if(Objects.isNull(files)) {
            return new ArrayList<>();
        }

        final int filesToRead = Math.min(files.length, MAX_NUMBER_OF_WORLOAD_FILES_TO_READ);
        final List<Cloudlet> list = new ArrayList<>(filesToRead);

        for (int i = 0; i < filesToRead; i++) {
            try {
                UtilizationModel utilizationModelCPU =
                    new UtilizationModelPlanetLab(files[i].getAbsolutePath(), Constants.SCHEDULING_INTERVAL);

                CloudletSimple cloudlet = new CloudletSimple(i, Constants.CLOUDLET_LENGTH, Constants.CLOUDLET_PES);

                cloudlet.setFileSize(fileSize).setOutputSize(outputSize).setUtilizationModelCpu(utilizationModelCPU);

                //cloudlet.setVm(i);

                list.add(cloudlet);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }

        return list;
    }

    @Override
    protected void init(final String inputFolder) {
        try {
            super.init(inputFolder);
            broker = Helper.createBroker(getSimulation());
            cloudletList = createCloudletListPlanetLab(broker, inputFolder);
            vmList = Helper.createVmList(broker, cloudletList.size());
            hostList = Helper.createHostList(NUMBER_OF_HOSTS);
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
    }

}
