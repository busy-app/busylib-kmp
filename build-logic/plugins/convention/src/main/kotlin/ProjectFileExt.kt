import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.Properties
import kotlin.io.path.reader

private fun File.takeIfIsLinkOrExist(): File? {
    if (exists() || Files.isSymbolicLink(toPath())) return this
    return null
}

internal fun Project.fileOrParentFile(name: String): File? {
    val parentOrNull = parent
    val projectFileOrNull = file(name).takeIfIsLinkOrExist()
    return when {
        projectFileOrNull != null -> projectFileOrNull
        parentOrNull != null -> parentOrNull.fileOrParentFile(name)
        else -> rootProject.file(name).takeIfIsLinkOrExist()
    }
}

internal fun Project.requireFileOrParentFile(name: String): File {
    return fileOrParentFile(name)
        ?: throw GradleException("No $name file found")
}

private fun File.defaultOrSymbolicReader(): InputStreamReader {
    val file = this
    val path = file.toPath()
    return if (Files.isSymbolicLink(path)) {
        Files.readSymbolicLink(path).reader()
    } else {
        file.reader()
    }
}

internal val Project.localProperties: Properties
    get() = Properties().apply {
        requireFileOrParentFile("local.properties")
            .defaultOrSymbolicReader()
            .run(::load)
    }

internal val Project.gradleProperties: Properties
    get() = Properties().apply {
        requireFileOrParentFile("gradle.properties")
            .defaultOrSymbolicReader()
            .run(::load)
    }
