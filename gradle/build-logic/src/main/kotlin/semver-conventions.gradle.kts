import io.github.compress4j.semver.CheckCommitMessagesTask
import io.github.compress4j.semver.ConventionalCommits
import io.github.compress4j.semver.GitHistoryValueSource
import io.github.compress4j.semver.ReleaseTagsValueSource
import io.github.compress4j.semver.SemverExtension
import io.github.compress4j.semver.SemverPlanTask
import io.github.compress4j.semver.parseGitHistory
import io.github.compress4j.semver.parseReleaseTags

plugins { id("me.champeau.gradle.japicmp") }

val semver = extensions.create<SemverExtension>("semver")

fun gitHistory(base: String?) = providers.of(GitHistoryValueSource::class) {
    parameters {
        projectDir.set(layout.projectDirectory.asFile)
        this.base.set(base.orEmpty())
    }
}

val historySinceLastRelease = gitHistory(null).map { parseGitHistory(it) }

semver.previousVersion.set(historySinceLastRelease.map { it.previousVersion.orEmpty() })
semver.declaredBump.set(historySinceLastRelease.map { ConventionalCommits.declaredBump(it.commits) })
semver.releaseVersions.set(
    providers.of(ReleaseTagsValueSource::class) {
        parameters { projectDir.set(layout.projectDirectory.asFile) }
    }.map { parseReleaseTags(it) }
)

tasks.register<SemverPlanTask>("semverPlan") {
    group = "release"
    description = "Prints the version the commits since the last release tag ask for."
    gitHistory.set(gitHistory(null))
    outputs.upToDateWhen { false }
}

tasks.register<SemverPlanTask>("writeSemverPlan") {
    group = "release"
    description = "Writes the release plan derived from the commits since the last release tag."
    gitHistory.set(gitHistory(null))
    planFile.set(layout.buildDirectory.file("semver/plan.properties"))
}

tasks.register<CheckCommitMessagesTask>("checkCommitMessages") {
    group = "verification"
    description = "Checks that commit messages follow Conventional Commits."
    gitHistory.set(gitHistory(providers.gradleProperty("commits.base").orNull))
    pullRequestTitle.set(providers.gradleProperty("pr.title"))
}
