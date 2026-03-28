pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
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

rootProject.name = "CanvasLauncher"
include(":app")
include(":core:common")
include(":core:model")
include(":core:ui")
include(":core:database")
include(":core:packages")
include(":core:performance")
include(":core:settings")
include(":domain")
include(":feature:apps")
include(":feature:canvas")
include(":feature:launcher")
 
