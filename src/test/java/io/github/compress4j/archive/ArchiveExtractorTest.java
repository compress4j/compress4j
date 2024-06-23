/*
 * Copyright 2025 The Compress4J Project
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

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.TRACE;
import static io.github.compress4j.archive.ArchiveExtractor.Entry.Type.DIR;
import static io.github.compress4j.archive.ArchiveExtractor.Entry.Type.SYMLINK;
import static io.github.compress4j.archive.ArchiveExtractor.ErrorHandlerChoice.*;
import static io.github.compress4j.archive.ArchiveExtractor.EscapingSymlinkPolicy.DISALLOW;
import static io.github.compress4j.archive.ArchiveExtractor.EscapingSymlinkPolicy.RELATIVIZE_ABSOLUTE;
import static io.github.compress4j.test.util.io.TestFileUtils.createFile;
import static java.nio.file.attribute.PosixFilePermission.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.github.compress4j.archive.memory.InMemoryArchiveEntry;
import io.github.compress4j.archive.memory.InMemoryArchiveExtractor;
import io.github.compress4j.assertion.Compress4JAssertions;
import io.github.compress4j.test.util.log.InMemoryLogAppender;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ArchiveExtractorTest {

    private static final String LOGGER_NAME = ArchiveExtractor.class.getPackageName();
    private InMemoryLogAppender inMemoryLogAppender;

    @TempDir
    private Path tempDir;

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

    @Test
    void shouldExtractFiles() throws IOException {
        // given
        var entry1 =
                InMemoryArchiveEntry.builder().name("test1").content("content1").build();
        var entry2 =
                InMemoryArchiveEntry.builder().name("test2").content("content2").build();
        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {
            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test1")).hasContent("content1");
            assertThat(tempDir.resolve("test2")).hasContent("content2");
        }
    }

    @Test
    void shouldExtractFilesWithSubdirectories() throws IOException {
        // given
        var entry1 =
                InMemoryArchiveEntry.builder().name("test1").content("content1").build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test1")).hasContent("content1");
            assertThat(tempDir.resolve("subdir/test2")).hasContent("content2");
        }
    }

    @Test
    void shouldNotExtractFilesThatAlreadyExists() throws IOException {
        // given
        createFile(tempDir, "test1", "789");

        var entry1 =
                InMemoryArchiveEntry.builder().name("test1").content("content1").build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test1")).hasContent("789");
            assertThat(tempDir.resolve("subdir/test2")).hasContent("content2");
            Compress4JAssertions.assertThat(inMemoryLogAppender)
                    .contains("Skipping file entry: test1 (already exists)", DEBUG);
        }
    }

    @Test
    void shouldExtractFilesThatAlreadyExistsWhenOverwriteTrue() throws IOException {
        // given
        createFile(tempDir, "test1", "789");

        var entry1 =
                InMemoryArchiveEntry.builder().name("test1").content("content1").build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {
            inMemoryDecompressor.setOverwrite(true);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test1")).hasContent("content1");
            assertThat(tempDir.resolve("subdir/test2")).hasContent("content2");
        }
    }

    @Test
    void shouldExtractFilesWithSubdirectoriesAndStripZeroComponents() throws IOException {
        // given
        var entry1 =
                InMemoryArchiveEntry.builder().name("test1").content("content1").build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {
            inMemoryDecompressor.setStripComponents(0);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test1")).hasContent("content1");
            assertThat(tempDir.resolve("subdir/test2")).hasContent("content2");
        }
    }

    @Test
    void shouldExtractFilesWithSubdirectoriesAndStripComponents() throws IOException {
        // given
        var entry1 =
                InMemoryArchiveEntry.builder().name("test1").content("content1").build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {
            inMemoryDecompressor.setStripComponents(1);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test2")).hasContent("content2");
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPaths() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder()
                .name("../test1")
                .content("content1")
                .build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {

            // when && then
            assertThatThrownBy(() -> inMemoryDecompressor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Invalid entry name: ../test1");
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsRetries() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder()
                .name("../test1")
                .content("content1")
                .build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();
        AtomicInteger retries = new AtomicInteger(3);
        BiFunction<ArchiveExtractor.Entry, IOException, ArchiveExtractor.ErrorHandlerChoice> errorHandler =
                (entry, exception) -> {
                    if (retries.get() == 0) {
                        return ABORT;
                    }
                    retries.getAndDecrement();
                    return RETRY;
                };

        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {
            inMemoryDecompressor.setErrorHandler(errorHandler);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            Compress4JAssertions.assertThat(inMemoryLogAppender).contains("Retying because of exception", DEBUG);
            assertThat(tempDir).isEmptyDirectory();
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsAborts() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder()
                .name("../test1")
                .content("content1")
                .build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {
            inMemoryDecompressor.setErrorHandler((entry, exception) -> ABORT);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isEmptyDirectory();
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsAbortsAfterSomeFilesExtractedAlready() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder()
                .name("subdir/test1")
                .content("content1")
                .build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("../test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {
            inMemoryDecompressor.setErrorHandler((entry, exception) -> ABORT);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsBails() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder()
                .name("../test1")
                .content("content1")
                .build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {
            inMemoryDecompressor.setErrorHandler((entry, exception) -> BAIL_OUT);

            // when
            assertThatThrownBy(() -> inMemoryDecompressor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Invalid entry name: ../test1");

            // then
            assertThat(tempDir).isEmptyDirectory();
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsBailsAfterSomeFilesExtractedAlready() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder()
                .name("subdir/test1")
                .content("content1")
                .build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("../test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {
            inMemoryDecompressor.setErrorHandler((entry, exception) -> BAIL_OUT);

            // when
            assertThatThrownBy(() -> inMemoryDecompressor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Invalid entry name: ../test2");

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsSkipsEntry() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder()
                .name("subdir/../test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("subdir/some/../test1a")
                .content("content1a")
                .build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(entry1, entry1a, entry2))
                .build()) {
            inMemoryDecompressor.setErrorHandler((entry, exception) -> SKIP);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            Compress4JAssertions.assertThat(inMemoryLogAppender).contains("Skipped exception", DEBUG);
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test1")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1a")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test2")).hasContent("content2");
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsSkipAllEntries() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder()
                .name("subdir/../test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("subdir/some/../test1a")
                .content("content1a")
                .build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();
        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(entry1, entry1a, entry2))
                .build()) {
            inMemoryDecompressor.setErrorHandler((entry, exception) -> SKIP_ALL);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            Compress4JAssertions.assertThat(inMemoryLogAppender).contains("SKIP_ALL is selected", DEBUG);
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test1")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1a")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test2")).hasContent("content2");
        }
    }

    @Test
    void shouldApplyEntryFilters() throws IOException {
        // given
        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("subdir/some/test1a")
                .content("content1a")
                .build();
        var entry2 = InMemoryArchiveEntry.builder()
                .name("subdir/test2")
                .content("content2")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a, entry2))
                .build()) {
            inMemoryDecompressor.setEntryFilter(entry -> !entry.name.contains("some"));

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("subdir/test2")).hasContent("content2");
        }
    }

    @Test
    void shouldExtractSymlinksInAllowMode() throws IOException {
        // given
        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("test1a")
                .type(SYMLINK)
                .linkName("subdir/test1")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).isSymbolicLink().hasContent("content1");
        }
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void shouldExtractSymlinksInRelativizeAbsoluteMode() throws IOException {
        // given
        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("/subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("test1a")
                .type(SYMLINK)
                .linkName("/subdir/test1")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {
            inMemoryDecompressor.setEscapingSymlinkPolicy(RELATIVIZE_ABSOLUTE);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).isSymbolicLink().hasContent("content1");
        }
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void shouldNotExtractSymlinksInRelativizeAbsoluteModeWhenTargetPathRelative() throws IOException {
        // given
        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("/subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("test1a")
                .type(SYMLINK)
                .linkName("../subdir/test1")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {
            inMemoryDecompressor.setEscapingSymlinkPolicy(RELATIVIZE_ABSOLUTE);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).isSymbolicLink();
        }
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void shouldExtractSymlinksInDisallowModeWithAbsoluteTargetPathWhenWithinTargetOutputDir() throws IOException {
        // given
        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("/subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("test1a")
                .type(SYMLINK)
                .linkName("subdir/test1")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {
            inMemoryDecompressor.setEscapingSymlinkPolicy(DISALLOW);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).isSymbolicLink().exists();
        }
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void shouldNotExtractSymlinksInDisallowModeWithAbsoluteTargetPath() throws IOException {
        // given
        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("/subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("test1a")
                .type(SYMLINK)
                .linkName("/subdir/test1")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {
            inMemoryDecompressor.setEscapingSymlinkPolicy(DISALLOW);

            // when
            assertThatThrownBy(() -> inMemoryDecompressor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Invalid symlink (absolute path): test1a -> /subdir/test1");

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).doesNotExist();
        }
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void shouldNotExtractSymlinksInDisallowModeWithAbsoluteTargetPathTargetOutsideOutput() throws IOException {
        // given
        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("/subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("test1a")
                .type(SYMLINK)
                .linkName("../subdir2/test1")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {
            inMemoryDecompressor.setEscapingSymlinkPolicy(DISALLOW);

            // when
            assertThatThrownBy(() -> inMemoryDecompressor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Invalid symlink (points outside of output directory): test1a -> ../subdir2/test1");

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).doesNotExist();
        }
    }

    @Test
    void shouldNotExtractSymlinksWhenTargetNull() throws IOException {
        // given
        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("/subdir/test1")
                .content("content1")
                .build();
        var entry1a =
                InMemoryArchiveEntry.builder().name("test1a").type(SYMLINK).build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {

            // when
            assertThatThrownBy(() -> inMemoryDecompressor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Invalid symlink entry: test1a (empty target)");

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).doesNotExist();
        }
    }

    @Test
    void shouldNotExtractSymlinksWhenTargetBlank() throws IOException {
        // given
        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("/subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("test1a")
                .type(SYMLINK)
                .linkName("")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {

            // when
            assertThatThrownBy(() -> inMemoryDecompressor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Invalid symlink entry: test1a (empty target)");

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).doesNotExist();
        }
    }

    @Test
    void shouldNotExtractSymlinksWhenTargetFileDoesExistsAndOverwriteFalse() throws IOException {
        // given
        createFile(tempDir, "test1a", "789");

        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("/subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("test1a")
                .type(SYMLINK)
                .linkName("some")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).isRegularFile().hasContent("789");
            Compress4JAssertions.assertThat(inMemoryLogAppender)
                    .contains("Skipping symlink entry: test1a -> some (already exists)", DEBUG);
        }
    }

    @Test
    void shouldExtractSymlinksWhenTargetFileDoesExistsAndOverwriteTrue() throws IOException {
        // given
        createFile(tempDir, "test1a", "789");

        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("test1a")
                .type(SYMLINK)
                .linkName("subdir/test1")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {
            inMemoryDecompressor.setOverwrite(true);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).isSymbolicLink().hasContent("content1");
        }
    }

    @Test
    void shouldRunBiConsumerPostProcessor() throws IOException {
        // given
        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("test1a")
                .type(SYMLINK)
                .linkName("subdir/test1")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {
            AtomicInteger counter = new AtomicInteger();
            inMemoryDecompressor.setPostProcessor((entry, path) -> counter.incrementAndGet());

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(counter).hasValue(3);
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).isSymbolicLink().hasContent("content1");
        }
    }

    @Test
    void shouldRunConsumerPostProcessor() throws IOException {
        // given
        var subdir = InMemoryArchiveEntry.builder().name("subdir").type(DIR).build();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("subdir/test1")
                .content("content1")
                .build();
        var entry1a = InMemoryArchiveEntry.builder()
                .name("test1a")
                .type(SYMLINK)
                .linkName("subdir/test1")
                .build();

        try (InMemoryArchiveExtractor inMemoryDecompressor = InMemoryArchiveExtractor.builder(
                        List.of(subdir, entry1, entry1a))
                .build()) {
            AtomicInteger counter = new AtomicInteger();
            inMemoryDecompressor.setPostProcessor(path -> counter.incrementAndGet());

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            assertThat(counter).hasValue(3);
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).isSymbolicLink().hasContent("content1");
        }
    }

    @Test
    void shouldNormalizePathAndSplit() throws IOException {
        // given
        var path = "some/path";

        // when
        var result = ArchiveExtractor.normalizePathAndSplit(path);

        // then
        assertThat(result).isNotEmpty();
    }

    @Test
    void shouldSetAttributesOnNixFileSystem() throws IOException {
        // given
        var mockPath = mock(Path.class);
        @SuppressWarnings("OctalInteger")
        int mode = 0644;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<ArchiveExtractor> mockedCompressor =
                                mockStatic(ArchiveExtractor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(ArchiveExtractor::isIsOsWindows).thenReturn(false);
            var mockAttributeView = mock(PosixFileAttributeView.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, PosixFileAttributeView.class))
                    .thenReturn(mockAttributeView);

            // when
            ArchiveExtractor.setAttributes(mode, mockPath);

            // then
            verify(mockAttributeView).setPermissions(Set.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ));
        }
    }

    @Test
    void shouldNotSetAttributesOnNixFileSystemWhenCouldNotReadExistingAttributes() throws IOException {
        // given
        var mockPath = mock(Path.class);
        given(mockPath.toString()).willReturn("some/path");
        @SuppressWarnings("OctalInteger")
        int mode = 0644;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<ArchiveExtractor> mockedCompressor =
                                mockStatic(ArchiveExtractor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(ArchiveExtractor::isIsOsWindows).thenReturn(false);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, PosixFileAttributeView.class))
                    .thenReturn(null);

            // when
            ArchiveExtractor.setAttributes(mode, mockPath);

            // then
            Compress4JAssertions.assertThat(inMemoryLogAppender)
                    .contains("Cannot set POSIX attributes for file: some/path", TRACE);
        }
    }

    @Test
    void shouldSetAttributesOnWindowsFileSystem() throws IOException {
        // given
        var mockPath = mock(Path.class);
        @SuppressWarnings("OctalInteger")
        int mode = 0003;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<ArchiveExtractor> mockedCompressor =
                                mockStatic(ArchiveExtractor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(ArchiveExtractor::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);

            // when
            ArchiveExtractor.setAttributes(mode, mockPath);

            // then
            verify(mockAttributeView).setReadOnly(true);
            verify(mockAttributeView).setHidden(true);
        }
    }

    @Test
    void shouldSetAttributesOnWindowsFileSystemWhenReadOnly() throws IOException {
        // given
        var mockPath = mock(Path.class);
        @SuppressWarnings("OctalInteger")
        int mode = 0001;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<ArchiveExtractor> mockedCompressor =
                                mockStatic(ArchiveExtractor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(ArchiveExtractor::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);

            // when
            ArchiveExtractor.setAttributes(mode, mockPath);

            // then
            verify(mockAttributeView).setReadOnly(true);
            verifyNoMoreInteractions(mockAttributeView);
        }
    }

    @Test
    void shouldSetAttributesOnWindowsFileSystemWhenHidden() throws IOException {
        // given
        var mockPath = mock(Path.class);
        @SuppressWarnings("OctalInteger")
        int mode = 0002;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<ArchiveExtractor> mockedCompressor =
                                mockStatic(ArchiveExtractor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(ArchiveExtractor::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);

            // when
            ArchiveExtractor.setAttributes(mode, mockPath);

            // then
            verify(mockAttributeView).setHidden(true);
            verifyNoMoreInteractions(mockAttributeView);
        }
    }

    @Test
    void shouldNotSetAttributesOnWindowsFileSystemWhenModeZero() throws IOException {
        // given
        var mockPath = mock(Path.class);
        @SuppressWarnings("OctalInteger")
        int mode = 0000;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<ArchiveExtractor> mockedCompressor =
                                mockStatic(ArchiveExtractor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(ArchiveExtractor::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);

            // when
            ArchiveExtractor.setAttributes(mode, mockPath);

            // then
            verifyNoInteractions(mockAttributeView);
        }
    }

    @Test
    void shouldNotSetAttributesOnWindowsFileSystemWithoutFileAttributes() throws IOException {
        // given
        var mockPath = mock(Path.class);
        given(mockPath.toString()).willReturn("some/path");
        @SuppressWarnings("OctalInteger")
        int mode = 0003;

        try (@SuppressWarnings("rawtypes")
                        MockedStatic<ArchiveExtractor> mockedCompressor =
                                mockStatic(ArchiveExtractor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedCompressor.when(ArchiveExtractor::isIsOsWindows).thenReturn(true);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(null);

            // when
            ArchiveExtractor.setAttributes(mode, mockPath);

            // then
            Compress4JAssertions.assertThat(inMemoryLogAppender)
                    .contains("Cannot set DOS attributes for file: some/path", TRACE);
        }
    }
}
