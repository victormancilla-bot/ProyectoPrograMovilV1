rootProject.name = "ProyectoPrograMovil"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        // Design System (edu.ucb.ds:shared) publicado dentro del repo; ver ds-repo/README.md
        maven {
            url = uri(settingsDir.resolve("ds-repo"))
            mavenContent { includeGroup("edu.ucb.ds") }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared")