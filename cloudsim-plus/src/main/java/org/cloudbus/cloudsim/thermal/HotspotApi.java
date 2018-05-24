package org.cloudbus.cloudsim.thermal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class HotspotApi {
    public static void writeToPtrace(double power) {
        try {
            String ptrace_content = "Core\r\n" + power;
            String write_command = "echo '" + ptrace_content + "' > xeon3040.ptrace";

            String[] cmd = {"/bin/sh", "-c", "cd ../FYP-hotspot; " + write_command};

            Runtime.getRuntime().exec(cmd);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double getTemperature(double power) {
        try {
            writeToPtrace(power);

            //Bash Command
            String[] cmd = {"/bin/sh", "-c", "cd ../FYP-hotspot; ./hotspot -c xeon3040.config -f xeon3040.flp -p xeon3040.ptrace -steady_file gcc.steady"};

            // create a process and execute
            Process proc = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

            String s = stdInput.readLine();     // Read to skip the headers
            double temperature = 0.0;

            while ((s = stdInput.readLine()) != null) {
                temperature = Math.max(temperature, Double.parseDouble(s.split("\\s+")[1]));
                return temperature;
            }

            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

            System.out.println(temperature);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
