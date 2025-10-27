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
package io.github.compress4j.archivers;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.TRACE;
import static io.github.compress4j.archivers.ArchiveExtractor.Entry.Type.DIR;
import static io.github.compress4j.archivers.ArchiveExtractor.Entry.Type.FILE;
import static io.github.compress4j.archivers.ArchiveExtractor.Entry.Type.SYMLINK;
import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.ABORT;
import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.BAIL_OUT;
import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.RETRY;
import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.SKIP;
import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.SKIP_ALL;
import static io.github.compress4j.archivers.ArchiveExtractor.EscapingSymlinkPolicy.ALLOW;
import static io.github.compress4j.archivers.ArchiveExtractor.EscapingSymlinkPolicy.DISALLOW;
import static io.github.compress4j.archivers.ArchiveExtractor.EscapingSymlinkPolicy.RELATIVIZE_ABSOLUTE;
import static io.github.compress4j.archivers.memory.InMemoryArchiveInputStream.toInputStream;
import static io.github.compress4j.test.util.io.TestFileUtils.createFile;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.github.compress4j.archivers.ArchiveExtractor.Entry;
import io.github.compress4j.archivers.memory.InMemoryArchiveEntry;
import io.github.compress4j.archivers.memory.InMemoryArchiveExtractor;
import io.github.compress4j.archivers.memory.InMemoryArchiveExtractor.InMemoryArchiveExtractorBuilder;
import io.github.compress4j.archivers.memory.InMemoryArchiveInputStream;
import io.github.compress4j.assertion.Compress4JAssertions;
import io.github.compress4j.test.util.log.InMemoryLogAppender;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
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
        var logger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
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
        try (var inMemoryDecompressor =
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
        try (var inMemoryDecompressor =
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
        try (var inMemoryDecompressor =
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
        try (var inMemoryDecompressor =
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
        try (var inMemoryDecompressor =
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
        try (var inMemoryDecompressor =
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
        try (var inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {

            // when && then
            assertThatThrownBy(() -> inMemoryDecompressor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessageStartingWith("Path traversal vulnerability detected!");
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
        var retries = new AtomicInteger(3);
        BiFunction<ArchiveExtractor.Entry, IOException, ArchiveExtractor.ErrorHandlerChoice> errorHandler =
                (entry, exception) -> {
                    if (retries.get() == 0) {
                        return ABORT;
                    }
                    retries.getAndDecrement();
                    return RETRY;
                };

        try (var inMemoryDecompressor =
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
        try (var inMemoryDecompressor =
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
        try (var inMemoryDecompressor =
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
        try (var inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {
            inMemoryDecompressor.setErrorHandler((entry, exception) -> BAIL_OUT);

            // when
            assertThatThrownBy(() -> inMemoryDecompressor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessageStartingWith("Path traversal vulnerability detected!");

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
        try (var inMemoryDecompressor =
                InMemoryArchiveExtractor.builder(List.of(entry1, entry2)).build()) {
            inMemoryDecompressor.setErrorHandler((entry, exception) -> BAIL_OUT);

            // when
            assertThatThrownBy(() -> inMemoryDecompressor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessageStartingWith("Path traversal vulnerability detected!");

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
        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(entry1, entry1a, entry2))
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
        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(entry1, entry1a, entry2))
                .build()) {
            inMemoryDecompressor.setErrorHandler((entry, exception) -> SKIP_ALL);

            // when
            inMemoryDecompressor.extract(tempDir);

            // then
            Compress4JAssertions.assertThat(inMemoryLogAppender).contains("SKIP_ALL is selected", DEBUG);
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test1")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1a")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test2")).doesNotExist();
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a, entry2))
                .build()) {
            inMemoryDecompressor.setEntryFilter(entry -> !entry.name().contains("some"));

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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
                .build()) {
            var counter = new AtomicInteger();
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

        try (var inMemoryDecompressor = InMemoryArchiveExtractor.builder(List.of(subdir, entry1, entry1a))
                .build()) {
            var counter = new AtomicInteger();
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
    void shouldSetAttributesOnNixFileSystem() throws IOException {
        // given
        var mockPath = mock(Path.class);
        @SuppressWarnings("OctalInteger")
        var mode = 0644;

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
        var mode = 0644;

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
        var mode = 0003;

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
        var mode = 0001;

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
        var mode = 0002;

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
        var mode = 0000;

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
        var mode = 0003;

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

    @Test
    void shouldExtractEmptyArchiveWithoutErrors() throws IOException {
        // given
        try (var extractor =
                InMemoryArchiveExtractor.builder(Collections.emptyList()).build()) {
            // when
            extractor.extract(tempDir);
            // then
            assertThat(tempDir).isEmptyDirectory();
            Compress4JAssertions.assertThat(inMemoryLogAppender).isEmpty();
        }
    }

    @Test
    void shouldExtractArchiveWithOnlyDirectories() throws IOException {
        // given
        var dirEntry =
                InMemoryArchiveEntry.builder().name("emptyDir/").type(DIR).build();
        var nestedDirEntry = InMemoryArchiveEntry.builder()
                .name("parentDir/childDir/")
                .type(DIR)
                .build();
        try (var extractor = InMemoryArchiveExtractor.builder(List.of(dirEntry, nestedDirEntry))
                .build()) {
            // when
            extractor.extract(tempDir);
            // then
            assertThat(tempDir.resolve("emptyDir")).exists().isDirectory();
            assertThat(tempDir.resolve("parentDir/childDir")).exists().isDirectory();
        }
    }

    @Test
    void shouldCorrectlyStripAllComponentsLeavingNoFilesIfNameMatchesComponents() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder()
                .name("a/b.txt")
                .content("content")
                .build();
        try (var extractor = InMemoryArchiveExtractor.builder(List.of(entry1)).build()) {
            extractor.setStripComponents(2);
            // when
            extractor.extract(tempDir);
            // then
            assertThat(tempDir).isEmptyDirectory();
            Compress4JAssertions.assertThat(inMemoryLogAppender).isEmpty(); // No error, just skipped
        }
    }

    @Test
    void shouldCorrectlyStripComponentsForDirectoryEntryMakingItTopLevel() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder().name("a/b/").type(DIR).build();
        try (var extractor = InMemoryArchiveExtractor.builder(List.of(entry1)).build()) {
            extractor.setStripComponents(1);
            // when
            extractor.extract(tempDir);
            // then
            assertThat(tempDir.resolve("b")).exists().isDirectory();
        }
    }

    @Test
    void shouldSkipEntryIfAllItsPathComponentsAreStripped() throws IOException {
        // given
        var entry =
                InMemoryArchiveEntry.builder().name("a/b/c.txt").content("test").build();
        try (var extractor = InMemoryArchiveExtractor.builder(List.of(entry)).build()) {
            extractor.setStripComponents(3);
            // when
            extractor.extract(tempDir);
            // then
            assertThat(tempDir).isEmptyDirectory();
            Compress4JAssertions.assertThat(inMemoryLogAppender).isEmpty(); // No file operations
        }
    }

    @Test
    void shouldCorrectlyHandleStripComponentsGreaterThanPathDepth() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder()
                .name("a/b.txt")
                .content("content")
                .build();
        try (var extractor = InMemoryArchiveExtractor.builder(List.of(entry1)).build()) {
            extractor.setStripComponents(3);
            // when
            extractor.extract(tempDir);
            // then
            assertThat(tempDir).isEmptyDirectory();
            Compress4JAssertions.assertThat(inMemoryLogAppender).isEmpty();
        }
    }

    @Test
    void shouldCreateOutputDirIfItDoesNotExist() throws IOException {
        // given
        var newOutputDir = tempDir.resolve("new_output_dir");
        assertThat(newOutputDir).doesNotExist();
        var entry1 = InMemoryArchiveEntry.builder()
                .name("file.txt")
                .content("content")
                .build();

        try (var extractor = InMemoryArchiveExtractor.builder(List.of(entry1)).build()) {
            // when
            extractor.extract(newOutputDir);
            // then
            assertThat(newOutputDir).exists().isDirectory();
            assertThat(newOutputDir.resolve("file.txt")).hasContent("content");
        }
    }

    @Test
    void shouldFailIfOutputDirIsAFile() throws IOException {
        // given
        var fileAsOutputDir = tempDir.resolve("iam_a_file.txt");
        Files.createFile(fileAsOutputDir);
        var entry1 = InMemoryArchiveEntry.builder()
                .name("file.txt")
                .content("content")
                .build();

        try (var extractor = InMemoryArchiveExtractor.builder(List.of(entry1)).build()) {
            // when & then
            assertThatThrownBy(() -> extractor.extract(fileAsOutputDir))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining(fileAsOutputDir.getFileName().toString()); // Or more specific if possible
        }
    }

    @Test
    void shouldHandleRuntimeExceptionDuringNextEntryAndBailOut() throws IOException {
        var entry1 =
                InMemoryArchiveEntry.builder().name("file1.txt").content("abc").build();
        List<InMemoryArchiveEntry> entries = new ArrayList<>();
        entries.add(entry1);
        var inputStream = toInputStream(entries);
        var faultInjectingBuilder = new InMemoryArchiveExtractorBuilder(inputStream) {
            private int callCount = 0;

            @Override
            public InMemoryArchiveInputStream buildArchiveInputStream() {
                return new InMemoryArchiveInputStream(entries) {
                    @Override
                    public InMemoryArchiveEntry getNextEntry() {
                        callCount++;
                        if (callCount == 2) {
                            throw new RuntimeException("Simulated error reading next entry");
                        }
                        return super.getNextEntry();
                    }
                };
            }
        };

        try (var extractor = faultInjectingBuilder.build()) {
            extractor.setErrorHandler((entry, ex) -> BAIL_OUT); // Default, but explicit

            // when & then
            assertThatThrownBy(() -> extractor.extract(tempDir))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Simulated error reading next entry");

            assertThat(tempDir.resolve("file1.txt")).hasContent("abc");
        }
    }

    @Test
    void shouldHandleIOExceptionDuringFileWriteAndUseRetryErrorHandler() throws IOException {
        // given
        var entry1 = InMemoryArchiveEntry.builder()
                .name("file_to_fail.txt")
                .content("content")
                .build();
        var entry2 =
                InMemoryArchiveEntry.builder().name("success.txt").content("ok").build();

        var simulatedException = new IOException("Simulated disk full");

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class, CALLS_REAL_METHODS);
                var extractor = InMemoryArchiveExtractor.builder(List.of(entry1, entry2))
                        .build()) {

            //noinspection resource
            mockedFiles
                    .when(() -> Files.newOutputStream(eq(tempDir.resolve("file_to_fail.txt"))))
                    .thenThrow(simulatedException)
                    .thenCallRealMethod();

            extractor.setErrorHandler((entry, ex) -> {
                if (entry.name().equals("file_to_fail.txt") && ex == simulatedException) {
                    return RETRY;
                }
                return BAIL_OUT;
            });
            extractor.setOverwrite(true);

            // when
            extractor.extract(tempDir);

            // then
            assertThat(tempDir.resolve("file_to_fail.txt")).exists().hasContent("content");
            assertThat(tempDir.resolve("success.txt")).hasContent("ok");
            Compress4JAssertions.assertThat(inMemoryLogAppender).contains("Retying because of exception", DEBUG);
        }
    }

    @SuppressWarnings("resource")
    @Test
    void shouldHandleIOExceptionDuringFileWriteAndUseSkipErrorHandler() throws IOException {
        // given
        var entryToFail = InMemoryArchiveEntry.builder()
                .name("file_to_fail.txt")
                .content("content")
                .build();
        var entryToSucceed =
                InMemoryArchiveEntry.builder().name("success.txt").content("ok").build();
        var simulatedException = new AccessDeniedException("Simulated permission error");

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class, CALLS_REAL_METHODS);
                var extractor = InMemoryArchiveExtractor.builder(List.of(entryToFail, entryToSucceed))
                        .build()) {

            mockedFiles
                    .when(() -> Files.newOutputStream(eq(tempDir.resolve(entryToFail.getName()))))
                    .thenThrow(simulatedException);

            extractor.setErrorHandler((entry, ex) -> {
                if (entry.name().equals(entryToFail.getName()) && ex == simulatedException) {
                    return SKIP;
                }
                return BAIL_OUT;
            });

            // when
            extractor.extract(tempDir);

            // then
            assertThat(tempDir.resolve(entryToFail.getName())).doesNotExist();
            assertThat(tempDir.resolve(entryToSucceed.getName())).hasContent("ok");
            Compress4JAssertions.assertThat(inMemoryLogAppender)
                    .contains("Skipped exception", DEBUG, simulatedException);
        }
    }

    @SuppressWarnings("resource")
    @Test
    void shouldHandleIOExceptionDuringFileWriteAndUseSkipAllErrorHandler() throws IOException {
        // given
        var entryToFail = InMemoryArchiveEntry.builder()
                .name("file_to_fail.txt")
                .content("content")
                .build();
        var anotherEntry = InMemoryArchiveEntry.builder()
                .name("another.txt")
                .content("another")
                .build();
        var thirdEntry = InMemoryArchiveEntry.builder()
                .name("third.txt")
                .content("third")
                .build();
        var simulatedException = new IOException("Simulated disk full");

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class, CALLS_REAL_METHODS);
                var extractor = InMemoryArchiveExtractor.builder(List.of(entryToFail, anotherEntry, thirdEntry))
                        .build()) {

            mockedFiles
                    .when(() -> Files.newOutputStream(eq(tempDir.resolve(entryToFail.getName()))))
                    .thenThrow(simulatedException);

            extractor.setErrorHandler((entry, ex) -> {
                if (entry.name().equals(entryToFail.getName()) && ex == simulatedException) {
                    return SKIP_ALL;
                }
                return BAIL_OUT;
            });

            // when
            extractor.extract(tempDir);

            // then
            assertThat(tempDir.resolve(entryToFail.getName())).doesNotExist();
            assertThat(tempDir.resolve(anotherEntry.getName())).doesNotExist();
            assertThat(tempDir.resolve(thirdEntry.getName())).doesNotExist();

            Compress4JAssertions.assertThat(inMemoryLogAppender)
                    .contains("SKIP_ALL is selected", DEBUG, simulatedException);
            mockedFiles.verify(() -> Files.newOutputStream(eq(tempDir.resolve(entryToFail.getName()))));
        }
    }

    @Test
    void shouldNotOverwriteExistingDirectoryWithFileWhenOverwriteTrue() throws IOException {
        // given
        var existingDir = tempDir.resolve("entryName");
        Files.createDirectories(existingDir);
        var fileEntry = InMemoryArchiveEntry.builder()
                .name("entryName")
                .content("file content")
                .build();

        try (var extractor =
                InMemoryArchiveExtractor.builder(List.of(fileEntry)).build()) {
            extractor.setOverwrite(true);
            // when
            assertThatThrownBy(() -> extractor.extract(tempDir)).isInstanceOf(FileSystemException.class);

            // then
            assertThat(tempDir.resolve("entryName")).isDirectory();
        }
    }

    @Test
    void shouldFailToOverwriteExistingDirectoryWithFileWhenOverwriteFalse() throws IOException {
        // given
        var existingDir = tempDir.resolve("entryName");
        Files.createDirectories(existingDir);
        var fileEntry = InMemoryArchiveEntry.builder()
                .name("entryName")
                .content("file content")
                .build();

        try (var extractor =
                InMemoryArchiveExtractor.builder(List.of(fileEntry)).build()) {
            extractor.setOverwrite(false);
            // when
            extractor.extract(tempDir);

            // then
            assertThat(tempDir.resolve("entryName")).isDirectory();
            Compress4JAssertions.assertThat(inMemoryLogAppender)
                    .contains("Skipping file entry: entryName (already exists)", DEBUG);
        }
    }

    @Test
    void shouldNotOverwriteExistingFileWithDirectoryWhenOverwriteTrue() throws IOException {
        // given
        var existingFile = tempDir.resolve("entryName");
        Files.writeString(existingFile, "i am a file");
        var dirEntry =
                InMemoryArchiveEntry.builder().name("entryName").type(DIR).build();

        try (var extractor = InMemoryArchiveExtractor.builder(List.of(dirEntry)).build()) {
            extractor.setOverwrite(true);
            // when
            extractor.extract(tempDir);
            // then
            assertThat(tempDir.resolve("entryName")).isRegularFile();
        }
    }

    @Test
    void shouldAllowSymlinkToAbsoluteExternalPathWhenPolicyAllow() throws IOException {
        // given
        var externalTarget = tempDir.resolve("external_target.txt");
        Files.writeString(externalTarget, "external content");
        var symlinkEntry = InMemoryArchiveEntry.builder()
                .name("myLink")
                .type(SYMLINK)
                .linkName(externalTarget.toAbsolutePath().toString())
                .build();

        try (var extractor =
                InMemoryArchiveExtractor.builder(List.of(symlinkEntry)).build()) {
            extractor.setEscapingSymlinkPolicy(ALLOW);
            // when
            extractor.extract(tempDir);
            // then
            var linkPath = tempDir.resolve("myLink");
            assertThat(linkPath).isSymbolicLink();
            assertThat(Files.readSymbolicLink(linkPath)).isEqualTo(externalTarget.toAbsolutePath());
            if (!OS.WINDOWS.isCurrentOs()) {
                assertThat(Files.readString(linkPath)).isEqualTo("external content");
            }
        } finally {
            Files.deleteIfExists(externalTarget);
        }
    }

    @Test
    void shouldExtractSymlinkToDirectory() throws IOException {
        // given
        var dirToLinkTo =
                InMemoryArchiveEntry.builder().name("actualDir").type(DIR).build();
        var symlinkEntry = InMemoryArchiveEntry.builder()
                .name("linkToDir")
                .type(SYMLINK)
                .linkName("actualDir")
                .build();

        try (var extractor = InMemoryArchiveExtractor.builder(List.of(dirToLinkTo, symlinkEntry))
                .build()) {
            // when
            extractor.extract(tempDir);
            // then
            var linkPath = tempDir.resolve("linkToDir");
            assertThat(tempDir.resolve("actualDir")).isDirectory();
            assertThat(linkPath).isSymbolicLink();
            assertThat(Files.readSymbolicLink(linkPath).toString().replace('\\', '/'))
                    .isEqualTo("actualDir");

            var resolvedLinkPath = Files.readSymbolicLink(linkPath);
            assertThat(tempDir.resolve(resolvedLinkPath)).isDirectory();
        }
    }

    @Test
    void shouldExtractSymlinkToNonExistentTarget() throws IOException {
        // given
        var symlinkEntry = InMemoryArchiveEntry.builder()
                .name("linkToNowhere")
                .type(SYMLINK)
                .linkName("non_existent_target")
                .build();

        try (var extractor =
                InMemoryArchiveExtractor.builder(List.of(symlinkEntry)).build()) {
            // when
            extractor.extract(tempDir);
            // then
            var linkPath = tempDir.resolve("linkToNowhere");
            assertThat(linkPath).isSymbolicLink();
            assertThat(Files.readSymbolicLink(linkPath).toString().replace('\\', '/'))
                    .isEqualTo("non_existent_target");
            assertThat(Files.exists(linkPath, LinkOption.NOFOLLOW_LINKS)).isTrue();
            assertThat(Files.exists(linkPath)).isFalse();
        }
    }

    @Test
    void entryConstructorShouldNormalizeAndTrimPaths() {
        assertThat(new Entry(" /a/b/ ", FILE, 0, null).name()).isEqualTo("a/b");
        assertThat(new Entry("\\a\\b\\", DIR, 0, null).name()).isEqualTo("a/b");
        assertThat(new Entry("a/b/", FILE, 0, null).name()).isEqualTo("a/b");
        assertThat(new Entry("a/b/", DIR, 0, null).name()).isEqualTo("a/b");
        assertThat(new Entry("///", DIR, 0, null).name()).isEmpty();
        assertThat(new Entry("", FILE, 0, null).name()).isEmpty();
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test
    void setAttributesShouldSetAllRelevantDosAttributesOnWindows() throws IOException {
        var mockPath = mock(Path.class);
        var mode = io.github.compress4j.utils.FileUtils.DOS_READ_ONLY | io.github.compress4j.utils.FileUtils.DOS_HIDDEN;

        //noinspection rawtypes
        try (MockedStatic<ArchiveExtractor> mockedExtractor = mockStatic(ArchiveExtractor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {

            mockedExtractor.when(ArchiveExtractor::isIsOsWindows).thenReturn(true);
            var mockAttributeView = mock(DosFileAttributeView.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, DosFileAttributeView.class))
                    .thenReturn(mockAttributeView);

            ArchiveExtractor.setAttributes(mode, mockPath);

            verify(mockAttributeView).setReadOnly(true);
            verify(mockAttributeView).setHidden(true);
            verifyNoMoreInteractions(mockAttributeView);
        }
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void setAttributesShouldSetComplexPosixPermissionsOnNix() throws IOException {
        var mockPath = mock(Path.class);
        // rwxr-xr-x with setuid
        @SuppressWarnings("OctalInteger")
        var mode = 04755; // setuid + rwxr-xr-x

        //noinspection rawtypes
        try (MockedStatic<ArchiveExtractor> mockedExtractor = mockStatic(ArchiveExtractor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedExtractor.when(ArchiveExtractor::isIsOsWindows).thenReturn(false);
            var mockAttributeView = mock(PosixFileAttributeView.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, PosixFileAttributeView.class))
                    .thenReturn(mockAttributeView);

            ArchiveExtractor.setAttributes(mode, mockPath);

            // Expected permissions from fromUnixMode(04755)
            Set<java.nio.file.attribute.PosixFilePermission> expectedPermissions =
                    PosixFilePermissions.fromString("rwxr-xr-x");

            verify(mockAttributeView).setPermissions(expectedPermissions);
        }
    }

    @Test
    void builderShouldCorrectlySetNullEntryFilter() throws IOException {
        InMemoryArchiveExtractor.ArchiveExtractorBuilder<?, ?, ?> builder =
                InMemoryArchiveExtractor.builder(Collections.emptyList());
        builder.filter(null);

        var entry1 = InMemoryArchiveEntry.builder().name("test1").content("c1").build();
        try (var extractor =
                InMemoryArchiveExtractor.builder(List.of(entry1)).filter(null).build()) {
            extractor.extract(tempDir);
            assertThat(tempDir.resolve("test1")).exists();
        }
    }

    @Test
    void shouldNotExtractSymlinksInDisallowModeWhenTargetIsAbsoluteEvenIfResolvesInside() throws IOException {
        var entry1 = InMemoryArchiveEntry.builder()
                .name("file.txt")
                .content("content")
                .build();
        var symlinkEntry = InMemoryArchiveEntry.builder()
                .name("absLink")
                .type(SYMLINK)
                .linkName(tempDir.resolve("file.txt").toAbsolutePath().toString())
                .build();

        try (var extractor = InMemoryArchiveExtractor.builder(List.of(entry1, symlinkEntry))
                .escapingSymlinkPolicy(DISALLOW)
                .build()) {

            assertThatThrownBy(() -> extractor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Invalid symlink (absolute path): absLink -> ");

            assertThat(tempDir.resolve("file.txt")).hasContent("content");
            assertThat(tempDir.resolve("absLink")).doesNotExist();
        }
    }

    @Test
    void shouldExtractMixedEntriesWithAllTypesAndPolicies() throws IOException {
        var tempDir = Files.createTempDirectory("test");
        var dirEntry = InMemoryArchiveEntry.builder().name("adir").type(DIR).build();
        var fileEntry = InMemoryArchiveEntry.builder()
                .name("adir/file.txt")
                .content("f")
                .build();
        var symlinkEntry = InMemoryArchiveEntry.builder()
                .name("adir/link")
                .type(SYMLINK)
                .linkName("file.txt")
                .build();
        var absSymlink = InMemoryArchiveEntry.builder()
                .name("absLink")
                .type(SYMLINK)
                .linkName(tempDir.resolve("adir/file.txt").toAbsolutePath().toString())
                .build();

        // ALLOW policy: absolute symlink is allowed
        try (var extractor = InMemoryArchiveExtractor.builder(List.of(dirEntry, fileEntry, symlinkEntry, absSymlink))
                .escapingSymlinkPolicy(ALLOW)
                .build()) {
            extractor.extract(tempDir);
            assertThat(tempDir.resolve("adir")).isDirectory();
            assertThat(tempDir.resolve("adir/file.txt")).hasContent("f");
            assertThat(tempDir.resolve("adir/link")).isSymbolicLink();
            assertThat(tempDir.resolve("absLink")).isSymbolicLink();
        }

        // DISALLOW policy: absolute symlink should fail
        try (var extractor = InMemoryArchiveExtractor.builder(List.of(dirEntry, fileEntry, absSymlink))
                .escapingSymlinkPolicy(DISALLOW)
                .build()) {
            assertThatThrownBy(() -> extractor.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Invalid symlink (absolute path)");
        }
    }

    @Test
    void shouldNotOverwriteSymlinkWithFileOrDirWhenOverwriteFalse() throws IOException {
        var dirEntry = InMemoryArchiveEntry.builder().name("adir").type(DIR).build();
        var fileEntry = InMemoryArchiveEntry.builder()
                .name("adir/file.txt")
                .content("f")
                .build();
        var symlinkEntry = InMemoryArchiveEntry.builder()
                .name("adir/link")
                .type(SYMLINK)
                .linkName("file.txt")
                .build();
        try (var extractor = InMemoryArchiveExtractor.builder(List.of(dirEntry, fileEntry, symlinkEntry))
                .build()) {
            extractor.extract(tempDir);
        }
        var fileAtSymlink =
                InMemoryArchiveEntry.builder().name("adir/link").content("new").build();
        try (var extractor =
                InMemoryArchiveExtractor.builder(List.of(fileAtSymlink)).build()) {
            extractor.setOverwrite(false);
            extractor.extract(tempDir);
            assertThat(tempDir.resolve("adir/link")).isSymbolicLink();
        }
    }

    @Test
    void shouldOverwriteSymlinkWithFileWhenOverwriteTrue() throws IOException {
        var dirEntry = InMemoryArchiveEntry.builder().name("adir").type(DIR).build();
        var fileEntry = InMemoryArchiveEntry.builder()
                .name("adir/file.txt")
                .content("f")
                .build();
        var symlinkEntry = InMemoryArchiveEntry.builder()
                .name("adir/link")
                .type(SYMLINK)
                .linkName("file.txt")
                .build();
        try (var extractor = InMemoryArchiveExtractor.builder(List.of(dirEntry, fileEntry, symlinkEntry))
                .build()) {
            extractor.extract(tempDir);
        }
        var fileAtSymlink =
                InMemoryArchiveEntry.builder().name("adir/link").content("new").build();
        try (var extractor =
                InMemoryArchiveExtractor.builder(List.of(fileAtSymlink)).build()) {
            extractor.setOverwrite(true);
            extractor.extract(tempDir);
            assertThat(tempDir.resolve("adir/link")).isRegularFile().hasContent("new");
        }
    }

    @Test
    void shouldHandleNullAndAlwaysFalseEntryFilter() throws IOException {
        var entry1 = InMemoryArchiveEntry.builder().name("test1").content("c1").build();
        var entry2 = InMemoryArchiveEntry.builder().name("test2").content("c2").build();
        try (var extractor = InMemoryArchiveExtractor.builder(List.of(entry1, entry2))
                .filter(null)
                .build()) {
            extractor.extract(tempDir);
            assertThat(tempDir.resolve("test1")).hasContent("c1");
            assertThat(tempDir.resolve("test2")).hasContent("c2");
        }
        var temp2 = Files.createTempDirectory("temp1");
        try (var extractor = InMemoryArchiveExtractor.builder(List.of(entry1, entry2))
                .filter(e -> false)
                .build()) {
            extractor.extract(temp2);
            assertThat(temp2).isEmptyDirectory();
        }
    }

    @Test
    void shouldSetAttributesWithEdgeCaseModes() throws IOException {
        var mockPath = mock(Path.class);
        @SuppressWarnings("OctalInteger")
        var mode = 07777;
        try (var mockedExtractor = mockStatic(ArchiveExtractor.class, CALLS_REAL_METHODS);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedExtractor.when(ArchiveExtractor::isIsOsWindows).thenReturn(false);
            var mockAttributeView = mock(PosixFileAttributeView.class);
            mockedFiles
                    .when(() -> Files.getFileAttributeView(mockPath, PosixFileAttributeView.class))
                    .thenReturn(mockAttributeView);
            ArchiveExtractor.setAttributes(mode, mockPath);
            verify(mockAttributeView)
                    .setPermissions(io.github.compress4j.utils.PosixFilePermissionsMapper.fromUnixMode(mode));
        }
    }
}
