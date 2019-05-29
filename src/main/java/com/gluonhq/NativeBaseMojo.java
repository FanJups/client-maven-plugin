/*
 * Copyright (c) 2019, Gluon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.gluonhq;

import com.gluonhq.omega.Config;
import org.apache.commons.exec.ProcessDestroyer;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public abstract class NativeBaseMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    MavenSession session;

    @Component
    BuildPluginManager pluginManager;

    @Parameter(readonly = true, required = true, defaultValue = "${basedir}")
    File basedir;

    @Parameter(property = "client.graalLibsPath")
    String graalLibsPath;

    @Parameter(property = "client.graalLibsVersion", defaultValue = "20.0.0-beta.01")
    String graalLibsVersion;

    @Parameter(property = "client.javaStaticSdkVersion", defaultValue = "13-ea+2")
    String javaStaticSdkVersion;

    @Parameter(property = "client.javafxStaticSdkVersion", defaultValue = "13-ea+1")
    String javafxStaticSdkVersion;

    @Parameter(property = "client.target", defaultValue = "host")
    String target;

    @Parameter(property = "client.backend", defaultValue = "lir")
    String backend;

    @Parameter(property = "client.bundlesList")
    List<String> bundlesList;

    @Parameter(property = "client.resourcesList")
    List<String> resourcesList;

    @Parameter(property = "client.reflectionList")
    List<String> reflectionList;

    @Parameter(property = "client.jniList")
    List<String> jniList;

    @Parameter(property = "client.delayInitList")
    List<String> delayInitList;

    @Parameter(property = "client.runtimeArgsList")
    List<String> runtimeArgsList;

    @Parameter(property = "client.releaseSymbolsList")
    List<String> releaseSymbolsList;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.directory}/client")
    File outputDir;

    @Parameter(property = "client.mainClass", required = true)
    String mainClass;

    @Parameter(property = "client.executable", defaultValue = "java")
    String executable;

    private ProcessDestroyer processDestroyer;

    Config clientConfig;

    public void execute() throws MojoExecutionException {
        configOmega();
    }

    private void configOmega() {
        clientConfig = new Config();
        clientConfig.setGraalLibsVersion(graalLibsVersion);
        clientConfig.setJavaStaticSdkVersion(javaStaticSdkVersion);
        clientConfig.setJavafxStaticSdkVersion(javafxStaticSdkVersion);
        clientConfig.setTarget(target.toLowerCase(Locale.ROOT));
        clientConfig.setBackend(backend.toLowerCase(Locale.ROOT));
        clientConfig.setBundlesList(bundlesList);
        clientConfig.setResourcesList(resourcesList);
        clientConfig.setDelayInitList(delayInitList);
        clientConfig.setJniList(jniList);
        clientConfig.setReflectionList(reflectionList);
        clientConfig.setRuntimeArgsList(runtimeArgsList);
        clientConfig.setReleaseSymbolsList(releaseSymbolsList);

        clientConfig.setMainClassName(mainClass);
        clientConfig.setAppName(project.getName());

        List<File> classPath = getCompileClasspathElements(project);
        clientConfig.setUseJavaFX(classPath.stream().anyMatch(f -> f.getName().contains("javafx")));
        clientConfig.setGraalLibsUserPath(graalLibsPath);
    }

    ProcessDestroyer getProcessDestroyer() {
        if (processDestroyer == null) {
            processDestroyer = new ShutdownHookProcessDestroyer();
        }
        return processDestroyer;
    }

    List<File> getCompileClasspathElements(MavenProject project) {
        List<File> list = project.getArtifacts().stream()
                .sorted((a1, a2) -> {
                    int compare = a1.compareTo(a2);
                    if (compare == 0) {
                        // give precedence to classifiers
                        return a1.hasClassifier() ? 1 : (a2.hasClassifier() ? -1 : 0);
                    }
                    return compare;
                })
                .map(Artifact::getFile)
                .collect(Collectors.toList());
        list.add(0, new File(project.getBuild().getOutputDirectory()));
        return list;
    }
}