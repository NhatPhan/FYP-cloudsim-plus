package app.result;

public class Result {
    public String fullPath;
    public String numHosts;
    public String numVMs;
    public String powerModel;
    public String utilizationThreshold;
    public String temperatureThreshold;
    public String underUtilizationThreshold;
    public String weightUtilization;

    public Result(String fullPath) {
        this.fullPath = fullPath;
        String[] parts = fullPath.split("_");
        this.numHosts = parts[0];
        this.numVMs = parts[1];
        this.utilizationThreshold = parts[2];
        this.temperatureThreshold = parts[3];
        this.underUtilizationThreshold = parts[4];
        this.weightUtilization = parts[5];
        this.powerModel = parts[6].split("\\.")[0];
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getNumHosts() {
        return numHosts;
    }

    public String getNumVMs() {
        return numVMs;
    }

    public String getPowerModel() {
        return powerModel;
    }

    public String getUtilizationThreshold() {
        return utilizationThreshold;
    }

    public String getTemperatureThreshold() {
        return temperatureThreshold;
    }

    public String getUnderUtilizationThreshold() {
        return underUtilizationThreshold;
    }

    public String getWeightUtilization() {
        return weightUtilization;
    }
}
