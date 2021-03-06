package org.symphonyoss.maven.enforcer;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.symphonyoss.maven.enforcer.utils.UnZip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class RequireFileInArtifactRule implements EnforcerRule {

    private String fileName;
    private String match;

    private UnZip unzip = new UnZip();

    public void execute(EnforcerRuleHelper enforcerRuleHelper) throws EnforcerRuleException {
        String artifactId = null;
        String packaging = null;
        String version = null;
        String target = null;

        try {
            target = (String) enforcerRuleHelper.evaluate( "${project.build.directory}" );
            artifactId = (String) enforcerRuleHelper.evaluate( "${project.artifactId}" );
            packaging = (String) enforcerRuleHelper.evaluate( "${project.packaging}" );
            version = (String) enforcerRuleHelper.evaluate( "${project.version}" );
        } catch (ExpressionEvaluationException e) {
            e.printStackTrace();
        }

        String filePath = target + "/" + artifactId + "-" + version + "." + packaging;

        boolean applyRule = packaging.equalsIgnoreCase("jar") || packaging.equalsIgnoreCase("war");

        if (applyRule) {
            if (!new File(filePath).exists()) {
                enforcerRuleHelper.getLog().warn("RequireFileInArtifactRule is skipping since file doesn't exist - " + filePath);
            } else {
                unzipAndCheck(filePath);
            }
        } else {
            enforcerRuleHelper.getLog().debug("RequireFileInArtifactRule is skipping execution since packaging ("+packaging+") is not a JAR or a WAR");
        }
    }

    private boolean unzipAndCheck(String filePath) throws EnforcerRuleException {
        File tmpFolder = null;
        try {
            tmpFolder = File.createTempFile("maven-enforcer-fileinjar-rule", String.valueOf(System.currentTimeMillis()));
            tmpFolder.delete();
            tmpFolder.mkdir();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create temp folder");
        }

        try {
            if (this.fileName != null) {
                boolean fileFound = unzip.extractFileFromJar(filePath, tmpFolder, this.fileName);
                File extractedFile = null;
                if (fileFound) {
                    extractedFile = new File(tmpFolder, this.fileName);

                    if (this.match != null) {
                        Scanner scanner = null;
                        try {
                            scanner = new Scanner(extractedFile);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException("Cannot create file scanner on folder", e);
                        }
                        while (scanner.hasNextLine()) {
                            final String lineFromFile = scanner.nextLine();

                            if (lineFromFile.contains(this.match)) {
                                return true;
                            }
                        }
                        throw new EnforcerRuleException("RequireFileInArtifactRule is unable to find match for '" + this.match + "' within file " + extractedFile + " and file " + filePath);
                    }
                } else {
                    throw new EnforcerRuleException("RequireFileInArtifactRule is unable to find file " + this.fileName + " within file " + filePath);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot unzip "+filePath,e);
        }
        return false;
    }

    public boolean isCacheable() {
        return false;
    }

    // Not used if cachable=false
    public String getCacheId() {
        return ""+"."+this.fileName+"."+this.match;
    }

    // Not used if cachable=false
    public boolean isResultValid( EnforcerRule arg0 ) {
        return false;
    }
}
