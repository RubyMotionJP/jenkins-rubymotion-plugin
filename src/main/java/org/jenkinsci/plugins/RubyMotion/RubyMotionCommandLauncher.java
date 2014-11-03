package org.jenkinsci.plugins.RubyMotion;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;

public class RubyMotionCommandLauncher {
    final AbstractBuild build;
    final Launcher launcher;
    final BuildListener listener;

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

    public boolean exec(String command, OutputStream outputStream) {
        command = "bash -c \"" + command + "\"";
        try {
            int r = launcher.launch()
                .cmdAsSingleString(command)
                .envs(build.getEnvVars())
                .stdout(outputStream)
                .stderr(listener.getLogger())
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

    public void printLog(String string) {
        listener.getLogger().println(string);
    }
 }
