package app.power;

import com.google.common.collect.ImmutableList;
import org.cloudbus.cloudsim.power.models.*;

import java.util.List;

public class PowerDao {
    private final List<PowerModel> powerModels = ImmutableList.of(
        new PowerModel(new PowerModelSpecPowerHpProLiantMl110G5Xeon3075()),
        new PowerModel(new PowerModelSpecPowerHpProLiantMl110G4Xeon3040()),
        new PowerModel(new PowerModelSpecPowerHpProLiantMl110G3PentiumD930()),
        new PowerModel(new PowerModelSpecPowerIbmX3550XeonX5675()),
        new PowerModel(new PowerModelSpecPowerIbmX3550XeonX5670()),
        new PowerModel(new PowerModelSpecPowerIbmX3250XeonX3480()),
        new PowerModel(new PowerModelSpecPowerIbmX3250XeonX3470()),
        new PowerModel(new PowerModelSpecPowerCustomIncremental())
    );

    public Iterable<PowerModel> getAllPowerModels() {
        return powerModels;
    }

    public PowerModel getPowerModelByName(String name) {
        return powerModels.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }
}
