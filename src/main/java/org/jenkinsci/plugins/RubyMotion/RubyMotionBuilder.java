package org.jenkinsci.plugins.RubyMotion;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.*;

import org.jenkinsci.plugins.RubyMotion.RubyMotionCommandLauncher;

public class RubyMotionBuilder extends Builder {

    private final String platform;
    private final String rakeTask;
    private final String outputStyle;
    private final String outputFileName;
    private final boolean useBundler;
    private final boolean installCocoaPods;
    private final boolean needClean;
    private final boolean outputResult;
    private final String deviceName;
    private final String simulatorVersion;

    RubyMotionCommandLauncher cmdLauncher = null;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public RubyMotionBuilder(String platform, String rakeTask, String outputStyle, String outputFileName,
                             boolean useBundler, boolean installCocoaPods, boolean needClean, boolean outputResult,
                             String deviceName, String simulatorVersion) {
        this.platform = platform;
        this.rakeTask = rakeTask;
        this.outputStyle = outputStyle;
        this.outputFileName = outputFileName;
        this.useBundler = useBundler;
        this.installCocoaPods = installCocoaPods;
        this.needClean = needClean;
        this.outputResult = outputResult;
        this.deviceName = deviceName;
        this.simulatorVersion = simulatorVersion;
    }

    public String getPlatform() {
        return platform;
    }

    public String getRakeTask() {
        return rakeTask;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getOutputStyle() {
        return outputStyle;
    }

    public String getSimulatorVersion() {
        return simulatorVersion;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public boolean getUseBundler() {
        return useBundler;
    }

    public boolean getInstallCocoaPods() {
        return installCocoaPods;
    }

    public boolean getNeedClean() {
        return needClean;
    }

    public boolean getOutputResult() {
        return outputResult;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        boolean result;
        cmdLauncher = new RubyMotionCommandLauncher(build, launcher, listener);

        cmdLauncher.removeFile(outputFileName);
        cmdLauncher.removeFile(".jenkins-error");

        if (useBundler) {
            String cmds = "bundle install";
            result = cmdLauncher.exec(cmds);
            if (!result) {
                return false;
            }
        }

        if (needClean) {
            String cmds = "";
            if (useBundler) {
                cmds = "bundle exec ";
            }
            cmds = cmds + "rake clean:all";
            result = cmdLauncher.exec(cmds);
            if (!result) {
                return false;
            }
        }

        if (installCocoaPods) {
            String cmds = "";
            if (useBundler) {
                cmds = "bundle exec ";
            }
            cmds = cmds + "rake pod:install";
            result = cmdLauncher.exec(cmds);
            if (!result) {
                return false;
            }
        }

        boolean success = false;
        if (platform.equals("ios") || platform.equals("tvos")) {
            success = execiOS();
        }
        else if (platform.equals("osx")) {
            success = execOSX();
        }

        if (outputResult) {
            printResult();
        }

        boolean noCrashed = checkFinishedWithoutCrash();
        if (success == false) {
            if (noCrashed) {
                build.setResult(Result.UNSTABLE);
            }
            else {
                build.setResult(Result.FAILURE);
                printError();
            }
        }
        return true;
    }

    private boolean execOSX() {
        String cmds = "rake ";
        if (getUseBundler()) {
            cmds = "bundle exec rake ";
        }

        cmds = cmds + rakeTask;
        cmds = cmds + " output=" + outputStyle;

        OutputStream outputStream;
        try {
            FilePath fp = cmdLauncher.getWorkspaceFilePath(outputFileName);
            outputStream = fp.write();
        }
        catch (Exception e) {
            return false;
        }
        return cmdLauncher.exec(cmds, outputStream);
    }

    private boolean execiOS() {
        String cmds = "rake ";
        if (getUseBundler()) {
            cmds = "bundle exec rake ";
        }

        cmds = cmds + rakeTask;
        if (deviceName != null && deviceName.length() > 0) {
            cmds = cmds + " device_name='" + deviceName + "'";
        }
        if (simulatorVersion != null && simulatorVersion.length() > 0) {
            cmds = cmds + " target=" + simulatorVersion;
        }
        cmds = cmds + " output=" + outputStyle;

        String output = cmdLauncher.getProjectWorkspace() + "/" + outputFileName;
        String error  = cmdLauncher.getProjectWorkspace() + "/.jenkins-error";
        cmds = cmds + " SIM_STDOUT_PATH='" + output + "' SIM_STDERR_PATH='" + error + "'";

        return cmdLauncher.exec(cmds);
    }

    private String readResult(String path) {
        String result = null;
        try {
            FilePath fp = cmdLauncher.getWorkspaceFilePath(path);
            if (fp.exists()) {
                result = fp.readToString().trim();
            }
            return result;
    }
        catch (Exception e) {
            return result;
        }
    }

    private void printResult() {
        String result = readResult(outputFileName);
        if (result == null) {
            return;
        }
        cmdLauncher.printLog(result + "\n");
    }

    private void printError() {
        String error = readResult(".jenkins-error");
        if (error == null) {
            return;
        }
        cmdLauncher.printLog(error + "\n");
    }

    private boolean checkFinishedWithoutCrash() {
        String result = readResult(outputFileName);
        if (result == null) {
            return false;
        }

        String lastLine = null;
        int index = result.lastIndexOf("\n");
        if (index != -1 && index != result.length()) {
            lastLine = result.substring(index + 1);
        }
        if (lastLine == null) {
            return false;
        }
        return lastLine.matches("^# \\d+ tests.+");
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "RubyMotion";
        }

        public ListBoxModel doFillPlatformItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("iOS Platform",  "ios");
            items.add("tvOS Platform", "tvos");
            items.add("OS X Platform", "osx");
            return items;
        }

        public ListBoxModel doFillRakeTaskItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("spec", "spec");
            return items;
        }

        public ListBoxModel doFillOutputStyleItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("tap", "tap");
            // items.add("spec_dox", "spec_dox");
            // items.add("fast", "fast");
            // items.add("test_unit", "test_unit");
            // items.add("knock", "knock");
            // items.add("colorized", "colorized");
            return items;
        }
    }
}
