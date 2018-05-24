package app;

import app.power.PowerDao;
import app.setup.*;
import app.result.*;
import app.util.*;
import static spark.Spark.*;
import static spark.debug.DebugScreen.*;

public class Application {

    // Declare dependencies
    public static PowerDao powerDao;

    public static void main(String[] args) {

        // Instantiate your dependencies
        powerDao = new PowerDao();

        // Configure Spark
        port(4567);
        staticFiles.location("/public");
        staticFiles.expireTime(600L);
        enableDebugScreen();

        // Set up before-filters (called before each get/post)
        before("*",                  Filters.addTrailingSlashes);
        before("*",                  Filters.handleLocaleChange);

        // Set up routes
        get(Path.Web.SETUP,          SetupController.serveSetupPage);
        post(Path.Web.SETUP,         SetupController.runSimulation);
        get(Path.Web.RESULTS,          ResultController.fetchAllResults);
        get(Path.Web.ONE_RESULT,       ResultController.fetchOneResult);
        get("*",                     ViewUtil.notFound);

        //Set up after-filters (called after each get/post)
        after("*",                   Filters.addGzipHeader);

    }

}
