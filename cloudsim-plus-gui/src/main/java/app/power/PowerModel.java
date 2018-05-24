package app.power;

public class PowerModel {
    public String name;
    public org.cloudbus.cloudsim.power.models.PowerModelSpecPower model;

    public PowerModel(org.cloudbus.cloudsim.power.models.PowerModelSpecPower model) {
        this.model = model;
        String[] names = this.model.toString().split("\\.");
        this.name = names[names.length-1].split("@")[0];
    }

    public String getName() {
        return name;
    }

    public org.cloudbus.cloudsim.power.models.PowerModelSpecPower getModel() {
        return model;
    }
}
