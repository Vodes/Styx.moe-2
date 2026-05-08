pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "Styx.moe"

val localCommon = file("../Styx-Common")
if (localCommon.isDirectory) {
    includeBuild(localCommon) {
        dependencySubstitution {
            substitute(module("moe.styx:styx-common"))
                .using(project(":styx-common"))
        }
    }
}

val localDownloader = file("../Styx-Downloader")
if (localDownloader.isDirectory) {
    includeBuild(localDownloader) {
        dependencySubstitution {
            substitute(module("moe.styx:styx-downloader"))
                .using(project(":"))
        }
    }
}
