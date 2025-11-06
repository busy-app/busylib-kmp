rootProject.name = "BusyStatusBar"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

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
        mavenCentral()
    }
}

include(
    ":components:principal:api",
    ":components:principal:impl",
)
include(":components:di")
include(":components:log")
include(":components:ktx")
include(
    ":components:device:bridge:config:api",
    ":components:device:bridge:config:impl",
    ":components:device:bridge:connectionbuilder:api",
    ":components:device:bridge:connectionbuilder:impl",
    ":components:device:bridge:device:common:api",
    ":components:device:bridge:device:bsb:api",
    ":components:device:bridge:device:bsb:impl",
    ":components:device:bridge:orchestrator:api",
    ":components:device:bridge:orchestrator:impl",
    ":components:device:bridge:service:api",
    ":components:device:bridge:service:impl",
    ":components:device:bridge:transport:mock:api",
    ":components:device:bridge:transport:mock:impl",
    ":components:device:bridge:transport:common:api",
    ":components:device:bridge:transport:common:impl",
    ":components:device:bridge:transport:ble:api",
    ":components:device:bridge:transport:ble:impl",
    ":components:device:bridge:transportconfigbuilder:api",
    ":components:device:bridge:transportconfigbuilder:impl",
    ":components:device:bridge:feature:common:api",
    ":components:device:bridge:feature:provider:api",
    ":components:device:bridge:feature:provider:impl",
    ":components:device:bridge:feature:rpc:api",
    ":components:device:bridge:feature:rpc:impl",
    ":components:device:bridge:feature:info:api",
    ":components:device:bridge:feature:info:impl",
    ":components:device:bridge:feature:wifi:api",
    ":components:device:bridge:feature:wifi:impl",
    ":components:device:bridge:feature:link:api",
    ":components:device:bridge:feature:link:impl",
    ":components:device:bridge:feature:sync:impl",
    ":components:device:bridge:feature:battery:api",
    ":components:device:bridge:feature:battery:impl",
    ":components:device:bridge:feature:screen-streaming:api",
    ":components:device:bridge:feature:screen-streaming:impl",
    ":components:device:bridge:feature:screen-streaming:compose",
    ":components:device:bridge:feature:firmware-update:api",
    ":components:device:bridge:feature:firmware-update:impl",
)