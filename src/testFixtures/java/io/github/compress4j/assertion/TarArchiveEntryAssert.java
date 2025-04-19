/*
 * Copyright 2025 The Compress4J Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.compress4j.assertion;

import static org.assertj.core.api.Assertions.within;

import io.github.compress4j.utils.PosixFilePermissionsMapper;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class TarArchiveEntryAssert extends AbstractAssert<TarArchiveEntryAssert, TarArchiveEntry> {

    protected TarArchiveEntryAssert(TarArchiveEntry tarArchiveEntry) {
        super(tarArchiveEntry, TarArchiveEntryAssert.class);
    }

    public static TarArchiveEntryAssert assertThat(TarArchiveEntry tarArchiveEntry) {
        return new TarArchiveEntryAssert(tarArchiveEntry);
    }

    public TarArchiveEntryAssert hasName(String name) {
        Assertions.assertThat(actual.getName()).isEqualTo(name);
        return this;
    }

    public TarArchiveEntryAssert hasLinkName(String linkName) {
        Assertions.assertThat(actual.getLinkName()).isEqualTo(linkName);
        return this;
    }

    public TarArchiveEntryAssert hasSize(long size) {
        Assertions.assertThat(actual.getSize()).isEqualTo(size);
        return this;
    }

    public TarArchiveEntryAssert hasModTimeCloseToInSeconds(Date date) {
        return hasModTimeCloseToInSeconds(date.toInstant());
    }

    @SuppressWarnings("UnusedReturnValue")
    public TarArchiveEntryAssert hasModTimeCloseToInSeconds(FileTime fileTime) {
        return hasModTimeCloseToInSeconds(fileTime.toInstant());
    }

    public TarArchiveEntryAssert hasModTimeCloseToInSeconds(Instant instant) {
        Assertions.assertThat(actual.getModTime().toInstant()).isCloseTo(instant, within(1, ChronoUnit.SECONDS));
        return this;
    }

    public TarArchiveEntryAssert hasMode(long mod) {
        Assertions.assertThat(actual.getMode()).isEqualTo(mod);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public TarArchiveEntryAssert hasMode(Set<PosixFilePermission> permissions) {
        Set<PosixFilePermission> actualPermissions = PosixFilePermissionsMapper.fromUnixMode(actual.getMode());
        Assertions.assertThat(actualPermissions).containsAll(permissions);
        return this;
    }

    public TarArchiveEntryAssert isSymbolicLink() {
        Assertions.assertThat(actual.isSymbolicLink()).isTrue();
        return this;
    }

    public TarArchiveEntryAssert isNotSymbolicLink() {
        Assertions.assertThat(actual.isSymbolicLink()).isFalse();
        return this;
    }
}
