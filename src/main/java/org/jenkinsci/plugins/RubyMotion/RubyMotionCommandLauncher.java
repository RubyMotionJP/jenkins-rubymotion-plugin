package org.jenkinsci.plugins.RubyMotion;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

public class RubyMotionCommandLauncher {
    private final AbstractBuild build;
    private final Launcher launcher;
    private final BuildListener listener;

    public RubyMotionCommandLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
    }

    public String getProjectWorkspace() {
        return build.getProject().getWorkspace().toString();
    }
    
    public boolean exec(String command) {
        command = "bash -c \"" + command + "\"";
        try {
            int r = launcher.launch()
                .cmdAsSingleString(command)
                .envs(build.getEnvVars())
                .stdout(listener.getLogger())
                .pwd(getProjectWorkspace())
                .join();
            return r == 0;
        } catch (IOException e) {
            e.printStackTrace();
            listener.getLogger().println("IOException !");
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            listener.getLogger().println("InterruptedException !");
            return false;
        }
    }

    public boolean exec(String command, File outputFile) {
        command = "bash -c \"" + command + "\"";
        try {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            int r = launcher.launch()
                .cmdAsSingleString(command)
                .envs(build.getEnvVars())
                .stdout(outputStream)
                .pwd(getProjectWorkspace())
                .join();
            return r == 0;
        } catch (IOException e) {
            e.printStackTrace();
            listener.getLogger().println("IOException !");
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            listener.getLogger().println("InterruptedException !");
            return false;
        }
    }
 }
