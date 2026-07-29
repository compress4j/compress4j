package io.github.compress4j.semver

/** Ordered from smallest to largest: comparisons rely on the declaration order. */
enum class SemverBump {
    NONE,
    PATCH,
    MINOR,
    MAJOR;

    companion object {
        fun highestOf(bumps: Iterable<SemverBump>): SemverBump = bumps.maxOrNull() ?: NONE
    }
}

fun nextVersion(previousVersion: String, bump: SemverBump): String {
    val core = previousVersion.substringBefore('-').substringBefore('+')
    val parts = core.split('.')
    require(parts.size == 3 && parts.all { it.toIntOrNull() != null }) {
        "Cannot derive the next version from '$previousVersion': expected a <major>.<minor>.<patch> release version"
    }
    val (major, minor, patch) = parts.map { it.toInt() }
    return when (bump) {
        SemverBump.MAJOR -> "${major + 1}.0.0"
        SemverBump.MINOR -> "$major.${minor + 1}.0"
        SemverBump.PATCH -> "$major.$minor.${patch + 1}"
        SemverBump.NONE -> core
    }
}
