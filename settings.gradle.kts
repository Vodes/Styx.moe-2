pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "Styx.moe"

val localDownloader = file("../Styx-Downloader")
if (localDownloader.isDirectory) {
    includeBuild(localDownloader) {
        dependencySubstitution {
            substitute(module("moe.styx:styx-downloader"))
                .using(project(":"))
        }
    }
}
