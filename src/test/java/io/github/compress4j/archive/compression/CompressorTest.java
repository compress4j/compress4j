/*
 * Copyright 2024-2025 The Compress4J Project
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
package io.github.compress4j.archive.compression;

import static io.github.compress4j.archive.compression.Compressor.sanitiseName;
import static io.github.compress4j.utils.FileUtils.NO_MODE;
import static io.github.compress4j.utils.TestFileUtils.createFile;
import static java.nio.file.attribute.PosixFilePermission.*;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompressorTest {
    @TempDir
    private Path tempDir;

    @Mock
    private OutputStream out;

    private static Stream<Arguments> namesWithLeadingAndTrailingSlashes() {
        return Stream.of(
                Arguments.of("/file.txt", "file.txt"),
                Arguments.of("/../../../file.txt", "../../../file.txt"),
                Arguments.of("path/", "path"),
                Arguments.of("\\file.txt", "file.txt"),
                Arguments.of("\\..\\..\\..\\file.txt", "../../../file.txt"),
                Arguments.of("path\\", "path"));
    }

    private static Stream<Arguments> invalidNames() {
        return Stream.of(
                Arguments.of("/ "),
                Arguments.of(" /"),
                Arguments.of("\\ "),
                Arguments.of(" \\"),
                Arguments.of(" "),
                Arguments.of(""));
    }

    // ######################################################
    // #     Files tests                                    #
    // ######################################################
    @Test
    void shouldAddFileWithPath() throws IOException {
        // given
        String fileName = "file_name.txt";
        var path = createFile(tempDir, fileName, "789");
        @SuppressWarnings("OctalInteger")
        int fileMode = IS_OS_WINDOWS ? 0 : 0644;

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {
            // when
            compressor.addFile(path);

            // then
            BasicFileAttributes fileAttrs = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime modTime = fileAttrs.lastModifiedTime();

            InOrder inOrder = inOrder(compressor);
            inOrder.verify(compressor).addFile(path);
            inOrder.verify(compressor)
                    .addFile(eq(fileName), eq(path), any(BasicFileAttributes.class), eq(Optional.empty()));
            mockCompressor.verify(() -> sanitiseName(fileName));
            inOrder.verify(compressor).accept(fileName, path);
            inOrder.verify(compressor)
                    .writeFileEntry(eq(fileName), any(InputStream.class), eq(3L), eq(modTime), eq(fileMode));
            inOrder.verify(compressor)
                    .writeFileEntry(
                            eq(fileName),
                            any(InputStream.class),
                            eq(3L),
                            eq(modTime),
                            eq(fileMode),
                            eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithPathAppliesFilter() throws IOException {
        // given
        String fileName = "file_name.txt";
        var path = createFile(tempDir, fileName, "789");

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {
            compressor.withFilter((name, p) -> false);

            // when
            compressor.addFile(path);

            // then
            InOrder inOrder = inOrder(compressor);
            inOrder.verify(compressor).addFile(path);
            inOrder.verify(compressor)
                    .addFile(eq(fileName), eq(path), any(BasicFileAttributes.class), eq(Optional.empty()));
            mockCompressor.verify(() -> sanitiseName(fileName));
            inOrder.verify(compressor).accept(fileName, path);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldAddFileWithNameAndPath() throws IOException {
        // given
        String fileName = "file_name.txt";
        var path = createFile(tempDir, fileName, "789");
        String entryName = "additional_name.txt";
        @SuppressWarnings("OctalInteger")
        int fileMode = IS_OS_WINDOWS ? 0 : 0644;

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {
            // when
            compressor.addFile(entryName, path);

            // then
            BasicFileAttributes fileAttrs = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime modTime = fileAttrs.lastModifiedTime();

            InOrder inOrder = inOrder(compressor);
            inOrder.verify(compressor).addFile(entryName, path);
            mockCompressor.verify(() -> sanitiseName(entryName));
            inOrder.verify(compressor)
                    .addFile(eq(entryName), eq(path), any(BasicFileAttributes.class), eq(Optional.empty()));
            inOrder.verify(compressor).accept(entryName, path);
            inOrder.verify(compressor)
                    .writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(fileMode));
            inOrder.verify(compressor)
                    .writeFileEntry(
                            eq(entryName),
                            any(InputStream.class),
                            eq(3L),
                            eq(modTime),
                            eq(fileMode),
                            eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNamePathAndModTime() throws IOException {
        // given
        String fileName = "file_name.txt";
        var path = createFile(tempDir, fileName, "789");
        String entryName = "additional_name.txt";
        FileTime modTime = FileTime.from(Instant.now());
        @SuppressWarnings("OctalInteger")
        int fileMode = IS_OS_WINDOWS ? 0 : 0644;

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {
            // when
            compressor.addFile(entryName, path, modTime);

            // then
            verify(compressor).accept(entryName, path);
            verify(compressor)
                    .addFile(eq(entryName), eq(path), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            mockCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(fileMode));
            verify(compressor)
                    .writeFileEntry(
                            eq(entryName),
                            any(InputStream.class),
                            eq(3L),
                            eq(modTime),
                            eq(fileMode),
                            eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNameAndBytes() throws IOException {
        // given
        String entryName = "additional_name.txt";

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {

            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            byte[] content = "789".getBytes();

            // when
            compressor.addFile(entryName, content);

            // then
            mockCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).accept(entryName, null);
            FileTime modTime = FileTime.from(mockedInstant);
            verify(compressor).writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0));
            verify(compressor)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNameAndBytesAppliesFilter() throws IOException {
        // given
        String entryName = "additional_name.txt";

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {
            compressor.withFilter((name, p) -> false);
            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            byte[] content = "789".getBytes();

            // when
            compressor.addFile(entryName, content);

            // then
            mockCompressor.verify(() -> sanitiseName(entryName));
            InOrder inOrder = inOrder(compressor);
            inOrder.verify(compressor).accept(entryName, null);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldAddFileWithNameBytesAndModTime() throws IOException {
        // given
        String entryName = "additional_name.txt";
        FileTime modTime = FileTime.from(Instant.now());

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {

            byte[] content = "789".getBytes();

            // when
            compressor.addFile(entryName, content, modTime);

            // then
            mockCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).accept(entryName, null);
            verify(compressor).writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0));
            verify(compressor)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNameAndInputStream() throws IOException {
        // given
        String entryName = "additional_name.txt";

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {

            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            FileTime modTime = FileTime.from(mockedInstant);
            var content = new ByteArrayInputStream("789".getBytes());

            // when
            compressor.addFile(entryName, content);

            // then
            mockCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).accept(entryName, null);
            verify(compressor).writeFileEntry(eq(entryName), any(InputStream.class), eq(-1L), eq(modTime), eq(0));
            verify(compressor)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(-1L), eq(modTime), eq(0), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNameAndInputStreamAppliesFilter() throws IOException {
        // given
        String entryName = "additional_name.txt";

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {
            compressor.withFilter((name, p) -> false);

            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            var content = new ByteArrayInputStream("789".getBytes());

            // when
            compressor.addFile(entryName, content);

            // then
            mockCompressor.verify(() -> sanitiseName(entryName));
            InOrder inOrder = inOrder(compressor);
            inOrder.verify(compressor).accept(entryName, null);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldAddFileWithNameInputStreamAndModTime() throws IOException {
        // given
        String entryName = "additional_name.txt";
        FileTime modTime = FileTime.from(Instant.now());

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {

            var content = new ByteArrayInputStream("789".getBytes());

            // when
            compressor.addFile(entryName, content, modTime);

            // then
            mockCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).accept(entryName, null);
            verify(compressor).writeFileEntry(eq(entryName), any(InputStream.class), eq(-1L), eq(modTime), eq(0));
            verify(compressor)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(-1L), eq(modTime), eq(0), eq(Optional.empty()));
        }
    }

    // ######################################################
    // #     Directory tests                                #
    // ######################################################
    @Test
    void shouldAddDirectoryWithName() throws IOException {
        // given
        String entryName = "dir_name";

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {
            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            FileTime modTime = FileTime.from(mockedInstant);

            // when
            compressor.addDirectory(entryName);

            // then
            verify(compressor).accept(entryName, null);
            mockCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).writeDirectoryEntry(entryName, modTime);
        }
    }

    @Test
    void shouldAddDirectoryWithNameAndAppliesFilter() throws IOException {
        // given
        String entryName = "dir_name";

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {
            compressor.withFilter((name, p) -> false);

            // when
            compressor.addDirectory(entryName);

            // then
            InOrder inOrder = inOrder(compressor);
            inOrder.verify(compressor).accept(entryName, null);
            mockCompressor.verify(() -> sanitiseName(entryName));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldAddDirectoryWithNameAndModTime() throws IOException {
        // given
        String entryName = "dir_name";
        FileTime modTime = FileTime.from(Instant.now());

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {
            // when
            compressor.addDirectory(entryName, modTime);

            // then
            verify(compressor).accept(entryName, null);
            mockCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).writeDirectoryEntry(entryName, modTime);
        }
    }

    // ######################################################
    // #     Directory Recursively tests                    #
    // ######################################################
    @Test
    void shouldAddDirectoryRecursivelyWithPath() throws IOException {
        // given
        var base = tempDir.resolve("base");
        var file1 = createFile(base, "file1", "1");
        var subDir1 = base.resolve("subDir1");
        var file11 = createFile(subDir1, "file11", "11");
        String file11RelativeName = base.relativize(file11).toString();
        @SuppressWarnings("OctalInteger")
        int file11Mode = IS_OS_WINDOWS ? 0 : 0644;

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {

            // when
            compressor.addDirectoryRecursively(base);

            // then
            verify(compressor).addDirectoryRecursively("", base);
            mockCompressor.verify(() -> sanitiseName("file1"), times(2));
            mockCompressor.verify(() -> sanitiseName("subDir1"), times(2));
            mockCompressor.verify(() -> sanitiseName(file11RelativeName), atLeastOnce());
            verify(compressor).accept("subDir1", subDir1);
            FileTime subDir1ModTime = Files.getLastModifiedTime(subDir1);
            verify(compressor).addDirectory(eq("subDir1"), assertArg(time -> assertThat(
                            time.toInstant().truncatedTo(ChronoUnit.SECONDS))
                    .isEqualTo(subDir1ModTime.toInstant().truncatedTo(ChronoUnit.SECONDS))));
            verify(compressor).accept("subDir1", null);
            verify(compressor).writeDirectoryEntry(eq("subDir1"), assertArg(time -> assertThat(
                            time.toInstant().truncatedTo(ChronoUnit.SECONDS))
                    .isEqualTo(subDir1ModTime.toInstant().truncatedTo(ChronoUnit.SECONDS))));
            verify(compressor, times(2)).accept("subDir1/file11", file11);
            verify(compressor)
                    .addFile(eq("subDir1/file11"), eq(file11), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file11ModTime = Files.getLastModifiedTime(file11);
            verify(compressor)
                    .writeFileEntry(
                            eq("subDir1/file11"), any(InputStream.class), eq(2L), eq(file11ModTime), eq(file11Mode));
            verify(compressor)
                    .writeFileEntry(
                            eq("subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(file11ModTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
            verify(compressor, times(2)).accept("file1", file1);
            verify(compressor).addFile(eq("file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file1ModTime = Files.getLastModifiedTime(file1);
            verify(compressor)
                    .writeFileEntry(eq("file1"), any(InputStream.class), eq(1L), eq(file1ModTime), eq(file11Mode));
            verify(compressor)
                    .writeFileEntry(
                            eq("file1"),
                            any(InputStream.class),
                            eq(1L),
                            eq(file1ModTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddDirectoryRecursivelyWithPathAppliesFilter() throws IOException {
        // given
        var base = tempDir.resolve("base");
        var file1 = createFile(base, "file1", "1");
        var subDir1 = base.resolve("subDir1");
        createFile(subDir1, "file11", "11");

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {
            compressor.withFilter((name, p) -> false);

            // when
            compressor.addDirectoryRecursively(base);

            // then
            verify(compressor).addDirectoryRecursively("", base);
            mockCompressor.verify(() -> sanitiseName("file1"));
            mockCompressor.verify(() -> sanitiseName("subDir1"));
            verify(compressor).accept("subDir1", subDir1);
            verify(compressor).accept("file1", file1);
        }
    }

    @Test
    void shouldAddDirectoryRecursivelyWithPathAndTopLevelDir() throws IOException {
        // given
        String top = "top";
        var base = tempDir.resolve("base");
        var file1 = createFile(base, "file1", "1");
        var subDir1 = base.resolve("subDir1");
        var file11 = createFile(subDir1, "file11", "11");
        String file11RelativeName = base.relativize(file11).toString();
        @SuppressWarnings("OctalInteger")
        int file11Mode = IS_OS_WINDOWS ? 0 : 0644;

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {

            // when
            compressor.addDirectoryRecursively(top, base);

            // then
            mockCompressor.verify(() -> sanitiseName("file1"));
            mockCompressor.verify(() -> sanitiseName("subDir1"));
            mockCompressor.verify(() -> sanitiseName(file11RelativeName));
            verify(compressor).accept(top, null);
            FileTime baseModTime = Files.getLastModifiedTime(base);
            verify(compressor).writeDirectoryEntry("top", baseModTime);
            verify(compressor).accept(top, base);
            verify(compressor).accept("top/subDir1", subDir1);
            FileTime subDir1ModTime = Files.getLastModifiedTime(subDir1);
            verify(compressor).addDirectory(eq("top/subDir1"), assertArg(time -> assertThat(
                            time.toInstant().truncatedTo(ChronoUnit.SECONDS))
                    .isEqualTo(subDir1ModTime.toInstant().truncatedTo(ChronoUnit.SECONDS))));
            verify(compressor).accept("top/subDir1", null);
            verify(compressor).writeDirectoryEntry(eq("top/subDir1"), assertArg(time -> assertThat(
                            time.toInstant().truncatedTo(ChronoUnit.SECONDS))
                    .isEqualTo(subDir1ModTime.toInstant().truncatedTo(ChronoUnit.SECONDS))));
            verify(compressor, times(2)).accept("top/subDir1/file11", file11);
            verify(compressor)
                    .addFile(
                            eq("top/subDir1/file11"), eq(file11), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file11ModTime = Files.getLastModifiedTime(file11);
            verify(compressor)
                    .writeFileEntry(
                            eq("top/subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(file11ModTime),
                            eq(file11Mode));
            verify(compressor)
                    .writeFileEntry(
                            eq("top/subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(file11ModTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
            verify(compressor, times(2)).accept("top/file1", file1);
            verify(compressor)
                    .addFile(eq("top/file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file1ModTime = Files.getLastModifiedTime(file1);
            verify(compressor)
                    .writeFileEntry(eq("top/file1"), any(InputStream.class), eq(1L), eq(file1ModTime), eq(file11Mode));
            verify(compressor)
                    .writeFileEntry(
                            eq("top/file1"),
                            any(InputStream.class),
                            eq(1L),
                            eq(file1ModTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddDirectoryRecursivelyWithPathAndModTime() throws IOException {
        // given
        FileTime modTime = FileTime.from(Instant.now());
        var base = tempDir.resolve("base");
        var file1 = createFile(base, "file1", "1");
        var subDir1 = base.resolve("subDir1");
        var file11 = createFile(subDir1, "file11", "11");
        String file11RelativeName = base.relativize(file11).toString();
        @SuppressWarnings("OctalInteger")
        int file11Mode = IS_OS_WINDOWS ? 0 : 0644;

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {

            // when
            compressor.addDirectoryRecursively(base, modTime);

            // then
            verify(compressor).addDirectoryRecursively("", base, modTime);
            mockCompressor.verify(() -> sanitiseName("file1"), times(2));
            mockCompressor.verify(() -> sanitiseName("subDir1"), times(2));
            mockCompressor.verify(() -> sanitiseName(file11RelativeName), atLeastOnce());
            verify(compressor).accept("subDir1", subDir1);
            verify(compressor).addDirectory("subDir1", modTime);
            verify(compressor).accept("subDir1", null);
            verify(compressor).writeDirectoryEntry("subDir1", modTime);
            verify(compressor, times(2)).accept("subDir1/file11", file11);
            verify(compressor)
                    .addFile(
                            eq("subDir1/file11"), eq(file11), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            verify(compressor)
                    .writeFileEntry(eq("subDir1/file11"), any(InputStream.class), eq(2L), eq(modTime), eq(file11Mode));
            verify(compressor)
                    .writeFileEntry(
                            eq("subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(modTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
            verify(compressor, times(2)).accept("file1", file1);
            verify(compressor)
                    .addFile(eq("file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            verify(compressor).writeFileEntry(eq("file1"), any(InputStream.class), eq(1L), eq(modTime), eq(file11Mode));
            verify(compressor)
                    .writeFileEntry(
                            eq("file1"),
                            any(InputStream.class),
                            eq(1L),
                            eq(modTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddDirectoryRecursivelyWithPathTopLevelDirAndModTime() throws IOException {
        // given
        FileTime modTime = FileTime.from(Instant.now());
        String top = "top";
        var base = tempDir.resolve("base");
        var file1 = createFile(base, "file1", "1");
        var subDir1 = base.resolve("subDir1");
        var file11 = createFile(subDir1, "file11", "11");
        String file11RelativeName = base.relativize(file11).toString();
        @SuppressWarnings("OctalInteger")
        int file11Mode = IS_OS_WINDOWS ? 0 : 0644;

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                CompressorUnderTest compressor = spy(new CompressorUnderTest(out))) {

            // when
            compressor.addDirectoryRecursively(top, base, modTime);

            // then
            mockCompressor.verify(() -> sanitiseName("file1"));
            mockCompressor.verify(() -> sanitiseName("subDir1"));
            mockCompressor.verify(() -> sanitiseName(file11RelativeName));
            verify(compressor).addDirectoryRecursively(top, base, modTime);
            verify(compressor).accept(top, base);
            verify(compressor).addDirectory(top, modTime);
            verify(compressor).accept(top, null);
            verify(compressor).writeDirectoryEntry("top", modTime);
            verify(compressor).accept("top/subDir1", subDir1);
            verify(compressor).addDirectory("top/subDir1", modTime);
            verify(compressor).accept("top/subDir1", null);
            verify(compressor).writeDirectoryEntry("top/subDir1", modTime);
            verify(compressor, times(2)).accept("top/subDir1/file11", file11);
            verify(compressor)
                    .addFile(
                            eq("top/subDir1/file11"),
                            eq(file11),
                            any(BasicFileAttributes.class),
                            eq(Optional.of(modTime)));
            verify(compressor)
                    .writeFileEntry(
                            eq("top/subDir1/file11"), any(InputStream.class), eq(2L), eq(modTime), eq(file11Mode));
            verify(compressor)
                    .writeFileEntry(
                            eq("top/subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(modTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
            verify(compressor, times(2)).accept("top/file1", file1);
            verify(compressor)
                    .addFile(eq("top/file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            verify(compressor)
                    .writeFileEntry(eq("top/file1"), any(InputStream.class), eq(1L), eq(modTime), eq(file11Mode));
            verify(compressor)
                    .writeFileEntry(
                            eq("top/file1"),
                            any(InputStream.class),
                            eq(1L),
                            eq(modTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
        }
    }

    // ######################################################
    // #  Utility methods tests                             #
    // ######################################################
    @Test
    void shouldReplaceBackslashesWithForwardSlashesFromName() {
        // given
        String name = "some_dir\\file_name.txt";

        // when
        String sanitisedName = sanitiseName(name);

        // then
        assertThat(sanitisedName).isEqualTo("some_dir/file_name.txt");
    }

    @ParameterizedTest
    // given
    @MethodSource("namesWithLeadingAndTrailingSlashes")
    void shouldRemoveLeadingAndTrailingSlashesFromName(String entryName, String expected) {
        // when
        String actual = sanitiseName(entryName);

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    // given
    @MethodSource("invalidNames")
    void shouldEnsurePathIsNotEmpty(String entryName) {
        // when & then
        assertThatThrownBy(() -> sanitiseName(entryName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid entry name: " + entryName);
    }

    @Test
    void shouldGetUnixModeOnNixFileSystem() throws IOException {
        // given
        var path = createFile(tempDir, "file.txt", "789");
        @SuppressWarnings("OctalInteger")
        int expectedMode = 0644;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor =
                                mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(false);
            var mockAttributeView = mock(PosixFileAttributeView.class);
            var mockedAttributes = mock(PosixFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(path, PosixFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.permissions()).thenReturn(Set.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ));

            // when
            int actualMode = Compressor.mode(path);

            // then
            assertThat(actualMode).isEqualTo(expectedMode);
        }
    }

    @Test
    void shouldGetNodeModeOnNixFileSystemWithoutFileAttributes() throws IOException {
        // given
        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor =
                                mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(false);
            var path = tempDir.resolve("file.txt");
            mockedFiles.when(() -> Files.getPosixFilePermissions(path)).thenReturn(null);

            // when
            int actualMode = Compressor.mode(path);

            // then
            assertThat(actualMode).isEqualTo(NO_MODE);
        }
    }

    @Test
    void shouldGetUnixModeOnWindowsFileSystem() throws IOException {
        // given
        var path = createFile(tempDir, "file.txt", "789");
        int expectedMode = 0b11;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor =
                                mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            var mockedAttributes = mock(DosFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(path, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.isReadOnly()).thenReturn(true);
            when(mockedAttributes.isHidden()).thenReturn(true);

            // when
            int actualMode = Compressor.mode(path);

            // then
            assertThat(actualMode).isEqualTo(expectedMode);
        }
    }

    @Test
    void shouldGetUnixModeOnWindowsFileSystemWithAttributesAsFalse() throws IOException {
        // given
        var path = createFile(tempDir, "file.txt", "789");

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor =
                                mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            var mockedAttributes = mock(DosFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(path, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.isReadOnly()).thenReturn(false);
            when(mockedAttributes.isHidden()).thenReturn(false);

            // when
            int actualMode = Compressor.mode(path);

            // then
            assertThat(actualMode).isEqualTo(NO_MODE);
        }
    }

    @Test
    void shouldGetNodeModeOnWindowsFileSystemWithoutFileAttributes() throws IOException {
        // given
        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor =
                                mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(true);
            var path = tempDir.resolve("file.txt");
            mockedFiles.when(() -> Files.getPosixFilePermissions(path)).thenReturn(null);

            // when
            int actualMode = Compressor.mode(path);

            // then
            assertThat(actualMode).isEqualTo(NO_MODE);
        }
    }

    @Test
    void shouldGetDefaultCompressionLevelFromOptionsWhenNotSet() {
        // given
        Map<String, Object> options = Map.of();
        int expectedLevel = -1;

        // when
        int actualLevel = Compressor.getCompressionLevel(options);

        // then
        assertThat(actualLevel).isEqualTo(expectedLevel);
    }

    @Test
    void shouldGetCompressionLevelFromOptions() {
        // given
        Map<String, Object> options = Map.of("compression-level", 5);
        int expectedLevel = 5;

        // when
        int actualLevel = Compressor.getCompressionLevel(options);

        // then
        assertThat(actualLevel).isEqualTo(expectedLevel);
    }

    @Test
    void shouldThrowExceptionWhenInvalidCompressionLevel() {
        // given
        Map<String, Object> options = Map.of("compression-level", "5");

        // when & then
        assertThatThrownBy(() -> Compressor.getCompressionLevel(options))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot set compression level 5");
    }

    @Test
    void shouldSetOptionOnArchiveOutputStream() throws IOException {
        // given
        Map<String, Object> options = Map.of("someOption", 42);
        try (CompressorUnderTest compressor = new CompressorUnderTest(out, options)) {

            // when
            int actualOption = compressor.archiveOutputStream.getSomeOption();

            // then
            assertThat(actualOption).isEqualTo(42);
        }
    }

    @Test
    void shouldThrowExceptionWhenInvalidOptionDefined() {
        // given
        Map<String, Object> options = Map.of("nonExistingOption", 42);
        // when
        try (CompressorUnderTest ignored = new CompressorUnderTest(out, options)) {
            fail("Should have thrown an IOException");
        } catch (IOException e) {
            // then
            assertThat(e).hasMessage("Cannot set option: nonExistingOption");
        }
    }

    // ######################################################
    // #  Utility classes                                   #
    // ######################################################
    public static class CompressorUnderTest extends Compressor<ArchiveOutputStreamUnderTest> {

        public CompressorUnderTest(OutputStream outputStream) throws IOException {
            super(outputStream);
        }

        public CompressorUnderTest(OutputStream outputStream, Map<String, Object> options) throws IOException {
            super(outputStream, options);
        }

        @Override
        protected ArchiveOutputStreamUnderTest buildArchiveOutputStream(
                OutputStream outputStream, Map<String, Object> options) throws IOException {
            ArchiveOutputStreamUnderTest aOut = new ArchiveOutputStreamUnderTest(outputStream);
            return applyFormatOptions(aOut, options);
        }

        @Override
        protected void writeDirectoryEntry(String name, FileTime modTime) {
            DumpArchiveEntry entry = new DumpArchiveEntry(name, name);
            entry.setLastModifiedDate(Date.from(modTime.toInstant()));
            archiveOutputStream.putArchiveEntry(entry);
            archiveOutputStream.closeArchiveEntry();
        }

        @Override
        protected void writeFileEntry(
                String name,
                InputStream source,
                long length,
                FileTime modTime,
                int mode,
                Optional<Path> symlinkTarget) {
            DumpArchiveEntry entry = new DumpArchiveEntry(name, name);
            entry.setSize(length);
            entry.setMode(mode);
            entry.setLastModifiedDate(Date.from(modTime.toInstant()));
            archiveOutputStream.putArchiveEntry(entry);
            archiveOutputStream.closeArchiveEntry();
        }
    }

    public static class ArchiveOutputStreamUnderTest extends ArchiveOutputStream<DumpArchiveEntry> {
        private int someOption = 0;

        public ArchiveOutputStreamUnderTest(OutputStream out) {
            super(out);
        }

        @Override
        public void closeArchiveEntry() {
            // do nothing
        }

        @Override
        public DumpArchiveEntry createArchiveEntry(File inputFile, String entryName) {
            return new DumpArchiveEntry(entryName, entryName);
        }

        @Override
        public void putArchiveEntry(DumpArchiveEntry entry) {
            // do nothing
        }

        public int getSomeOption() {
            return someOption;
        }

        @SuppressWarnings("unused") // used reflectively in tests
        public void setSomeOption(int someOption) {
            this.someOption = someOption;
        }
    }
}