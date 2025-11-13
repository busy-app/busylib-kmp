import net.flipper.Config.requireProjectInfo
import net.flipper.Config.requirePublishInfo

plugins {
    id("com.vanniktech.maven.publish")
}

private val projectInfo = project.requireProjectInfo
private val publishInfo = project.requirePublishInfo

mavenPublishing {
    publishToMavenCentral(automaticRelease = false)
    coordinates(
        groupId = publishInfo.publishGroupId,
        artifactId = project.name,
        version = projectInfo.versionString
    )
    signAllPublications()
    pom {
        this.name.set(publishInfo.libraryName)
        this.description.set(projectInfo.description)
        this.url.set(publishInfo.gitHubUrl)
        licenses {
            license {
                this.name.set(publishInfo.license)
                this.distribution.set("repo")
                this.url.set("${publishInfo.gitHubUrl}/blob/master/LICENSE.md")
            }
        }

        developers {
            projectInfo.developersList.forEach { dev ->
                developer {
                    id.set(dev.id)
                    name.set(dev.name)
                    email.set(dev.email)
                }
            }
        }

        scm {
            this.connection.set(publishInfo.sshUrl)
            this.developerConnection.set(publishInfo.sshUrl)
            this.url.set(publishInfo.gitHubUrl)
        }
    }
}