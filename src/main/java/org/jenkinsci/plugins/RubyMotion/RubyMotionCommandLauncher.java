package org.jenkinsci.plugins.RubyMotion;
import hudson.Launcher;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;

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
        return build.getWorkspace().toString();
    }

    public FilePath getWorkspaceFilePath(String fileName) {
        return build.getWorkspace().child(fileName);
    }

    public boolean exec(String command) {
        command = "bash -c \"" + command + "\"";
        try {
            int r = launcher.launch()
                .cmdAsSingleString(command)
                .envs(build.getEnvironment(listener))
                .stdout(listener.getLogger())
                .pwd(getProjectWorkspace())
                .join();
            return r == 0;
        }
        catch (IOException e) {
            e.printStackTrace();
            listener.getLogger().println("IOException !");
            return false;
        }
        catch (InterruptedException e) {
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
                .envs(build.getEnvironment(listener))
                .stdout(outputStream)
                .stderr(listener.getLogger())
                .pwd(getProjectWorkspace())
                .join();
            return r == 0;
        }
        catch (IOException e) {
            e.printStackTrace();
            listener.getLogger().println("IOException !");
            return false;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            listener.getLogger().println("InterruptedException !");
            return false;
        }
    }

    public void printLog(String string) {
        listener.getLogger().println(string);
    }
 }
