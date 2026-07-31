package io.github.compress4j.semver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReleaseTagsTest {

    @Test
    fun `strips the prefix and keeps the order git listed the tags in`() {
        assertThat(parseReleaseTags("v3.1.0\nv3.0.0\nv2.2.1")).containsExactly("3.1.0", "3.0.0", "2.2.1")
    }

    @Test
    fun `blank lines are dropped`() {
        assertThat(parseReleaseTags("v1.0.0\n\n  \n")).containsExactly("1.0.0")
    }

    @Test
    fun `no tag at all yields no candidates`() {
        assertThat(parseReleaseTags("")).isEmpty()
    }
}
