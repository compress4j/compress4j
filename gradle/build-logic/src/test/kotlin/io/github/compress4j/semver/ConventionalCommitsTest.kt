package io.github.compress4j.semver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class ConventionalCommitsTest {

    private fun commit(subject: String, body: String = "") = Commit("0123456789abcdef", subject, body)

    @ParameterizedTest
    @ValueSource(
        strings = [
            "fix: a thing",
            "feat: a thing",
            "feat(scope): a thing",
            "feat(scope)!: a thing",
            "feat!: a thing",
            "chore(deps): bump commons-compress",
            "  fix: leading and trailing space is trimmed  ",
        ]
    )
    fun `accepts conventional subjects`(subject: String) {
        assertThat(ConventionalCommits.isValidSubject(subject)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "fix a thing",
            "nope: a thing",
            "fix:no space after the colon",
            "fix: ",
            "fix(): empty scope",
            "fix(nested(scope)): a thing",
            "Fix: capitalised type",
            "",
        ]
    )
    fun `rejects non conventional subjects`(subject: String) {
        assertThat(ConventionalCommits.isValidSubject(subject)).isFalse()
    }

    @ParameterizedTest
    @CsvSource(
        "fix: a thing,PATCH",
        "perf: a thing,PATCH",
        "feat: a thing,MINOR",
        "feat(scope): a thing,MINOR",
        "feat!: a thing,MAJOR",
        "fix!: a thing,MAJOR",
        "chore: a thing,NONE",
        "docs: a thing,NONE",
        "not a conventional subject,NONE",
    )
    fun `derives the bump from the subject`(subject: String, expected: SemverBump) {
        assertThat(ConventionalCommits.bumpOf(commit(subject))).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(strings = ["BREAKING CHANGE: the API moved", "BREAKING-CHANGE: the API moved"])
    fun `a breaking change footer forces a major bump`(footer: String) {
        assertThat(ConventionalCommits.bumpOf(commit("fix: a thing", "Some body\n\n$footer"))).isEqualTo(SemverBump.MAJOR)
    }

    @Test
    fun `a breaking change mentioned mid line does not force a major bump`() {
        val body = "This is not a BREAKING CHANGE: footer because it does not start the line"
        assertThat(ConventionalCommits.bumpOf(commit("fix: a thing", body))).isEqualTo(SemverBump.PATCH)
    }

    @Test
    fun `the declared bump is the highest of all commits`() {
        val commits = listOf(commit("chore: a thing"), commit("feat: a thing"), commit("fix: a thing"))
        assertThat(ConventionalCommits.declaredBump(commits)).isEqualTo(SemverBump.MINOR)
    }

    @Test
    fun `an empty history declares no bump`() {
        assertThat(ConventionalCommits.declaredBump(emptyList())).isEqualTo(SemverBump.NONE)
    }

    @Test
    fun `invalid subjects are reported, valid ones are not`() {
        val bad = commit("just a message")
        val commits = listOf(commit("feat: a thing"), bad, commit("fix: another thing"))
        assertThat(ConventionalCommits.invalidSubjects(commits)).containsExactly(bad)
    }

    @Test
    fun `the convention explanation lists every allowed type`() {
        val explanation = ConventionalCommits.explainConvention()
        assertThat(ConventionalCommits.types).allSatisfy { assertThat(explanation).contains(it) }
    }
}
