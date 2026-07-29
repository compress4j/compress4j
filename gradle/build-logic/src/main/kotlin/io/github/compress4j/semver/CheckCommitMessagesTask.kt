package io.github.compress4j.semver

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

abstract class CheckCommitMessagesTask : DefaultTask() {

    @get:Input
    abstract val gitHistory: Property<String>

    /** Validated on top of the commits: squash merges turn the pull request title into the commit subject. */
    @get:Optional
    @get:Input
    abstract val pullRequestTitle: Property<String>

    @TaskAction
    fun check() {
        val commits = parseGitHistory(gitHistory.get()).commits
        val invalid = ConventionalCommits.invalidSubjects(commits).map { "${it.hash.take(8)} ${it.subject}" }
        val invalidTitle = pullRequestTitle.orNull
            ?.takeIf { it.isNotBlank() && !ConventionalCommits.isValidSubject(it) }
            ?.let { "pull request title: $it" }
        val failures = invalid + listOfNotNull(invalidTitle)

        if (failures.isEmpty()) {
            logger.lifecycle("All ${commits.size} commit message(s) follow Conventional Commits")
            return
        }
        throw GradleException(
            failures.joinToString(
                prefix = "Invalid commit message(s):\n  ",
                separator = "\n  ",
                postfix = "\n\n" + ConventionalCommits.explainConvention()
            )
        )
    }
}
