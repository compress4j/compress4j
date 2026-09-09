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
        val log = git(*(listOf("log", "--no-merges", LOG_FORMAT) + range).toTypedArray(), failOnError = true)
        return tag + "\n" + log
    }

    /** When the head itself is tagged the release it describes is the one being validated, not the baseline. */
    private fun previousReleaseTag(): String {
        val headIsTagged = git("describe", "--tags", "--match", "v*", "--exact-match", "HEAD").isNotBlank()
        val head = if (headIsTagged) "HEAD^" else "HEAD"
        return git("describe", "--tags", "--match", "v*", "--abbrev=0", head).trim()
    }

    /**
     * Runs `git`, tolerating a non-zero exit by returning an empty string - used for the `describe` probes above,
     * where "no matching tag" is an expected outcome, not a failure. Pass [failOnError] for invocations (like `git
     * log`) where an empty result must not be silently confused with "no commits".
     */
    private fun git(vararg args: String, failOnError: Boolean = false): String {
        val output = ByteArrayOutputStream()
        val errorOutput = ByteArrayOutputStream()
        val result = execOperations.exec {
            commandLine(listOf("git") + args)
            workingDir = parameters.projectDir.get()
            standardOutput = output
            this.errorOutput = errorOutput
            isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            check(!failOnError) {
                "git ${args.joinToString(" ")} failed with exit code ${result.exitValue}: " +
                    errorOutput.toString(Charsets.UTF_8.name())
            }
            return ""
        }
        return output.toString(Charsets.UTF_8.name())
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
