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
            substitute(module("moe.styx:styx-common-jvm"))
                .using(project(":styx-common"))
        }
    }
}

val localDB = file("../Styx-DB")
if (localDB.isDirectory) {
    includeBuild(localDB) {
        dependencySubstitution {
            substitute(module("moe.styx:styx-db"))
                .using(project(":lib"))
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
