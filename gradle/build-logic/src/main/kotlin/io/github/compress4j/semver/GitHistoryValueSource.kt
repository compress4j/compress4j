package io.github.compress4j.semver

import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

private const val RECORD_SEPARATOR = '\u001E'
private const val FIELD_SEPARATOR = '\u001F'
private const val LOG_FORMAT = "--format=%H%x1f%s%x1f%b%x1e"

/**
 * Emits the latest reachable `v*` tag on the first line, followed by the `git log` records of the commits that
 * followed it (or that followed [Params.base], when set).
 */
abstract class GitHistoryValueSource : ValueSource<String, GitHistoryValueSource.Params> {

    interface Params : ValueSourceParameters {
        val projectDir: Property<File>
        val base: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val tag = previousReleaseTag()
        val base = parameters.base.orNull?.takeIf { it.isNotBlank() } ?: tag.takeIf { it.isNotEmpty() }
        val range = base?.let { listOf("$it..HEAD") } ?: emptyList()
        return tag + "\n" + git(*(listOf("log", "--no-merges", LOG_FORMAT) + range).toTypedArray())
    }

    /** When the head itself is tagged the release it describes is the one being validated, not the baseline. */
    private fun previousReleaseTag(): String {
        val head = if (git("describe", "--tags", "--match", "v*", "--exact-match", "HEAD").isNotBlank()) "HEAD^" else "HEAD"
        return git("describe", "--tags", "--match", "v*", "--abbrev=0", head).trim()
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

fun parseGitHistory(raw: String): GitHistory {
    val tagAndLog = raw.split('\n', limit = 2)
    val previousVersion = tagAndLog[0].trim().removePrefix("v").ifBlank { null }
    val commits = tagAndLog.getOrElse(1) { "" }
        .split(RECORD_SEPARATOR)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { record ->
            val fields = record.split(FIELD_SEPARATOR)
            Commit(fields[0], fields.getOrElse(1) { "" }, fields.getOrElse(2) { "" })
        }
    return GitHistory(previousVersion, commits)
}
