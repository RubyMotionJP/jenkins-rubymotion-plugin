package org.jenkinsci.plugins.RubyMotion;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
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
    private final String deviceName;
    private final String simulatorVersion;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public RubyMotionBuilder(String platform, String rakeTask, String outputStyle, String outputFileName, 
                             boolean useBundler, boolean installCocoaPods, boolean needClean,
                             String deviceName, String simulatorVersion) {
        this.platform = platform;
        this.rakeTask = rakeTask;
        this.outputStyle = outputStyle;
        this.outputFileName = outputFileName;
        this.useBundler = useBundler;
        this.installCocoaPods = installCocoaPods;
        this.needClean = needClean;
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

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        boolean result;
        RubyMotionCommandLauncher cmdLauncher = new RubyMotionCommandLauncher(build, launcher, listener);

	cmdLauncher.exec("rm -rf " + outputFileName + " .jenkins-error");

        if (useBundler) {
            String cmds = "bundle install";
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

        if (needClean) {
            String cmds = "";
            if (useBundler) {
                cmds = "bundle exec ";
            }
            cmds = cmds + "rake clean";
            result = cmdLauncher.exec(cmds);
            if (!result) {
                return false;
            }
        }

        if (platform.equals("ios")) {
            return execiOS(cmdLauncher);
        }
        else if (platform.equals("osx")) {
            return execOSX(cmdLauncher);
        }
        return false;
    }

    private boolean execOSX(RubyMotionCommandLauncher cmdLauncher) {
        String cmds = "rake ";
        if (getUseBundler()) {
            cmds = "bundle exec rake ";
        }

        cmds = cmds + rakeTask;
        cmds = cmds + " output=" + outputStyle;

        String output = cmdLauncher.getProjectWorkspace() + "/" + outputFileName;
        File outputFile = new File(output);
        cmdLauncher.exec(cmds, outputFile);
        return checkFinishedWithoutCrash(cmdLauncher);
    }

    private boolean execiOS(RubyMotionCommandLauncher cmdLauncher) {
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

        cmdLauncher.exec(cmds);
        return checkFinishedWithoutCrash(cmdLauncher);
    }

    private boolean checkFinishedWithoutCrash(RubyMotionCommandLauncher cmdLauncher) {
        String lastLine = null;
        try {
            FilePath fp = cmdLauncher.build.getWorkspace().child(outputFileName);
            String output = fp.readToString().trim();
            int index = output.lastIndexOf("\n");
            if (index != -1 && index != output.length()) {
                lastLine = output.substring(index + 1);
            }
        } catch (IOException e) {
            return false;
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
            items.add("OS X Platform", "osx");
            return items;
        }

        public ListBoxModel doFillRakeTaskItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("spec", "spec");
            return items;
        }

        public ListBoxModel doFillRetinaItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("off",           "false");
            items.add("on",            "true");
            items.add("on (3.5-inch)", "3.5");
            items.add("on (4-inch)",   "4");
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

