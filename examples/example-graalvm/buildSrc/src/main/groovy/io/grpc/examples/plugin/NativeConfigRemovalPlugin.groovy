package io.grpc.examples.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.Exec

import java.nio.file.Path
import java.nio.file.Paths

interface NativeConfigRemovalPluginExtension {
    Property<String> getRepoUrl()
    Property<String> getGroup()
    Property<String> getBaseName()
    Property<String> getVersion()
}

// Plugin that removes problematic native image configuration in dependency JAR
class NativeConfigRemovalPlugin implements Plugin<Project> {

    void apply(Project project) {

        // Make the plugin configurable
        def extension = project.extensions.create('jarToRemoveNativeConfig', NativeConfigRemovalPluginExtension)
        extension.repoUrl.convention('https://repo1.maven.org/maven2')
        extension.group.convention('io.grpc')
        extension.baseName.convention('grpc-netty-shaded')
        extension.version.convention('1.60.0')

        def buildDir = project.buildDir.toString()

        project.task('downloadJar') {
            def remotePomLocation
            def remoteJarLocation
            def originalPom
            def originalJar

            project.afterEvaluate {
                def repoUrl = extension.repoUrl.get()
                def group = extension.group.get().replace('.', '/')
                def baseName = extension.baseName.get()
                def version = extension.version.get()
                remotePomLocation = "${repoUrl}/${group}/${baseName}/${version}/${baseName}-${version}.pom"
                remoteJarLocation = "${repoUrl}/${group}/${baseName}/${version}/${baseName}-${version}.jar"
                originalPom = getPom(buildDir, baseName, version)
                originalJar = getOriginalJar(buildDir, baseName, version)

                inputs.files(new File(remotePomLocation), new File(remoteJarLocation))
                outputs.files(originalPom, originalJar)
            }

            doLast {
                // Download POM
                URL pomUrl = new URL(remotePomLocation)
                originalPom.bytes = pomUrl.bytes
                println "Downloaded ${originalPom}"

                // Download JAR
                URL jarUrl = new URL(remoteJarLocation)
                originalJar.bytes = jarUrl.bytes
                println "Downloaded ${originalJar}"
            }
        }

        project.task(type: Jar, 'removeNativeConfig') {
            dependsOn 'downloadJar'

            def baseName
            def version

            project.afterEvaluate {
                baseName = extension.baseName.get()
                version = extension.version.get()

                // Remove the native image configuration in the original JAR
                from project.zipTree(getOriginalJar(buildDir, baseName, version)).matching {
                    exclude 'META-INF/native-image/**'
                }
                archiveBaseName.set(baseName)
                archiveVersion.set(version)
                destinationDirectory.set(getModifiedPath(buildDir).toFile())
                // Replace if exists
                duplicatesStrategy(DuplicatesStrategy.INCLUDE)
            }
        }

        project.task(type: Exec, 'createRepository') {
            dependsOn 'removeNativeConfig'

            def baseName
            def version
            def pom
            def modifiedJar
            def repoDir

            project.afterEvaluate {
                baseName = extension.baseName.get()
                version = extension.version.get()
                pom = getPom(buildDir, baseName, version)
                modifiedJar = getModifiedJar(buildDir, baseName, version)
                repoDir = new File(buildDir, 'repo')

                inputs.files(pom, modifiedJar)
                outputs.dir(repoDir)

                // Use maven to create a repository
                executable('mvn')
                args('install:install-file', "-Dfile=${modifiedJar}", "-DpomFile=${pom}", "-Dmaven.repo.local=${repoDir}")
            }

            doLast {
                if (!repoDir.exists()) {
                    repoDir.mkdirs()
                }
            }
        }
    }

    static Path getOriginalPath(String buildDir) {
        return Paths.get(buildDir, 'original')
    }

    static Path getModifiedPath(String buildDir) {
        return Paths.get(buildDir, 'modified')
    }

    static File getPom(String buildDir, String baseName, String version) {
        return getOriginalPath(buildDir).resolve("${baseName}-${version}.pom").toFile()
    }

    static File getOriginalJar(String buildDir, String baseName, String version) {
        return getOriginalPath(buildDir).resolve("${baseName}-${version}.jar").toFile()
    }

    static File getModifiedJar(String buildDir, String baseName, String version) {
        return getModifiedPath(buildDir).resolve("${baseName}-${version}.jar").toFile()
    }

}