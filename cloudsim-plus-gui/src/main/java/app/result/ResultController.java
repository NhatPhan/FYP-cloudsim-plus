package app.result;

import app.util.*;
import org.unix4j.Unix4j;
import spark.*;
import java.util.*;
import java.io.File;

public class ResultController {
    public static Route fetchAllResults = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List<Result> results = new ArrayList<>();

        File dir = new File("output/log");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                results.add(new Result(child.getName()));
            }
        }

        Iterable<Result> resultsIterable = results;
        model.put("results", resultsIterable);

        return ViewUtil.render(request, model, Path.Template.RESULTS_ALL);
    };

    public static Route fetchOneResult = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        List<String> summaryLines = new ArrayList<>();
        List<String> detailLines = new ArrayList<>();

        String fileName = request.params("name");

        File dir = new File("output/log");
        File file = null;

        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.getName().equals(fileName)) {
                    file = child;
                }
            }
        }

        if (file == null) {
            return ViewUtil.render(request, model, Path.Template.NOT_FOUND);
        }

        summaryLines.addAll(Unix4j.grep("Number of Hosts", file).toStringList());
        summaryLines.addAll(Unix4j.grep("Number of VMs", file).toStringList());
        summaryLines.addAll(Unix4j.grep("Total simulation time", file).toStringList());
        summaryLines.addAll(Unix4j.grep("Energy consumption", file).toStringList());
        summaryLines.addAll(Unix4j.grep("Number of VM migrations", file).toStringList());
        summaryLines.addAll(Unix4j.grep("Overall SLA violation", file).toStringList());
        summaryLines.addAll(Unix4j.grep("Number of host shutdowns", file).toStringList());

        List<String> cannotCreateVMs = Unix4j.grep("none of the required VMs could be created", file).toStringList();

        if (cannotCreateVMs.isEmpty()) {
            detailLines.add("<b>At Initialization:</b>");

            String searchPattern = "^0.00(?s)(.*)has been allocated to(?s)(.*)";
            for (String line : Unix4j.grep(searchPattern, file).toStringList()) {
                String[] lineParts = line.split(":");
                detailLines.add(lineParts[1]);
            }

            searchPattern = "^0.00: No suitable host found for";
            for (String line : Unix4j.grep(searchPattern, file).toStringList()) {
                String[] lineParts = line.split(":");
                detailLines.add(lineParts[1]);
            }

            detailLines.add("</br>");

            for (int i = 0; i <= 1435; i += 5) {
                String time = Integer.toString(i * 60) + ".10";
                detailLines.add("<b>At minute " + i + ":</b>");

                searchPattern = "^" + time + "(?s)(.*)has been created(?s)(.*)";
                for (String line : Unix4j.grep(searchPattern, file).toStringList()) {
                    String[] lineParts = line.split(":");
                    detailLines.add(lineParts[2]);
                }

                searchPattern = "^" + time + "(?s)(.*)failed in(?s)(.*)";
                for (String line : Unix4j.grep(searchPattern, file).toStringList()) {
                    String[] lineParts = line.split(":");
                    detailLines.add(lineParts[2]);
                }

                searchPattern = "^" + time + "(?s)(.*)has been allocated to(?s)(.*)";
                for (String line : Unix4j.grep(searchPattern, file).toStringList()) {
                    String[] lineParts = line.split(":");
                    detailLines.add(lineParts[1]);
                }

                searchPattern = "^" + time + ": No suitable host found for";
                for (String line : Unix4j.grep(searchPattern, file).toStringList()) {
                    String[] lineParts = line.split(":");
                    detailLines.add(lineParts[1]);
                }

                searchPattern = "^" + time + ": Number of";
                for (String line : Unix4j.grep(searchPattern, file).toStringList()) {
                    String[] lineParts = line.split(":");
                    detailLines.add(lineParts[1] + lineParts[2]);
                }

                searchPattern = "^" + time + ": Host ";
                for (String line : Unix4j.grep(searchPattern, file).toStringList()) {
                    String[] lineParts = line.split(":");
                    detailLines.add(lineParts[1] + lineParts[2]);
                }
                detailLines.add("</br>");

                searchPattern = "^" + time + "(?s)(.*)will be migrated to host(?s)(.*)";
                for (String line : Unix4j.grep(searchPattern, file).toStringList()) {
                    String[] lineParts = line.split(":");
                    detailLines.add(lineParts[1] + lineParts[2]);
                }
                detailLines.add("</br>");
            }
        }
        else {
            detailLines.addAll(cannotCreateVMs);
        }

        Iterable<String> summaryLinesIterable = summaryLines;
        Iterable<String> detailLinesIterable = detailLines;

        model.put("summaryLines", summaryLinesIterable);
        model.put("detailLines", detailLinesIterable);

        return ViewUtil.render(request, model, Path.Template.RESULTS_ONE);
    };
}
