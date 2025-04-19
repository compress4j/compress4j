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
package io.github.compress4j.archive;

import static ch.qos.logback.classic.Level.TRACE;
import static io.github.compress4j.archive.ArchiveCreator.sanitiseName;
import static io.github.compress4j.test.util.io.TestFileUtils.createFile;
import static io.github.compress4j.utils.FileUtils.NO_MODE;
import static java.nio.file.attribute.PosixFilePermission.*;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.github.compress4j.archive.memory.InMemoryArchiveCreator;
import io.github.compress4j.archive.memory.InMemoryArchiveCreator.InMemoryArchiveCreatorBuilder;
import io.github.compress4j.assertion.Compress4JAssertions;
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
class ArchiveCreatorTest {

    private static final String LOGGER_NAME = ArchiveCreator.class.getPackageName();
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
    void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
        inMemoryLogAppender = new InMemoryLogAppender();
        inMemoryLogAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(TRACE);
        logger.addAppender(inMemoryLogAppender);
        inMemoryLogAppender.start();
    }

    @AfterEach
    void cleanUp() {
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
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {
            // when
            archive.addFile(path);

            // then
            BasicFileAttributes fileAttrs = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime modTime = fileAttrs.lastModifiedTime();

            InOrder inOrder = inOrder(archive);
            inOrder.verify(archive).addFile(path);
            inOrder.verify(archive)
                    .addFile(eq(fileName), eq(path), any(BasicFileAttributes.class), eq(Optional.empty()));
            mockArchive.verify(() -> sanitiseName(fileName));
            inOrder.verify(archive).accept(fileName, path);
            inOrder.verify(archive)
                    .writeFileEntry(eq(fileName), any(InputStream.class), eq(3L), eq(modTime), eq(fileMode));
            inOrder.verify(archive)
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
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {
            archive.withFilter((name, p) -> false);

            // when
            archive.addFile(path);

            // then
            InOrder inOrder = inOrder(archive);
            inOrder.verify(archive).addFile(path);
            inOrder.verify(archive)
                    .addFile(eq(fileName), eq(path), any(BasicFileAttributes.class), eq(Optional.empty()));
            mockArchive.verify(() -> sanitiseName(fileName));
            inOrder.verify(archive).accept(fileName, path);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldAddFileWithPathAppliesFilterViaBuilder() throws IOException {
        // given
        String fileName = "file_name.txt";
        var path = createFile(tempDir, fileName, "789");

        //noinspection rawtypes
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive = spy(new InMemoryArchiveCreator(
                        new InMemoryArchiveCreatorBuilder(out).withFilter((name, p) -> false)))) {
            // when
            archive.addFile(path);

            // then
            InOrder inOrder = inOrder(archive);
            inOrder.verify(archive).addFile(path);
            inOrder.verify(archive)
                    .addFile(eq(fileName), eq(path), any(BasicFileAttributes.class), eq(Optional.empty()));
            mockArchive.verify(() -> sanitiseName(fileName));
            inOrder.verify(archive).accept(fileName, path);
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
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {
            // when
            archive.addFile(entryName, path);

            // then
            BasicFileAttributes fileAttrs = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime modTime = fileAttrs.lastModifiedTime();

            InOrder inOrder = inOrder(archive);
            inOrder.verify(archive).addFile(entryName, path);
            mockArchive.verify(() -> sanitiseName(entryName));
            inOrder.verify(archive)
                    .addFile(eq(entryName), eq(path), any(BasicFileAttributes.class), eq(Optional.empty()));
            inOrder.verify(archive).accept(entryName, path);
            inOrder.verify(archive)
                    .writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(fileMode));
            inOrder.verify(archive)
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
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {
            // when
            archive.addFile(entryName, path, modTime);

            // then
            verify(archive).accept(entryName, path);
            verify(archive).addFile(eq(entryName), eq(path), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            mockArchive.verify(() -> sanitiseName(entryName));
            verify(archive).writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(fileMode));
            verify(archive)
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
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {

            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            byte[] content = "789".getBytes();

            // when
            archive.addFile(entryName, content);

            // then
            mockArchive.verify(() -> sanitiseName(entryName));
            verify(archive).accept(entryName, null);
            FileTime modTime = FileTime.from(mockedInstant);
            verify(archive).writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0));
            verify(archive)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNameAndBytesAppliesFilter() throws IOException {
        // given
        String entryName = "additional_name.txt";

        //noinspection rawtypes
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {
            archive.withFilter((name, p) -> false);
            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            byte[] content = "789".getBytes();

            // when
            archive.addFile(entryName, content);

            // then
            mockArchive.verify(() -> sanitiseName(entryName));
            InOrder inOrder = inOrder(archive);
            inOrder.verify(archive).accept(entryName, null);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldAddFileWithNameBytesAndModTime() throws IOException {
        // given
        String entryName = "additional_name.txt";
        FileTime modTime = FileTime.from(Instant.now());

        //noinspection rawtypes
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {

            byte[] content = "789".getBytes();

            // when
            archive.addFile(entryName, content, modTime);

            // then
            mockArchive.verify(() -> sanitiseName(entryName));
            verify(archive).accept(entryName, null);
            verify(archive).writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0));
            verify(archive)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNameAndInputStream() throws IOException {
        // given
        String entryName = "additional_name.txt";

        //noinspection rawtypes
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {

            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            FileTime modTime = FileTime.from(mockedInstant);
            var content = new ByteArrayInputStream("789".getBytes());

            // when
            archive.addFile(entryName, content);

            // then
            mockArchive.verify(() -> sanitiseName(entryName));
            verify(archive).accept(entryName, null);
            verify(archive).writeFileEntry(eq(entryName), any(InputStream.class), eq(-1L), eq(modTime), eq(0));
            verify(archive)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(-1L), eq(modTime), eq(0), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNameAndInputStreamAppliesFilter() throws IOException {
        // given
        String entryName = "additional_name.txt";

        //noinspection rawtypes
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {
            archive.withFilter((name, p) -> false);

            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            var content = new ByteArrayInputStream("789".getBytes());

            // when
            archive.addFile(entryName, content);

            // then
            mockArchive.verify(() -> sanitiseName(entryName));
            InOrder inOrder = inOrder(archive);
            inOrder.verify(archive).accept(entryName, null);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldAddFileWithNameInputStreamAndModTime() throws IOException {
        // given
        String entryName = "additional_name.txt";
        FileTime modTime = FileTime.from(Instant.now());

        //noinspection rawtypes
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {

            var content = new ByteArrayInputStream("789".getBytes());

            // when
            archive.addFile(entryName, content, modTime);

            // then
            mockArchive.verify(() -> sanitiseName(entryName));
            verify(archive).accept(entryName, null);
            verify(archive).writeFileEntry(eq(entryName), any(InputStream.class), eq(-1L), eq(modTime), eq(0));
            verify(archive)
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
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {
            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            FileTime modTime = FileTime.from(mockedInstant);

            // when
            archive.addDirectory(entryName);

            // then
            verify(archive).accept(entryName, null);
            mockArchive.verify(() -> sanitiseName(entryName));
            verify(archive).writeDirectoryEntry(entryName, modTime);
        }
    }

    @Test
    void shouldAddDirectoryWithNameAndAppliesFilter() throws IOException {
        // given
        String entryName = "dir_name";

        //noinspection rawtypes
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {
            archive.withFilter((name, p) -> false);

            // when
            archive.addDirectory(entryName);

            // then
            InOrder inOrder = inOrder(archive);
            inOrder.verify(archive).accept(entryName, null);
            mockArchive.verify(() -> sanitiseName(entryName));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldAddDirectoryWithNameAndModTime() throws IOException {
        // given
        String entryName = "dir_name";
        FileTime modTime = FileTime.from(Instant.now());

        //noinspection rawtypes
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {
            // when
            archive.addDirectory(entryName, modTime);

            // then
            verify(archive).accept(entryName, null);
            mockArchive.verify(() -> sanitiseName(entryName));
            verify(archive).writeDirectoryEntry(entryName, modTime);
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
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {

            // when
            archive.addDirectoryRecursively(base);

            // then
            verify(archive).addDirectoryRecursively("", base);
            mockArchive.verify(() -> sanitiseName("file1"), times(2));
            mockArchive.verify(() -> sanitiseName("subDir1"), times(2));
            mockArchive.verify(() -> sanitiseName(file11RelativeName), atLeastOnce());
            verify(archive).accept("subDir1", subDir1);
            FileTime subDir1ModTime = Files.getLastModifiedTime(subDir1);
            verify(archive).addDirectory(eq("subDir1"), assertArg(time -> assertThat(
                            time.toInstant().truncatedTo(ChronoUnit.SECONDS))
                    .isEqualTo(subDir1ModTime.toInstant().truncatedTo(ChronoUnit.SECONDS))));
            verify(archive).accept("subDir1", null);
            verify(archive).writeDirectoryEntry(eq("subDir1"), assertArg(time -> assertThat(
                            time.toInstant().truncatedTo(ChronoUnit.SECONDS))
                    .isEqualTo(subDir1ModTime.toInstant().truncatedTo(ChronoUnit.SECONDS))));
            verify(archive, times(2)).accept("subDir1/file11", file11);
            verify(archive)
                    .addFile(eq("subDir1/file11"), eq(file11), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file11ModTime = Files.getLastModifiedTime(file11);
            verify(archive)
                    .writeFileEntry(
                            eq("subDir1/file11"), any(InputStream.class), eq(2L), eq(file11ModTime), eq(file11Mode));
            verify(archive)
                    .writeFileEntry(
                            eq("subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(file11ModTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
            verify(archive, times(2)).accept("file1", file1);
            verify(archive).addFile(eq("file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file1ModTime = Files.getLastModifiedTime(file1);
            verify(archive)
                    .writeFileEntry(eq("file1"), any(InputStream.class), eq(1L), eq(file1ModTime), eq(file11Mode));
            verify(archive)
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
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {
            archive.withFilter((name, p) -> false);

            // when
            archive.addDirectoryRecursively(base);

            // then
            verify(archive).addDirectoryRecursively("", base);
            mockArchive.verify(() -> sanitiseName("file1"));
            mockArchive.verify(() -> sanitiseName("subDir1"));
            verify(archive).accept("subDir1", subDir1);
            verify(archive).accept("file1", file1);
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
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {

            // when
            archive.addDirectoryRecursively(top, base);

            // then
            mockArchive.verify(() -> sanitiseName("file1"));
            mockArchive.verify(() -> sanitiseName("subDir1"));
            mockArchive.verify(() -> sanitiseName(file11RelativeName));
            verify(archive).accept(top, null);
            FileTime baseModTime = Files.getLastModifiedTime(base);
            verify(archive).writeDirectoryEntry("top", baseModTime);
            verify(archive).accept(top, base);
            verify(archive).accept("top/subDir1", subDir1);
            FileTime subDir1ModTime = Files.getLastModifiedTime(subDir1);
            verify(archive).addDirectory(eq("top/subDir1"), assertArg(time -> assertThat(
                            time.toInstant().truncatedTo(ChronoUnit.SECONDS))
                    .isEqualTo(subDir1ModTime.toInstant().truncatedTo(ChronoUnit.SECONDS))));
            verify(archive).accept("top/subDir1", null);
            verify(archive).writeDirectoryEntry(eq("top/subDir1"), assertArg(time -> assertThat(
                            time.toInstant().truncatedTo(ChronoUnit.SECONDS))
                    .isEqualTo(subDir1ModTime.toInstant().truncatedTo(ChronoUnit.SECONDS))));
            verify(archive, times(2)).accept("top/subDir1/file11", file11);
            verify(archive)
                    .addFile(
                            eq("top/subDir1/file11"), eq(file11), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file11ModTime = Files.getLastModifiedTime(file11);
            verify(archive)
                    .writeFileEntry(
                            eq("top/subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(file11ModTime),
                            eq(file11Mode));
            verify(archive)
                    .writeFileEntry(
                            eq("top/subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(file11ModTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
            verify(archive, times(2)).accept("top/file1", file1);
            verify(archive).addFile(eq("top/file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file1ModTime = Files.getLastModifiedTime(file1);
            verify(archive)
                    .writeFileEntry(eq("top/file1"), any(InputStream.class), eq(1L), eq(file1ModTime), eq(file11Mode));
            verify(archive)
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
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {

            // when
            archive.addDirectoryRecursively(base, modTime);

            // then
            verify(archive).addDirectoryRecursively("", base, modTime);
            mockArchive.verify(() -> sanitiseName("file1"), times(2));
            mockArchive.verify(() -> sanitiseName("subDir1"), times(2));
            mockArchive.verify(() -> sanitiseName(file11RelativeName), atLeastOnce());
            verify(archive).accept("subDir1", subDir1);
            verify(archive).addDirectory("subDir1", modTime);
            verify(archive).accept("subDir1", null);
            verify(archive).writeDirectoryEntry("subDir1", modTime);
            verify(archive, times(2)).accept("subDir1/file11", file11);
            verify(archive)
                    .addFile(
                            eq("subDir1/file11"), eq(file11), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            verify(archive)
                    .writeFileEntry(eq("subDir1/file11"), any(InputStream.class), eq(2L), eq(modTime), eq(file11Mode));
            verify(archive)
                    .writeFileEntry(
                            eq("subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(modTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
            verify(archive, times(2)).accept("file1", file1);
            verify(archive).addFile(eq("file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            verify(archive).writeFileEntry(eq("file1"), any(InputStream.class), eq(1L), eq(modTime), eq(file11Mode));
            verify(archive)
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
        try (MockedStatic<ArchiveCreator> mockArchive = mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                InMemoryArchiveCreator archive =
                        spy(new InMemoryArchiveCreator(new InMemoryArchiveCreatorBuilder(out)))) {

            // when
            archive.addDirectoryRecursively(top, base, modTime);

            // then
            mockArchive.verify(() -> sanitiseName("file1"));
            mockArchive.verify(() -> sanitiseName("subDir1"));
            mockArchive.verify(() -> sanitiseName(file11RelativeName));
            verify(archive).addDirectoryRecursively(top, base, modTime);
            verify(archive).accept(top, base);
            verify(archive).addDirectory(top, modTime);
            verify(archive).accept(top, null);
            verify(archive).writeDirectoryEntry("top", modTime);
            verify(archive).accept("top/subDir1", subDir1);
            verify(archive).addDirectory("top/subDir1", modTime);
            verify(archive).accept("top/subDir1", null);
            verify(archive).writeDirectoryEntry("top/subDir1", modTime);
            verify(archive, times(2)).accept("top/subDir1/file11", file11);
            verify(archive)
                    .addFile(
                            eq("top/subDir1/file11"),
                            eq(file11),
                            any(BasicFileAttributes.class),
                            eq(Optional.of(modTime)));
            verify(archive)
                    .writeFileEntry(
                            eq("top/subDir1/file11"), any(InputStream.class), eq(2L), eq(modTime), eq(file11Mode));
            verify(archive)
                    .writeFileEntry(
                            eq("top/subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(modTime),
                            eq(file11Mode),
                            eq(Optional.empty()));
            verify(archive, times(2)).accept("top/file1", file1);
            verify(archive)
                    .addFile(eq("top/file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            verify(archive)
                    .writeFileEntry(eq("top/file1"), any(InputStream.class), eq(1L), eq(modTime), eq(file11Mode));
            verify(archive)
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
                        MockedStatic<ArchiveCreator> mockedArchive =
                                mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedArchive.when(ArchiveCreator::isIsOsWindows).thenReturn(false);
            var mockAttributeView = mock(PosixFileAttributeView.class);
            var mockedAttributes = mock(PosixFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, PosixFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.permissions()).thenReturn(Set.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ));

            // when
            int actualMode = ArchiveCreator.mode(mockPath);

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
                        MockedStatic<ArchiveCreator> mockedArchive =
                                mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedArchive.when(ArchiveCreator::isIsOsWindows).thenReturn(false);
            mockedFiles.when(() -> Files.getPosixFilePermissions(mockPath)).thenReturn(null);

            // when
            int actualMode = ArchiveCreator.mode(mockPath);

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
                        MockedStatic<ArchiveCreator> mockedArchive =
                                mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedArchive.when(ArchiveCreator::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            var mockedAttributes = mock(DosFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.isReadOnly()).thenReturn(true);
            when(mockedAttributes.isHidden()).thenReturn(true);

            // when
            int actualMode = ArchiveCreator.mode(mockPath);

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
                        MockedStatic<ArchiveCreator> mockedArchive =
                                mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedArchive.when(ArchiveCreator::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            var mockedAttributes = mock(DosFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.isReadOnly()).thenReturn(true);
            when(mockedAttributes.isHidden()).thenReturn(false);

            // when
            int actualMode = ArchiveCreator.mode(mockPath);

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
                        MockedStatic<ArchiveCreator> mockedArchive =
                                mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedArchive.when(ArchiveCreator::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            var mockedAttributes = mock(DosFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.isReadOnly()).thenReturn(false);
            when(mockedAttributes.isHidden()).thenReturn(true);

            // when
            int actualMode = ArchiveCreator.mode(mockPath);

            // then
            assertThat(actualMode).isEqualTo(expectedMode);
        }
    }

    @Test
    void shouldGetFileModeOnWindowsFileSystemWithAttributesAsFalse() throws IOException {
        // given
        var mockPath = mock(Path.class);

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<ArchiveCreator> mockedArchive =
                                mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedArchive.when(ArchiveCreator::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            var mockedAttributes = mock(DosFileAttributes.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            when(mockAttributeView.readAttributes()).thenReturn(mockedAttributes);
            when(mockedAttributes.isReadOnly()).thenReturn(false);
            when(mockedAttributes.isHidden()).thenReturn(false);

            // when
            int actualMode = ArchiveCreator.mode(mockPath);

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
                        MockedStatic<ArchiveCreator> mockedArchive =
                                mockStatic(ArchiveCreator.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedArchive.when(ArchiveCreator::isIsOsWindows).thenReturn(true);
            mockedFiles.when(() -> Files.getPosixFilePermissions(mockPath)).thenReturn(null);

            // when
            int actualMode = ArchiveCreator.mode(mockPath);

            // then
            assertThat(actualMode).isEqualTo(NO_MODE);
            Compress4JAssertions.assertThat(inMemoryLogAppender)
                    .contains("Cannot get DOS file attributes for: some/path", TRACE);
        }
    }

    @Test
    void shouldSetOptionOnArchiveOutputStream() throws IOException {
        // given
        var builder = new InMemoryArchiveCreatorBuilder(out).withSomeOption(42);
        try (InMemoryArchiveCreator compressor = new InMemoryArchiveCreator(builder)) {

            // when
            int actualOption = compressor.archiveOutputStream.getSomeOption();

            // then
            assertThat(actualOption).isEqualTo(42);
        }
    }
}
