package io.github.compress4j.semver

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class SemverPlanTask : DefaultTask() {

    @get:Input
    abstract val gitHistory: Property<String>

    @get:Optional
    @get:OutputFile
    abstract val planFile: RegularFileProperty

    @TaskAction
    fun plan() {
        val history = parseGitHistory(gitHistory.get())
        val bump = ConventionalCommits.declaredBump(history.commits)
        val previousVersion = history.previousVersion
        val next = nextVersion(previousVersion ?: "0.0.0", bump)
        val release = bump != SemverBump.NONE

        ConventionalCommits.invalidSubjects(history.commits).forEach {
            logger.warn("Ignoring non conventional commit ${it.hash.take(8)}: ${it.subject}")
        }

        logger.lifecycle(
            """
            |Previous version : ${previousVersion ?: "none"}
            |Commits          : ${history.commits.size}
            |Declared bump    : $bump
            |Next version     : $next
            |Release          : $release
            """.trimMargin()
        )

        if (planFile.isPresent) {
            planFile.get().asFile.apply { parentFile.mkdirs() }.writeText(
                """
                |previousVersion=${previousVersion ?: ""}
                |bump=${bump.name.lowercase()}
                |nextVersion=$next
                |release=$release
                |
                """.trimMargin()
            )
        }
    }
}
