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

import static ch.qos.logback.classic.Level.TRACE;
import static io.github.compress4j.archive.compression.Compressor.sanitiseName;
import static io.github.compress4j.test.util.io.TestFileUtils.createFile;
import static io.github.compress4j.utils.FileUtils.NO_MODE;
import static java.nio.file.attribute.PosixFilePermission.*;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.github.compress4j.assertion.Compress4JAssertions;
import io.github.compress4j.memory.InMemoryCompressor;
import io.github.compress4j.memory.builder.InMemoryArchiveOutputStreamBuilder;
import io.github.compress4j.test.util.log.InMemoryLogAppender;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class CompressorTest {

    private static final String LOGGER_NAME = Compressor.class.getPackageName();
    private InMemoryLogAppender inMemoryLogAppender;

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

    @BeforeEach
    public void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
        inMemoryLogAppender = new InMemoryLogAppender();
        inMemoryLogAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(TRACE);
        logger.addAppender(inMemoryLogAppender);
        inMemoryLogAppender.start();
    }

    @AfterEach
    public void cleanUp() {
        inMemoryLogAppender.reset();
        inMemoryLogAppender.stop();
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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {
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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {
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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {
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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {
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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {

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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {
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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {

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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {

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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {
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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {

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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {
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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {
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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {
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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {

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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {
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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {

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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {

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
        try (MockedStatic<Compressor> mockCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                InMemoryCompressor compressor =
                        spy(new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out)))) {

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
    void shouldGetFileModeOnNixFileSystem() throws IOException {
        // given
        var mockPath = mock(Path.class);
        @SuppressWarnings("OctalInteger")
        int expectedMode = 0644;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(false);
            var mockAttributeView = mock(PosixFileAttributeView.class);
            var mockedAttributes = mock(PosixFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, PosixFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.permissions()).thenReturn(Set.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ));

            // when
            int actualMode = Compressor.mode(mockPath);

            // then
            assertThat(actualMode).isEqualTo(expectedMode);
        }
    }

    @Test
    void shouldGetNoModeOnNixFileSystemWithoutFileAttributes() throws IOException {
        // given
        var mockPath = mock(Path.class);
        given(mockPath.toString()).willReturn("some/path");

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(false);
            mockedFiles.when(() -> Files.getPosixFilePermissions(mockPath)).thenReturn(null);

            // when
            int actualMode = Compressor.mode(mockPath);

            // then
            assertThat(actualMode).isEqualTo(NO_MODE);
            Compress4JAssertions.assertThat(inMemoryLogAppender)
                    .contains("Cannot get POSIX file attributes for: some/path", TRACE);
        }
    }

    @Test
    void shouldGetFileModeOnWindowsFileSystem() throws IOException {
        // given
        var mockPath = mock(Path.class);
        @SuppressWarnings("OctalInteger")
        int expectedMode = 0003;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            var mockedAttributes = mock(DosFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.isReadOnly()).thenReturn(true);
            when(mockedAttributes.isHidden()).thenReturn(true);

            // when
            int actualMode = Compressor.mode(mockPath);

            // then
            assertThat(actualMode).isEqualTo(expectedMode);
        }
    }

    @Test
    void shouldGetFileModeOnWindowsFileSystemWithAttributesHiddenAsFalse() throws IOException {
        // given
        var mockPath = mock(Path.class);
        @SuppressWarnings("OctalInteger")
        int expectedMode = 0001;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            var mockedAttributes = mock(DosFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.isReadOnly()).thenReturn(true);
            when(mockedAttributes.isHidden()).thenReturn(false);

            // when
            int actualMode = Compressor.mode(mockPath);

            // then
            assertThat(actualMode).isEqualTo(expectedMode);
        }
    }

    @Test
    void shouldGetFileModeOnWindowsFileSystemWithAttributesReadOnlyAsFalse() throws IOException {
        // given
        var mockPath = mock(Path.class);
        @SuppressWarnings("OctalInteger")
        int expectedMode = 0002;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            var mockedAttributes = mock(DosFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.isReadOnly()).thenReturn(false);
            when(mockedAttributes.isHidden()).thenReturn(true);

            // when
            int actualMode = Compressor.mode(mockPath);

            // then
            assertThat(actualMode).isEqualTo(expectedMode);
        }
    }

    @Test
    void shouldGetFileModeOnWindowsFileSystemWithAttributesAsFalse() throws IOException {
        // given
        var mockPath = mock(Path.class);

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            var mockedAttributes = mock(DosFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.isReadOnly()).thenReturn(false);
            when(mockedAttributes.isHidden()).thenReturn(false);

            // when
            int actualMode = Compressor.mode(mockPath);

            // then
            assertThat(actualMode).isEqualTo(NO_MODE);
        }
    }

    @Test
    void shouldGetNoModeOnWindowsFileSystemWithoutFileAttributes() throws IOException {
        // given
        var mockPath = mock(Path.class);
        given(mockPath.toString()).willReturn("some/path");

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<Compressor> mockedCompressor = mockStatic(Compressor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(Compressor::isIsOsWindows).thenReturn(true);
            mockedFiles.when(() -> Files.getPosixFilePermissions(mockPath)).thenReturn(null);

            // when
            int actualMode = Compressor.mode(mockPath);

            // then
            assertThat(actualMode).isEqualTo(NO_MODE);
            Compress4JAssertions.assertThat(inMemoryLogAppender)
                    .contains("Cannot get DOS file attributes for: some/path", TRACE);
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
        try (InMemoryCompressor compressor =
                new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out, options))) {

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
        try (InMemoryCompressor ignored =
                new InMemoryCompressor(new InMemoryArchiveOutputStreamBuilder(out, options))) {
            fail("Should have thrown an IOException");
        } catch (IOException e) {
            // then
            assertThat(e).hasMessage("Cannot set option: nonExistingOption");
        }
    }
}
