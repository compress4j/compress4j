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

import com.hominux.compress4j.archivers.ar.ArArchiveCreator;
import com.hominux.compress4j.archivers.cpio.CpioArchiveCreator;
import com.hominux.compress4j.archivers.memory.InMemoryArchiveCreator;
import com.hominux.compress4j.archivers.tar.TarArchiveCreator;
import com.hominux.compress4j.exceptions.UnsafeEntryException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.io.function.IOFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ArchiveCreatorAddTest {

    private static final FileTime T = FileTime.from(Instant.EPOCH);

    private static EntrySource.File unsized(String name) {
        return new EntrySource.File(
                name, 0, T, OptionalLong.empty(), () -> new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../evil", "a/../../evil", "a/..", "C:/evil", "..\u0000", "sub/..\u0000x", "C:evil.txt"})
    void rejectsUnsafeNames(String name) throws IOException {
        try (var creator =
                TarArchiveCreator.builder(new ByteArrayOutputStream()).build()) {
            assertThatThrownBy(() -> creator.add(EntrySource.file(name, new byte[0])))
                    .isInstanceOf(UnsafeEntryException.class)
                    .hasMessageContaining(name);
        }
    }

    @Test
    void sanitisesLeadingSlashesAndBackslashes() throws IOException {
        var out = new ByteArrayOutputStream();
        try (var creator = TarArchiveCreator.builder(out).build()) {
            creator.add(EntrySource.file("/a\\b.txt", new byte[0]));
        }
        assertThat(out.toString(StandardCharsets.ISO_8859_1)).contains("a/b.txt");
    }

    private static Stream<Arguments> sizeFirstFormats() {
        return Stream.of(
                Arguments.of("tar", (IOFunction<OutputStream, ArchiveCreator<?>>)
                        out -> TarArchiveCreator.builder(out).build()),
                Arguments.of("ar", (IOFunction<OutputStream, ArchiveCreator<?>>)
                        out -> ArArchiveCreator.builder(out).build()),
                Arguments.of("cpio", (IOFunction<OutputStream, ArchiveCreator<?>>)
                        out -> CpioArchiveCreator.builder(out).build()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sizeFirstFormats")
    void unknownSizeIntoSizeFirstFormatFailsWithBufferedHint(
            String format, IOFunction<OutputStream, ArchiveCreator<?>> factory) throws IOException {
        try (var creator = factory.apply(new ByteArrayOutputStream())) {
            assertThatThrownBy(() -> creator.add(unsized("a.txt")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a.txt")
                    .hasMessageContaining("EntrySource.buffered");
        }
    }

    @Test
    void unknownSizeRejectionLeavesTheCreatorUsable(@TempDir Path tempDir) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var creator = TarArchiveCreator.builder(out).build()) {
            assertThatThrownBy(() -> creator.add(unsized("a.txt"))).isInstanceOf(IllegalArgumentException.class);
            creator.add(EntrySource.buffered(unsized("a.txt"), tempDir));
        }
        assertThat(out.toString(StandardCharsets.ISO_8859_1)).contains("a.txt");
    }

    @Test
    void cpioWritesTheGivenSizeForAStreamThatReportsNothingAvailable() throws IOException {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        var stingy = new ByteArrayInputStream(content) {
            @Override
            public synchronized int available() {
                return 0;
            }
        };
        var out = new ByteArrayOutputStream();
        try (var creator = CpioArchiveCreator.builder(out).build()) {
            creator.addFile("a.txt", stingy, content.length);
        }

        try (var in = new CpioArchiveInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            var entry = in.getNextEntry();
            assertThat(entry.getName()).isEqualTo("a.txt");
            assertThat(entry.getSize()).isEqualTo(content.length);
            assertThat(in.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    void unknownSizeIntoSizeFreeFormatIsWritten() throws IOException {
        try (var creator =
                new InMemoryArchiveCreator.InMemoryArchiveCreatorBuilder(new ByteArrayOutputStream()).build()) {
            creator.add(unsized("a.txt"));
        }
    }

    @Test
    void creatorIsFailedAfterAWriteThrows() throws IOException {
        var creator = TarArchiveCreator.builder(new ByteArrayOutputStream()).build();
        var lying = new EntrySource.File("a", 0, T, OptionalLong.of(10), () -> new ByteArrayInputStream(new byte[2]));
        assertThatThrownBy(() -> creator.add(lying)).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> creator.add(EntrySource.file("b", new byte[0])))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(creator::close).isInstanceOf(IOException.class).hasMessageContaining("unclosed entries");
    }

    @Test
    void filterSeesSanitisedSources() throws IOException {
        var out = new ByteArrayOutputStream();
        try (var creator = TarArchiveCreator.builder(out)
                .filter(s -> !s.name().equals("skip.txt"))
                .build()) {
            creator.addAll(
                    Stream.of(EntrySource.file("/skip.txt", new byte[0]), EntrySource.file("keep.txt", new byte[0])));
        }
        assertThat(out.toString(StandardCharsets.ISO_8859_1))
                .contains("keep.txt")
                .doesNotContain("skip.txt");
    }

    @Test
    void addAllDoesNotCloseTheCallersStream() throws IOException {
        boolean[] closed = {false};
        try (var creator =
                TarArchiveCreator.builder(new ByteArrayOutputStream()).build()) {
            creator.addAll(Stream.of(EntrySource.file("a", new byte[0])).onClose(() -> closed[0] = true));
        }
        assertThat(closed[0]).isFalse();
    }

    @Test
    void addFileWithInputStreamRequiresASize() throws IOException {
        var out = new ByteArrayOutputStream();
        try (var creator = TarArchiveCreator.builder(out).build()) {
            creator.addFile("a.txt", new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)), 3);
        }
        assertThat(out.toString(StandardCharsets.ISO_8859_1)).contains("a.txt");
    }
}
