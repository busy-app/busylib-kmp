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
    ":principal:api",
    ":principal:impl",
)
include(":di")
include(":log")
include(":ktx")
include(
    ":device:bridge:config:api",
    ":device:bridge:config:impl",
    ":device:bridge:connectionbuilder:api",
    ":device:bridge:connectionbuilder:impl",
    ":device:bridge:device:common:api",
    ":device:bridge:device:bsb:api",
    ":device:bridge:device:bsb:impl",
    ":device:bridge:orchestrator:api",
    ":device:bridge:orchestrator:impl",
    ":device:bridge:service:api",
    ":device:bridge:service:impl",
    ":device:bridge:transport:mock:api",
    ":device:bridge:transport:mock:impl",
    ":device:bridge:transport:common:api",
    ":device:bridge:transport:common:impl",
    ":device:bridge:transport:ble:api",
    ":device:bridge:transport:ble:impl",
    ":device:bridge:transportconfigbuilder:api",
    ":device:bridge:transportconfigbuilder:impl",
    ":device:bridge:feature:common:api",
    ":device:bridge:feature:provider:api",
    ":device:bridge:feature:provider:impl",
    ":device:bridge:feature:rpc:api",
    ":device:bridge:feature:rpc:impl",
    ":device:bridge:feature:info:api",
    ":device:bridge:feature:info:impl",
    ":device:bridge:feature:wifi:api",
    ":device:bridge:feature:wifi:impl",
    ":device:bridge:feature:link:api",
    ":device:bridge:feature:link:impl",
    ":device:bridge:feature:sync:impl",
    ":device:bridge:feature:battery:api",
    ":device:bridge:feature:battery:impl",
    ":device:bridge:feature:screen-streaming:api",
    ":device:bridge:feature:screen-streaming:impl",
    ":device:bridge:feature:screen-streaming:compose",
    ":device:bridge:feature:firmware-update:api",
    ":device:bridge:feature:firmware-update:impl",
)