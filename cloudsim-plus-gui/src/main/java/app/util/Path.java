package app.util;

import lombok.*;

public class Path {

    // The @Getter methods are needed in order to access
    // the variables from Velocity Templates
    public static class Web {
        @Getter public static final String RESULTS = "/results/";
        @Getter public static final String ONE_RESULT = "/results/:name/";
        @Getter public static final String SETUP = "/setup/";
    }

    public static class Template {
        public static final String NOT_FOUND = "/velocity/notFound.vm";
        public final static String SETUP = "/velocity/setup/setup.vm";
        public final static String RESULTS_ALL = "/velocity/result/all.vm";
        public static final String RESULTS_ONE = "/velocity/result/one.vm";
    }

}
