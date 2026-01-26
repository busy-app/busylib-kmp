rootProject.name = "BusyLibKmp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven {
            url = uri("https://reposilite.flipp.dev/releases")
        }
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://reposilite.flipp.dev/releases")
        }
        mavenLocal()
    }
}

include(
    ":components:core:di",
    ":components:core:buildkonfig",
    ":components:core:log",
    ":components:core:ktx",
    ":components:core:wrapper",

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
    ":components:bridge:transport:tcp:common",
    ":components:bridge:transport:tcp:lan:api",
    ":components:bridge:transport:tcp:lan:impl",
    ":components:bridge:transport:tcp:cloud:api",
    ":components:bridge:transport:tcp:cloud:impl",
    ":components:bridge:transport:combined:api",
    ":components:bridge:transport:combined:impl",
    ":components:bridge:transport:combined:noop",
    ":components:bridge:transport:common:api",
    ":components:bridge:transport:common:impl",

    ":components:bridge:transport:ble:api",
    ":components:bridge:transport:ble:impl",
    ":components:bridge:transport:ble:common",
    ":components:bridge:transport:ble:http",

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
    ":components:bridge:feature:ble:api",
    ":components:bridge:feature:ble:impl",
    ":components:bridge:feature:settings:api",
    ":components:bridge:feature:settings:impl",
    ":components:bridge:feature:link:api",
    ":components:bridge:feature:link:impl",
    ":components:bridge:feature:sync:impl",
    ":components:bridge:feature:battery:api",
    ":components:bridge:feature:battery:impl",
    ":components:bridge:feature:screen-streaming:api",
    ":components:bridge:feature:screen-streaming:impl",
    ":components:bridge:feature:firmware-update:api",
    ":components:bridge:feature:firmware-update:impl",
    ":components:bridge:feature:events:api",
    ":components:bridge:feature:events:impl",
    ":components:bridge:feature:oncall:api",
    ":components:bridge:feature:oncall:impl",

    ":components:principal:api",
    ":components:cloud:api",

    ":entrypoint",
    ":sample"
)
