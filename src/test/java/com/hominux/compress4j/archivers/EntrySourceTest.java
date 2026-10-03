/*
 * Copyright 2026 The Compress4J Project
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
package com.hominux.compress4j.archivers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class EntrySourceTest {

    @TempDir
    Path tmp;

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void ofReadsFileAttributesWithRelativeName() throws IOException {
        // Given
        Path file = Files.writeString(Files.createDirectories(tmp.resolve("d")).resolve("a.sh"), "hi");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-x---"));

        // When
        var source = EntrySource.of(tmp, file);

        // Then
        assertThat(source).isInstanceOfSatisfying(EntrySource.File.class, f -> {
            assertThat(f.name()).isEqualTo("d/a.sh");
            assertThat(f.mode()).isEqualTo(0750);
            assertThat(f.size()).hasValue(2);
        });
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void ofDoesNotFollowSymlinks() throws IOException {
        // Given
        Files.writeString(tmp.resolve("t.txt"), "t");
        Path link = Files.createSymbolicLink(tmp.resolve("link"), Path.of("t.txt"));

        // Then
        assertThat(EntrySource.of(tmp, link))
                .isInstanceOfSatisfying(
                        EntrySource.Symlink.class, s -> assertThat(s.target()).isEqualTo("t.txt"));
    }

    @Test
    void ofRejectsBaseItselfAndOutsidePaths() {
        assertThatThrownBy(() -> EntrySource.of(tmp, tmp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(tmp.toString());
        assertThatThrownBy(() -> EntrySource.of(tmp.resolve("sub"), tmp.resolve("other.txt")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fileFromBytesKnowsItsSize() throws IOException {
        var f = EntrySource.file("a.txt", "abc".getBytes(StandardCharsets.UTF_8));
        assertThat(f.size()).hasValue(3);
        assertThat(f.mode()).isZero();
        try (var in = f.content().get()) {
            assertThat(in.readAllBytes()).asString(StandardCharsets.UTF_8).isEqualTo("abc");
        }
    }

    @Test
    void bufferedFillsInTheSizeAndSpoolsUnderTempDir() throws IOException {
        // Given
        var unsized = new EntrySource.File(
                "a.txt",
                0,
                FileTime.from(Instant.EPOCH),
                OptionalLong.empty(),
                () -> new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

        // When
        var buffered = EntrySource.buffered(unsized, tmp);

        // Then
        assertThat(buffered.size()).hasValue(5);
        assertThat(Files.list(tmp)).hasSize(1);
        try (var in = buffered.content().get()) {
            assertThat(in.readAllBytes()).asString(StandardCharsets.UTF_8).isEqualTo("hello");
        }
        assertThat(Files.list(tmp)).isEmpty();
    }

    @Test
    void modeKeepsPermissionBitsOnly() {
        var d = new EntrySource.Directory("d", 040755, FileTime.from(Instant.EPOCH));
        assertThat(d.mode()).isEqualTo(0755);
    }
}
