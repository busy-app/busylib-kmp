package net.flipper.property.model

data class PublishInfo(
    val libraryName: String,
    val description: String,
    val gitHubOrganization: String,
    val gitHubName: String,
    val license: String,
    val publishGroupId: String,
) {
    val gitHubUrl = "https://github.com/$gitHubOrganization/$gitHubName"
    val sshUrl = "scm:git:ssh://github.com/$gitHubOrganization/$gitHubName.git"
}
