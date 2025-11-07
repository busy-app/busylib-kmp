rootProject.name = "BusyLibKmp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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
        mavenCentral()
    }
}

include(
    ":components:principal:api",
    ":components:principal:impl",
)

include(
    ":components:di",
    ":components:buildkonfig",
    ":components:log",
    ":components:ktx"
)

include(":instances:multiplatform")
include(":instances:shadow")

include(
    ":components:bridge:config:api",
    ":components:bridge:config:impl",
    ":components:bridge:connectionbuilder:api",
    ":components:bridge:connectionbuilder:impl",
    ":components:bridge:device:common:api",
    ":components:bridge:device:bsb:api",
    ":components:bridge:device:bsb:impl",
    ":components:bridge:device:firstpair:connection:api",
    ":components:bridge:device:firstpair:connection:impl",
    ":components:bridge:orchestrator:api",
    ":components:bridge:orchestrator:impl",
    ":components:bridge:service:api",
    ":components:bridge:service:impl",
    ":components:bridge:transport:mock:api",
    ":components:bridge:transport:mock:impl",
    ":components:bridge:transport:common:api",
    ":components:bridge:transport:common:impl",
    ":components:bridge:transport:ble:api",
    ":components:bridge:transport:ble:impl",
    ":components:bridge:transportconfigbuilder:api",
    ":components:bridge:transportconfigbuilder:impl",
    ":components:bridge:feature:common:api",
    ":components:bridge:feature:provider:api",
    ":components:bridge:feature:provider:impl",
    ":components:bridge:feature:rpc:api",
    ":components:bridge:feature:rpc:impl",
    ":components:bridge:feature:info:api",
    ":components:bridge:feature:info:impl",
    ":components:bridge:feature:wifi:api",
    ":components:bridge:feature:wifi:impl",
    ":components:bridge:feature:link:api",
    ":components:bridge:feature:link:impl",
    ":components:bridge:feature:sync:impl",
    ":components:bridge:feature:battery:api",
    ":components:bridge:feature:battery:impl",
    ":components:bridge:feature:screen-streaming:api",
    ":components:bridge:feature:screen-streaming:impl",
    ":components:bridge:feature:screen-streaming:compose",
    ":components:bridge:feature:firmware-update:api",
    ":components:bridge:feature:firmware-update:impl",
)
