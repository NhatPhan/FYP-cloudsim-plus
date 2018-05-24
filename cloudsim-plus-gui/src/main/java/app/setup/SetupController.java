package app.setup;
import app.util.*;
import static app.Application.powerDao;

import org.cloudbus.cloudsim.examples.power.thermal.Thr;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower;
import spark.*;
import java.util.*;

public class SetupController {
    public static Route serveSetupPage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        model.put("powerModels", powerDao.getAllPowerModels());
        return ViewUtil.render(request, model, Path.Template.SETUP);
    };

    public static Route runSimulation = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();

        int numHosts = Integer.parseInt(request.queryParams("numHosts"));
        int numVMs = Integer.parseInt(request.queryParams("numVMs"));
        PowerModelSpecPower powerModel = powerDao.getPowerModelByName(request.queryParams("powerModel")).getModel();
        double utilizationThreshold = Double.parseDouble(request.queryParams("utilizationThreshold"));
        double temperatureThreshold = Double.parseDouble(request.queryParams("temperatureThreshold"));
        double underUtilizationThreshold = Double.parseDouble(request.queryParams("underUtilizationThreshold"));
        double weightUtilization = Double.parseDouble(request.queryParams("weightUtilization"));

        String experimentName = Thr.run(numHosts, numVMs, utilizationThreshold, temperatureThreshold,
            underUtilizationThreshold, weightUtilization, powerModel);

        response.redirect("/results/" + experimentName);
        return null;
    };
}

