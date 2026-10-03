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

import static com.hominux.compress4j.archivers.catalog.Capability.DIRECTORIES;
import static com.hominux.compress4j.archivers.catalog.Capability.LAST_MODIFIED;
import static com.hominux.compress4j.archivers.catalog.Capability.MODES;
import static com.hominux.compress4j.archivers.catalog.Capability.SYMLINKS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import com.hominux.compress4j.archivers.catalog.ArchiveFormat;
import com.hominux.compress4j.archivers.catalog.FormatCatalog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisabledOnOs(OS.WINDOWS)
class FormatContractTest {

    private static final FileTime MODIFIED = FileTime.from(Instant.parse("2024-05-01T10:00:00Z"));

    @TempDir
    Path tmp;

    static Stream<ArchiveFormat> writable() {
        return FormatCatalog.writable();
    }

    private Path source() throws IOException {
        Path src = Files.createDirectories(tmp.resolve("src"));
        Path dir = Files.createDirectories(src.resolve("d"));
        Path file = Files.writeString(dir.resolve("run.sh"), "echo hi", StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-x---"));
        Files.setLastModifiedTime(file, MODIFIED);
        Files.createSymbolicLink(src.resolve("link"), Path.of("d/run.sh"));
        return src;
    }

    private Path roundTrip(ArchiveFormat format) throws IOException {
        Path src = source();
        Path archive = tmp.resolve("archive." + format.name());
        try (var creator = format.createAt().orElseThrow().apply(archive)) {
            creator.addDirectoryRecursively(src);
        }
        return archive;
    }

    private Map<String, ArchiveExtractor.Entry> entries(ArchiveFormat format, Path archive) throws IOException {
        try (var extractor = format.readAt().apply(archive)) {
            return extractor.stream()
                    .map(ArchiveItem::entry)
                    .collect(Collectors.toMap(ArchiveExtractor.Entry::name, e -> e));
        }
    }

    @ParameterizedTest
    @MethodSource("writable")
    void fileContentSurvivesRoundTrip(ArchiveFormat format) throws IOException {
        Path archive = roundTrip(format);
        Path out = tmp.resolve("out");
        try (var extractor = format.readAt().apply(archive)) {
            extractor.extract(out);
        }
        assertThat(out.resolve("d/run.sh")).hasContent("echo hi");
    }

    @ParameterizedTest
    @MethodSource("writable")
    void modesMatchDeclaration(ArchiveFormat format) throws IOException {
        var file = entries(format, roundTrip(format)).get("d/run.sh");
        if (format.has(MODES)) {
            assertThat(file.mode() & 0777).isEqualTo(0750);
        } else {
            assertThat(file.mode() & 0777).isNotEqualTo(0750);
        }
    }

    @ParameterizedTest
    @MethodSource("writable")
    void symlinksMatchDeclaration(ArchiveFormat format) throws IOException {
        var link = entries(format, roundTrip(format)).get("link");
        if (format.has(SYMLINKS)) {
            assertThat(link.type()).isEqualTo(ArchiveExtractor.Entry.Type.SYMLINK);
            assertThat(link.linkTarget()).contains("d/run.sh");
        } else {
            assertThat(link.type()).isNotEqualTo(ArchiveExtractor.Entry.Type.SYMLINK);
        }
    }

    @ParameterizedTest
    @MethodSource("writable")
    void directoriesMatchDeclaration(ArchiveFormat format) throws IOException {
        var names = entries(format, roundTrip(format));
        assertThat(names.containsKey("d") && names.get("d").type() == ArchiveExtractor.Entry.Type.DIR)
                .isEqualTo(format.has(DIRECTORIES));
    }

    @ParameterizedTest
    @MethodSource("writable")
    void lastModifiedMatchesDeclaration(ArchiveFormat format) throws IOException {
        assumeThat(format.has(LAST_MODIFIED)).isTrue();
        var file = entries(format, roundTrip(format)).get("d/run.sh");
        assertThat(file.lastModified()).isPresent();
        long deltaMillis = Math.abs(file.lastModified().orElseThrow().toMillis() - MODIFIED.toMillis());
        assertThat(deltaMillis).isLessThanOrEqualTo(2000);
    }
}
