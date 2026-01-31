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
package io.github.compress4j;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.compress4j.archivers.ArchiveExtractor;
import io.github.compress4j.archivers.memory.InMemoryArchiveEntry;
import io.github.compress4j.archivers.memory.InMemoryArchiveExtractor;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Demonstration tests for the v3 API prototype.
 *
 * <p>These tests validate the new streaming and iteration capabilities of the v3 API.
 *
 * @since 3.0
 */
@DisplayName("V3 API Prototype Demonstration")
class V3PrototypeDemoTest {

    @Test
    @DisplayName("Stream API - filter and collect entries")
    void testStreamApi() throws IOException {
        // Given: An in-memory archive with various files
        List<InMemoryArchiveEntry> entries = List.of(
                InMemoryArchiveEntry.builder()
                        .name("readme.txt")
                        .content("README content")
                        .build(),
                InMemoryArchiveEntry.builder()
                        .name("src/main.java")
                        .content("Java code")
                        .build(),
                InMemoryArchiveEntry.builder()
                        .name("src/test.java")
                        .content("Test code")
                        .build(),
                InMemoryArchiveEntry.builder()
                        .name("docs/guide.md")
                        .content("Documentation")
                        .build(),
                InMemoryArchiveEntry.builder()
                        .name("build/")
                        .type(ArchiveExtractor.Entry.Type.DIR)
                        .build());

        // When: Using the new stream API to filter entries
        try (InMemoryArchiveExtractor extractor =
                InMemoryArchiveExtractor.builder(entries).build()) {
            List<String> javaFiles = extractor.stream()
                    .filter(e -> e.name().endsWith(".java"))
                    .map(ArchiveExtractor.Entry::name)
                    .collect(Collectors.toList());

            // Then: Only Java files should be in the result
            assertThat(javaFiles).containsExactly("src/main.java", "src/test.java");
        }
    }

    @Test
    @DisplayName("Iterator API - traditional for-each loop")
    void testIteratorApi() throws IOException {
        // Given: An in-memory archive
        List<InMemoryArchiveEntry> entries = List.of(
                InMemoryArchiveEntry.builder()
                        .name("file1.txt")
                        .content("content1")
                        .build(),
                InMemoryArchiveEntry.builder()
                        .name("file2.txt")
                        .content("content2")
                        .build(),
                InMemoryArchiveEntry.builder()
                        .name("file3.txt")
                        .content("content3")
                        .build());

        // When: Using the new iterator API
        try (InMemoryArchiveExtractor extractor =
                InMemoryArchiveExtractor.builder(entries).build()) {
            List<String> names = new java.util.ArrayList<>();
            for (ArchiveExtractor.Entry entry : extractor) {
                names.add(entry.name());
            }

            // Then: All entries should be iterated
            assertThat(names).containsExactly("file1.txt", "file2.txt", "file3.txt");
        }
    }

    @Test
    @DisplayName("Manual iteration - nextEntry() method")
    void testManualIteration() throws IOException {
        // Given: An in-memory archive
        List<InMemoryArchiveEntry> entries = List.of(
                InMemoryArchiveEntry.builder().name("alpha.txt").content("a").build(),
                InMemoryArchiveEntry.builder().name("beta.txt").content("b").build());

        // When: Using manual iteration with nextEntry()
        try (InMemoryArchiveExtractor extractor =
                InMemoryArchiveExtractor.builder(entries).build()) {
            ArchiveExtractor.Entry first = extractor.nextEntry();
            ArchiveExtractor.Entry second = extractor.nextEntry();
            ArchiveExtractor.Entry third = extractor.nextEntry();

            // Then: Entries should come in order, followed by null
            assertThat(first).isNotNull();
            assertThat(first.name()).isEqualTo("alpha.txt");
            assertThat(second).isNotNull();
            assertThat(second.name()).isEqualTo("beta.txt");
            assertThat(third).isNull();
        }
    }

    @Test
    @DisplayName("Stream API - count files vs directories")
    void testCountByType() throws IOException {
        // Given: An archive with mixed files and directories
        List<InMemoryArchiveEntry> entries = List.of(
                InMemoryArchiveEntry.builder()
                        .name("src/")
                        .type(ArchiveExtractor.Entry.Type.DIR)
                        .build(),
                InMemoryArchiveEntry.builder()
                        .name("src/main.java")
                        .content("code")
                        .build(),
                InMemoryArchiveEntry.builder()
                        .name("test/")
                        .type(ArchiveExtractor.Entry.Type.DIR)
                        .build(),
                InMemoryArchiveEntry.builder()
                        .name("test/test.java")
                        .content("test")
                        .build(),
                InMemoryArchiveEntry.builder().name("README.md").content("docs").build());

        // When: Counting files vs directories using stream
        try (InMemoryArchiveExtractor extractor =
                InMemoryArchiveExtractor.builder(entries).build()) {
            long fileCount = extractor.stream()
                    .filter(e -> e.type() == ArchiveExtractor.Entry.Type.FILE)
                    .count();

            // Then: Should have 3 files
            assertThat(fileCount).isEqualTo(3);
        }

        // When: Counting directories (need new extractor as stream was consumed)
        try (InMemoryArchiveExtractor extractor =
                InMemoryArchiveExtractor.builder(entries).build()) {
            long dirCount = extractor.stream()
                    .filter(e -> e.type() == ArchiveExtractor.Entry.Type.DIR)
                    .count();

            // Then: Should have 2 directories
            assertThat(dirCount).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Stream API - find first matching entry")
    void testFindFirst() throws IOException {
        // Given: An archive with configuration files
        List<InMemoryArchiveEntry> entries = List.of(
                InMemoryArchiveEntry.builder()
                        .name("app.properties")
                        .content("config")
                        .build(),
                InMemoryArchiveEntry.builder()
                        .name("config.yaml")
                        .content("yaml")
                        .build(),
                InMemoryArchiveEntry.builder().name("data.json").content("json").build());

        // When: Finding first YAML file
        try (InMemoryArchiveExtractor extractor =
                InMemoryArchiveExtractor.builder(entries).build()) {
            String yamlFile = extractor.stream()
                    .filter(e -> e.name().endsWith(".yaml"))
                    .map(ArchiveExtractor.Entry::name)
                    .findFirst()
                    .orElse(null);

            // Then: Should find config.yaml
            assertThat(yamlFile).isEqualTo("config.yaml");
        }
    }

    @Test
    @DisplayName("openEntryStream() - read entry content")
    void testOpenEntryStream() throws IOException {
        // Given: An archive with file content
        List<InMemoryArchiveEntry> entries = List.of(InMemoryArchiveEntry.builder()
                .name("hello.txt")
                .content("Hello, World!")
                .build());

        // When: Reading entry content using openEntryStream
        try (InMemoryArchiveExtractor extractor =
                InMemoryArchiveExtractor.builder(entries).build()) {
            ArchiveExtractor.Entry entry = extractor.nextEntry();
            assertThat(entry).isNotNull();

            String content = new String(extractor.openEntryStream(entry).readAllBytes());

            // Then: Content should match
            assertThat(content).isEqualTo("Hello, World!");
        }
    }
}
