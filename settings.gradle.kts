pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google\\.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SmartFiles"

include(":app")
include(":core:common")
include(":core:model")
include(":core:database")
include(":core:datastore")
include(":core:filesystem")
include(":core:ml")
include(":core:designsystem")
include(":domain")
include(":data")
include(":feature")
