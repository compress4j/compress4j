package io.github.compress4j.semver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Matches the separators `GitHistoryValueSource` asks `git log` for via `%x1f` and `%x1e`. */
private const val FIELD = ''
private const val RECORD = ''

class GitHistoryTest {

    private fun record(hash: String, subject: String, body: String = "") =
        "$hash$FIELD$subject$FIELD$body$RECORD"

    @Test
    fun `reads the tag and the commits that followed it`() {
        val raw = "v1.2.3\n" + record("aaa", "feat: a thing") + record("bbb", "fix: another thing", "the body")

        val history = parseGitHistory(raw)

        assertThat(history.previousVersion).isEqualTo("1.2.3")
        assertThat(history.commits).containsExactly(
            Commit("aaa", "feat: a thing", ""),
            Commit("bbb", "fix: another thing", "the body"),
        )
    }

    @Test
    fun `a repository without a release tag has no previous version`() {
        val history = parseGitHistory("\n" + record("aaa", "feat: a thing"))

        assertThat(history.previousVersion).isNull()
        assertThat(history.commits).hasSize(1)
    }

    @Test
    fun `a blank tag line is treated as no previous version`() {
        assertThat(parseGitHistory("   \n").previousVersion).isNull()
    }

    @Test
    fun `an empty history has no commits`() {
        val history = parseGitHistory("v1.0.0\n")

        assertThat(history.previousVersion).isEqualTo("1.0.0")
        assertThat(history.commits).isEmpty()
    }

    @Test
    fun `completely empty input is tolerated`() {
        val history = parseGitHistory("")

        assertThat(history.previousVersion).isNull()
        assertThat(history.commits).isEmpty()
    }

    @Test
    fun `a multi line body is kept whole`() {
        val body = "line one\nline two\n\nBREAKING CHANGE: gone"
        val history = parseGitHistory("v1.0.0\n" + record("aaa", "feat: a thing", body))

        assertThat(history.commits.single().body).isEqualTo(body)
        assertThat(ConventionalCommits.declaredBump(history.commits)).isEqualTo(SemverBump.MAJOR)
    }

    @Test
    fun `a record without a body still parses`() {
        val history = parseGitHistory("v1.0.0\naaa${FIELD}feat: a thing$RECORD")

        assertThat(history.commits).containsExactly(Commit("aaa", "feat: a thing", ""))
    }
}
