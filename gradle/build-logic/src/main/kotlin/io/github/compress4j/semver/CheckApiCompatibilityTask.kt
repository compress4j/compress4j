package io.github.compress4j.semver

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

private const val MAX_REPORTED_CHANGES = 25

abstract class CheckApiCompatibilityTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val reports: ConfigurableFileCollection

    @get:Input
    abstract val baselineVersion: Property<String>

    @get:Input
    abstract val declaredBump: Property<SemverBump>

    @TaskAction
    fun check() {
        if (baselineVersion.get().isEmpty()) {
            logger.warn("No release tag reachable, skipping the API compatibility check")
            return
        }
        val changes = reports.files.flatMap { JapicmpReport.changes(it) }
        val requiredBump = SemverBump.highestOf(changes.map { it.requiredBump })
        val declared = declaredBump.get()

        logger.lifecycle(
            "API changes against ${baselineVersion.get()} require a $requiredBump bump, commits declare $declared"
        )
        if (declared >= requiredBump) return

        val offenders = changes.filter { it.requiredBump > declared }
            .sortedByDescending { it.requiredBump }
            .take(MAX_REPORTED_CHANGES)
        throw GradleException(
            """
            |The public API changed more than the commit messages declare.
            |Required bump : $requiredBump
            |Declared bump : $declared
            |
            |${offenders.joinToString("\n|") { "  $it" }}
            |
            |Report: ${reports.files.joinToString(", ") { it.path.replace(".xml", ".html") }}
            |
            |${ConventionalCommits.explainConvention()}
            """.trimMargin()
        )
    }
}
