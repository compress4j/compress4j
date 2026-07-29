package io.github.compress4j.semver

data class Commit(val hash: String, val subject: String, val body: String)

data class GitHistory(val previousVersion: String?, val commits: List<Commit>)

object ConventionalCommits {

    /** Kept in sync with the types of the generated `commit-msg` hook (`conventionalCommits { defaultTypes() }`). */
    val types =
        listOf("fix", "feat", "build", "chore", "ci", "docs", "perf", "refactor", "revert", "style", "test")

    private val subjectPattern =
        Regex("^(${types.joinToString("|")})(\\([^()]+\\))?(!)?: \\S.*$")

    private val breakingFooterPattern = Regex("^BREAKING[ -]CHANGE:", RegexOption.MULTILINE)

    fun isValidSubject(subject: String): Boolean = subjectPattern.matches(subject.trim())

    fun bumpOf(commit: Commit): SemverBump {
        val match = subjectPattern.matchEntire(commit.subject.trim()) ?: return SemverBump.NONE
        val breaking = match.groupValues[3] == "!" || breakingFooterPattern.containsMatchIn(commit.body)
        return when {
            breaking -> SemverBump.MAJOR
            match.groupValues[1] == "feat" -> SemverBump.MINOR
            match.groupValues[1] in setOf("fix", "perf") -> SemverBump.PATCH
            else -> SemverBump.NONE
        }
    }

    fun declaredBump(commits: List<Commit>): SemverBump = SemverBump.highestOf(commits.map(::bumpOf))

    fun invalidSubjects(commits: List<Commit>): List<Commit> = commits.filterNot { isValidSubject(it.subject) }

    fun explainConvention(): String =
        """
        |Commit subjects and pull request titles must follow Conventional Commits:
        |    <type>[(<scope>)][!]: <description>
        |Allowed types: ${types.joinToString(", ")}
        |A '!' suffix or a 'BREAKING CHANGE:' footer marks a breaking change.
        |Version impact: feat -> minor, fix/perf -> patch, '!'/BREAKING CHANGE -> major, anything else -> no release.
        """.trimMargin()
}
