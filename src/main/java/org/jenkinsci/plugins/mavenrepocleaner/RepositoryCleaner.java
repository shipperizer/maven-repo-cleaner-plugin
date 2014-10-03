package org.jenkinsci.plugins.mavenrepocleaner;

import hudson.os.PosixAPI;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.DirectoryWalker;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.index.artifact.M2GavCalculator;
import org.jruby.ext.posix.FileStat;

/**
 * Hello world!
 *
 */
public class RepositoryCleaner extends DirectoryWalker implements Serializable
{
    private M2GavCalculator gavCalculator = new M2GavCalculator();
    private long timeVar;
    private String root;
    private static final Logger LOGGER = Logger.getLogger(RepositoryCleaner.class.getName()); 
    
    public RepositoryCleaner(long timestamp) {
    	int days = 7;
        this.timeVar = (timestamp/ 1000)-(days*24*60*60);
        
    }

    public Collection<String> clean(File repository) throws IOException {
    	LOGGER.log(Level.WARNING, "CLEAN" + repository.toString() );
    	this.root = repository.getAbsolutePath();
        Collection<String> result = new ArrayList<String>();
        walk(repository, result);
        return result;
    }

    protected final void handleDirectoryStart(File directory, int depth, Collection results) throws IOException {
    	LOGGER.log(Level.WARNING, "HANDLE DIRECTORY START");
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) continue;
            String fileName = file.getName();

            if (fileName.endsWith(".sha1") || fileName.endsWith(".md5")) continue;

            String location = file.getAbsolutePath().substring(root.length());
            Gav gav = gavCalculator.pathToGav(location);
            if (gav == null) continue; // Not an artifact

            olderThan(file, gav, results);
        }

        if ( directory.listFiles(new MetadataFileFilter()).length == 0 ) {
            for (File file : directory.listFiles()) {
                file.delete();
            }
            directory.delete();
        }

    }

    private void olderThan(File file, Gav artifact, Collection<String> results) {
    	LOGGER.log(Level.WARNING, "OLDER THAN");
        FileStat fs = PosixAPI.get().lstat(file.getPath());
        long lastAccessTime = fs.atime();
        
        if (lastAccessTime < timeVar) {
            // This artifact hasn't been accessed during build
            clean(file, artifact, results);
        }
    }

    private void clean(File file, Gav artifact, Collection<String> results) {
    	File directory = file.getParentFile();
        String fineName = gavCalculator.calculateArtifactName(artifact);
        LOGGER.log(Level.WARNING, "CLEAN "+ fineName);
        new File(directory, fineName + ".md5").delete();
        new File(directory, fineName + ".sha1").delete();
        file.delete();
        results.add(gavCalculator.gavToPath(artifact));
    }

    private static class MetadataFileFilter implements FileFilter {

        private final List<String> metadata =
            Arrays.asList(new String[]
                    {"_maven.repositories", "maven-metadata.xml", "maven-metadata.xml.md5", "maven-metadata.xml.sha1"});

        public boolean accept(File file) {
            return !metadata.contains(file.getName());
        }
    }
}

