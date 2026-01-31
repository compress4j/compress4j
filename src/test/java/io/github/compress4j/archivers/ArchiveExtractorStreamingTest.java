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
package io.github.compress4j.archivers;

import static io.github.compress4j.archivers.ArchiveExtractor.Entry.Type.FILE;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.compress4j.archivers.memory.InMemoryArchiveEntry;
import io.github.compress4j.archivers.memory.InMemoryArchiveExtractor;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Tests for the streaming API added to {@link ArchiveExtractor}.
 *
 * @since 3.0
 */
class ArchiveExtractorStreamingTest {

    @Test
    void testStreamReturnsAllEntries() throws IOException {
        // Given
        var entries = List.of(
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

        try (var extractor = InMemoryArchiveExtractor.builder(entries).build()) {
            // When
            List<String> names =
                    extractor.stream().map(ArchiveExtractor.Entry::name).collect(Collectors.toList());

            // Then
            assertThat(names).containsExactly("file1.txt", "file2.txt", "file3.txt");
        }
    }

    @Test
    void testStreamWithFilter() throws IOException {
        // Given
        var entries = List.of(
                InMemoryArchiveEntry.builder().name("doc.txt").content("text").build(),
                InMemoryArchiveEntry.builder()
                        .name("image.png")
                        .content("binary")
                        .build(),
                InMemoryArchiveEntry.builder()
                        .name("readme.txt")
                        .content("readme")
                        .build());

        try (var extractor = InMemoryArchiveExtractor.builder(entries).build()) {
            // When
            List<String> txtFiles = extractor.stream()
                    .map(ArchiveExtractor.Entry::name)
                    .filter(name -> name.endsWith(".txt"))
                    .toList();

            // Then
            assertThat(txtFiles).containsExactly("doc.txt", "readme.txt");
        }
    }

    @Test
    void testStreamCount() throws IOException {
        // Given
        var entries = List.of(
                InMemoryArchiveEntry.builder().name("file1.txt").type(FILE).build(),
                InMemoryArchiveEntry.builder().name("file2.txt").type(FILE).build());

        try (var extractor = InMemoryArchiveExtractor.builder(entries).build()) {
            // When
            long count = extractor.stream().count();

            // Then
            assertThat(count).isEqualTo(2);
        }
    }

    @Test
    void testIteratorWithForEachLoop() throws IOException {
        // Given
        var entries = List.of(
                InMemoryArchiveEntry.builder().name("a.txt").build(),
                InMemoryArchiveEntry.builder().name("b.txt").build(),
                InMemoryArchiveEntry.builder().name("c.txt").build());

        try (var extractor = InMemoryArchiveExtractor.builder(entries).build()) {
            // When
            List<String> names = new java.util.ArrayList<>();
            for (ArchiveExtractor.Entry entry : extractor) {
                names.add(entry.name());
            }

            // Then
            assertThat(names).containsExactly("a.txt", "b.txt", "c.txt");
        }
    }

    @Test
    void testIteratorHasNextAndNext() throws IOException {
        // Given
        var entries = List.of(
                InMemoryArchiveEntry.builder().name("first.txt").build(),
                InMemoryArchiveEntry.builder().name("second.txt").build());

        try (var extractor = InMemoryArchiveExtractor.builder(entries).build()) {
            // When
            Iterator<ArchiveExtractor.Entry> iterator = extractor.iterator();

            // Then
            assertThat(iterator.hasNext()).isTrue();
            assertThat(iterator.next().name()).isEqualTo("first.txt");

            assertThat(iterator.hasNext()).isTrue();
            assertThat(iterator.next().name()).isEqualTo("second.txt");

            assertThat(iterator.hasNext()).isFalse();
        }
    }

    @Test
    void testIteratorOnEmptyArchive() throws IOException {
        // Given
        try (var extractor = InMemoryArchiveExtractor.builder(List.of()).build()) {
            // When
            Iterator<ArchiveExtractor.Entry> iterator = extractor.iterator();

            // Then
            assertThat(iterator.hasNext()).isFalse();
        }
    }

    @Test
    void testStreamOnEmptyArchive() throws IOException {
        // Given
        try (var extractor = InMemoryArchiveExtractor.builder(List.of()).build()) {
            // When
            long count = extractor.stream().count();

            // Then
            assertThat(count).isZero();
        }
    }
}
