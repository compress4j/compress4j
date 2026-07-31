package io.github.compress4j.semver

import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

/** Emits the reachable `v*` tags, newest first, one per line. */
abstract class ReleaseTagsValueSource : ValueSource<String, ReleaseTagsValueSource.Params> {

    interface Params : ValueSourceParameters {
        val projectDir: Property<File>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val tags = git("tag", "--list", "v*", "--merged", "HEAD", "--sort=-v:refname")
        val headTag = git("describe", "--tags", "--match", "v*", "--exact-match", "HEAD").trim()
        return tags.lines().filter { it.trim() != headTag }.joinToString("\n")
    }

    private fun git(vararg args: String): String {
        val output = ByteArrayOutputStream()
        val result = execOperations.exec {
            commandLine(listOf("git") + args)
            workingDir = parameters.projectDir.get()
            standardOutput = output
            errorOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }
        return if (result.exitValue == 0) output.toString(Charsets.UTF_8.name()) else ""
    }
}

fun parseReleaseTags(raw: String): List<String> =
    raw.lines().map { it.trim().removePrefix("v") }.filter { it.isNotEmpty() }
