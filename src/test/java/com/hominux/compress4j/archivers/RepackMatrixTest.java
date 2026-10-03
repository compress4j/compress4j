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

import com.hominux.compress4j.archivers.catalog.ArchiveFormat;
import com.hominux.compress4j.archivers.catalog.FormatCatalog;
import com.hominux.compress4j.archivers.memory.InMemoryArchiveEntry;
import com.hominux.compress4j.archivers.memory.InMemoryArchiveExtractor;
import com.hominux.compress4j.exceptions.UnsafeEntryException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RepackMatrixTest {

    @TempDir
    Path tmp;

    static Stream<Arguments> pairs() {
        return FormatCatalog.writable().flatMap(from -> FormatCatalog.writable().map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("pairs")
    void repacksFileContent(ArchiveFormat from, ArchiveFormat to) throws IOException {
        // Given
        Path source = tmp.resolve("in." + from.name());
        try (var creator = from.createAt().orElseThrow().apply(source)) {
            creator.addFile("d/a.txt", "alpha".getBytes(StandardCharsets.UTF_8));
        }

        // When
        Path target = tmp.resolve("out." + to.name());
        try (var extractor = from.readAt().apply(source);
                var creator = to.createAt().orElseThrow().apply(target)) {
            creator.addAll(extractor.stream().map(ArchiveItem::toSource));
        }

        // Then
        Path out = Files.createDirectories(tmp.resolve("x"));
        try (var extractor = to.readAt().apply(target)) {
            extractor.extract(out);
        }
        assertThat(out.resolve("d/a.txt")).hasContent("alpha");
    }

    @Test
    void repackingAHostileNameIsRejected() throws IOException {
        var tar = FormatCatalog.all()
                .filter(f -> f.name().equals("tar"))
                .findFirst()
                .orElseThrow();
        var hostile = InMemoryArchiveExtractor.builder(List.of(InMemoryArchiveEntry.builder()
                        .name("../evil")
                        .content("x")
                        .build()))
                .build();
        try (hostile;
                var creator = tar.createAt().orElseThrow().apply(tmp.resolve("out.tar"))) {
            assertThatThrownBy(() -> creator.addAll(hostile.stream().map(ArchiveItem::toSource)))
                    .isInstanceOf(UnsafeEntryException.class);
        }
    }
}
