/*
 * Copyright 2024-2026 The Compress4J Project
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
package io.github.compress4j.archivers.sevenz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.compress4j.archivers.ArchiveExtractor.EscapingSymlinkPolicy;
import io.github.compress4j.exceptions.ArchiveLimitExceededException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class SevenZArchiveTest {

    @TempDir
    Path tmp;

    private Path sourceTree() throws IOException {
        Path src = Files.createDirectories(tmp.resolve("src/sub"));
        Files.writeString(tmp.resolve("src/a.txt"), "alpha");
        Files.writeString(src.resolve("b.txt"), "bravo");
        return tmp.resolve("src");
    }

    private Path craft(String entryName, String content, char[] password) throws IOException {
        Path archive = tmp.resolve(password == null ? "crafted.7z" : "encrypted.7z");
        File file = archive.toFile();
        try (SevenZOutputFile out =
                password == null ? new SevenZOutputFile(file) : new SevenZOutputFile(file, password)) {
            SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setName(entryName);
            entry.setSize(content.length());
            out.putArchiveEntry(entry);
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.closeArchiveEntry();
        }
        return archive;
    }

    @Test
    @DisplayName("Round-trips files and directories")
    void roundTrip() throws IOException {
        Path archive = tmp.resolve("out.7z");
        try (var creator = SevenZArchiveCreator.builder(archive).build()) {
            creator.addDirectoryRecursively(sourceTree());
        }

        Path out = tmp.resolve("out");
        try (var extractor = SevenZArchiveExtractor.builder(archive).build()) {
            extractor.extract(out);
        }

        assertThat(out.resolve("a.txt")).hasContent("alpha");
        assertThat(out.resolve("sub/b.txt")).hasContent("bravo");
    }

    @Test
    @DisplayName("Applies the entry filter")
    void filter() throws IOException {
        Path archive = tmp.resolve("out.7z");
        try (var creator = SevenZArchiveCreator.builder(archive).build()) {
            creator.addDirectoryRecursively(sourceTree());
        }

        Path out = tmp.resolve("out");
        try (var extractor = SevenZArchiveExtractor.builder(archive)
                .filter(e -> e.name().endsWith("a.txt"))
                .build()) {
            extractor.extract(out);
        }

        assertThat(out.resolve("a.txt")).exists();
        assertThat(out.resolve("sub/b.txt")).doesNotExist();
    }

    @Test
    @DisplayName("Round-trips a symbolic link as a symlink")
    @DisabledOnOs(OS.WINDOWS)
    void symlinkRoundTrip() throws IOException {
        Path src = Files.createDirectories(tmp.resolve("src"));
        Files.writeString(src.resolve("target.txt"), "t");
        Files.createSymbolicLink(src.resolve("link"), Path.of("target.txt"));
        Path archive = tmp.resolve("out.7z");
        try (var creator = SevenZArchiveCreator.builder(archive).build()) {
            creator.addDirectoryRecursively(src);
        }

        Path out = tmp.resolve("out");
        try (var extractor = SevenZArchiveExtractor.builder(archive).build()) {
            extractor.extract(out);
        }

        assertThat(Files.isSymbolicLink(out.resolve("link"))).isTrue();
        assertThat(Files.readSymbolicLink(out.resolve("link"))).isEqualTo(Path.of("target.txt"));
    }

    @Test
    @DisplayName("Rejects path traversal")
    void pathTraversal() throws IOException {
        Path archive = craft("../evil.txt", "x", null);

        try (var extractor = SevenZArchiveExtractor.builder(archive).build()) {
            assertThatThrownBy(() -> extractor.extract(tmp.resolve("out")))
                    .hasMessageStartingWith("Path traversal vulnerability detected!");
        }
        assertThat(tmp.resolve("evil.txt")).doesNotExist();
    }

    @Test
    @DisplayName("Rejects an escaping symlink when the policy is DISALLOW")
    @DisabledOnOs(OS.WINDOWS)
    void escapingSymlink() throws IOException {
        Path src = Files.createDirectories(tmp.resolve("src"));
        Files.createSymbolicLink(src.resolve("link"), Path.of("../outside"));
        Path archive = tmp.resolve("out.7z");
        try (var creator = SevenZArchiveCreator.builder(archive).build()) {
            creator.addDirectoryRecursively(src);
        }

        try (var extractor = SevenZArchiveExtractor.builder(archive)
                .escapingSymlinkPolicy(EscapingSymlinkPolicy.DISALLOW)
                .build()) {
            assertThatThrownBy(() -> extractor.extract(tmp.resolve("out"))).hasMessageStartingWith("Invalid symlink");
        }
    }

    @Test
    @DisplayName("Enforces maxEntries")
    void maxEntries() throws IOException {
        Path archive = tmp.resolve("out.7z");
        try (var creator = SevenZArchiveCreator.builder(archive).build()) {
            creator.addDirectoryRecursively(sourceTree());
        }

        try (var extractor =
                SevenZArchiveExtractor.builder(archive).maxEntries(1).build()) {
            assertThatThrownBy(() -> extractor.extract(tmp.resolve("out")))
                    .isInstanceOf(ArchiveLimitExceededException.class);
        }
    }

    @Test
    @DisplayName("Enforces maxEntrySize")
    void maxEntrySize() throws IOException {
        Path archive = craft("big.txt", "0123456789", null);

        try (var extractor =
                SevenZArchiveExtractor.builder(archive).maxEntrySize(4).build()) {
            assertThatThrownBy(() -> extractor.extract(tmp.resolve("out")))
                    .isInstanceOf(ArchiveLimitExceededException.class);
        }
    }

    @Test
    @DisplayName("Extracts an encrypted archive with the right password")
    void password() throws IOException {
        Path archive = craft("secret.txt", "hush", "pw".toCharArray());

        Path out = tmp.resolve("out");
        try (var extractor = SevenZArchiveExtractor.builder(archive)
                .password("pw".toCharArray())
                .build()) {
            extractor.extract(out);
        }

        assertThat(out.resolve("secret.txt")).hasContent("hush");
    }

    @Test
    @DisplayName("Fails on an encrypted archive with the wrong or missing password")
    void wrongPassword() throws IOException {
        Path archive = craft("secret.txt", "hush", "pw".toCharArray());

        assertThatThrownBy(() -> SevenZArchiveExtractor.builder(archive)
                        .password("nope".toCharArray())
                        .build()
                        .extract(tmp.resolve("wrong")))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> SevenZArchiveExtractor.builder(archive).build().extract(tmp.resolve("none")))
                .isInstanceOf(IOException.class);
    }
}
