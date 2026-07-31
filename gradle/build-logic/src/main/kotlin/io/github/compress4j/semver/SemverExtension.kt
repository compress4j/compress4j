package io.github.compress4j.semver

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class SemverExtension {

    /** Version of the latest reachable `v*` tag, or empty when the repository has no release tag yet. */
    abstract val previousVersion: Property<String>

    /** Versions of the reachable `v*` tags, newest first, excluding a tag on the head itself. */
    abstract val releaseVersions: ListProperty<String>

    /** Highest bump declared by the conventional commits since [previousVersion]. */
    abstract val declaredBump: Property<SemverBump>
}
