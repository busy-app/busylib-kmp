package net.flipper.detekt.rules

import dev.detekt.api.Rule
import dev.detekt.test.lint
import dev.detekt.test.utils.compileForTest
import java.nio.file.Files

fun Rule.lintWithPath(code: String, relativePath: String): Int {
    val tempDir = Files.createTempDirectory("detekt-test")
    try {
        val file = tempDir.resolve(relativePath)
        Files.createDirectories(file.parent)
        Files.writeString(file, code)
        val ktFile = compileForTest(file)
        return lint(ktFile).size
    } finally {
        tempDir.toFile().deleteRecursively()
    }
}
