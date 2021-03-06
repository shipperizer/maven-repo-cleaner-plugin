package org.jenkinsci.plugins.mavenrepocleaner;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.AbstractMavenProject;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class MavenRepoCleanerPostBuildTask extends Recorder implements Serializable{
	
	private static String maven_repo = "maven-repositories"; 
	
    @DataBoundConstructor
    public MavenRepoCleanerPostBuildTask() {

    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        final long started = build.getTimeInMillis();
        FilePath.FileCallable<Collection<String>> cleanup =
            new FilePath.FileCallable<Collection<String>>() {
                public Collection<String> invoke(File repository, VirtualChannel channel) throws IOException, InterruptedException {
                    return new RepositoryCleaner(started).clean(repository);
                }
            };
        Collection<String> removed = null;
        String ex_no = null;
        try{    
        	ex_no = build.getEnvironment().get("EXECUTOR_NUMBER");
        	removed = build.getWorkspace().getParent().getParent().child(maven_repo).child(ex_no).act(cleanup);
        	listener.getLogger().println(build.getWorkspace().getParent().getParent().child(maven_repo).child(ex_no).getRemote());
        	if (removed.size() > 0) {
                listener.getLogger().println( removed.size() + " unused artifacts removed from private maven repository" );
            }
        	else {
        		listener.getLogger().println( "Nothing to delete" );
        	}
        } catch(Exception e){
        	e.printStackTrace(listener.error("Failed to delete"));
        	listener.getLogger().println("CRAP CRAP CRAP");
        }
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {  
        return BuildStepMonitor.NONE;
    }
    
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(MavenRepoCleanerPostBuildTask.class);
        }

        @Override
        public String getDisplayName() {
            return "Cleanup maven repository for unused artifacts";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType)
                    || AbstractMavenProject.class.isAssignableFrom(jobType);
        }
    }
}
