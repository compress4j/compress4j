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
package io.github.compress4j.archive.decompression;

import static ch.qos.logback.classic.Level.DEBUG;
import static io.github.compress4j.archive.decompression.Decompressor.Entry.Type.SYMLINK;
import static io.github.compress4j.archive.decompression.Decompressor.ErrorHandlerChoice.*;
import static io.github.compress4j.archive.decompression.Decompressor.EscapingSymlinkPolicy.DISALLOW;
import static io.github.compress4j.archive.decompression.Decompressor.EscapingSymlinkPolicy.RELATIVIZE_ABSOLUTE;
import static io.github.compress4j.utils.FileUtils.NO_MODE;
import static io.github.compress4j.utils.TestFileUtils.createFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.github.compress4j.assertion.Compress4JAssertions;
import io.github.compress4j.memory.MemoryArchiveEntry;
import io.github.compress4j.memory.MemoryArchiveInputStream;
import io.github.compress4j.test.util.InMemoryLogAppender;
import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class DecompressorTest {

    private static final String LOGGER_NAME = Decompressor.class.getPackageName();
    private InMemoryLogAppender inMemoryLogAppender;

    @TempDir
    private Path tempDir;

    @BeforeEach
    public void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
        inMemoryLogAppender = new InMemoryLogAppender();
        inMemoryLogAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(DEBUG);
        logger.addAppender(inMemoryLogAppender);
        inMemoryLogAppender.start();
    }

    @AfterEach
    public void cleanUp() {
        inMemoryLogAppender.reset();
        inMemoryLogAppender.stop();
    }

    @Test
    void shouldExtractFiles() throws IOException {
        // given
        var entry1 = new MemoryArchiveEntry("test1", "content1");
        var entry2 = new MemoryArchiveEntry("test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {
            // when
            decompressorUnderTest.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test1")).hasContent("content1");
            assertThat(tempDir.resolve("test2")).hasContent("content2");
        }
    }

    @Test
    void shouldExtractFilesWithSubdirectories() throws IOException {
        // given
        var entry1 = new MemoryArchiveEntry("test1", "content1");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {

            // when
            decompressorUnderTest.extract(tempDir);

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

        var entry1 = new MemoryArchiveEntry("test1", "content1");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {

            // when
            decompressorUnderTest.extract(tempDir);

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

        var entry1 = new MemoryArchiveEntry("test1", "content1");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {
            decompressorUnderTest.setOverwrite(true);

            // when
            decompressorUnderTest.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test1")).hasContent("content1");
            assertThat(tempDir.resolve("subdir/test2")).hasContent("content2");
        }
    }

    @Test
    void shouldExtractFilesWithSubdirectoriesAndStripZeroComponents() throws IOException {
        // given
        var entry1 = new MemoryArchiveEntry("test1", "content1");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {
            decompressorUnderTest.setStripComponents(0);

            // when
            decompressorUnderTest.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test1")).hasContent("content1");
            assertThat(tempDir.resolve("subdir/test2")).hasContent("content2");
        }
    }

    @Test
    void shouldExtractFilesWithSubdirectoriesAndStripComponents() throws IOException {
        // given
        var entry1 = new MemoryArchiveEntry("test1", "content1");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {
            decompressorUnderTest.setStripComponents(1);

            // when
            decompressorUnderTest.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("test2")).hasContent("content2");
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPaths() throws IOException {
        // given
        var entry1 = new MemoryArchiveEntry("../test1", "content1");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {

            // when && then
            assertThatThrownBy(() -> decompressorUnderTest.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Invalid entry name: ../test1");
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsRetries() throws IOException {
        // given
        var entry1 = new MemoryArchiveEntry("../test1", "content1");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");
        AtomicInteger retries = new AtomicInteger(3);
        BiFunction<Decompressor.Entry, IOException, Decompressor.ErrorHandlerChoice> errorHandler =
                (entry, exception) -> {
                    if (retries.get() == 0) {
                        return ABORT;
                    }
                    retries.getAndDecrement();
                    return RETRY;
                };

        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {
            decompressorUnderTest.setErrorHandler(errorHandler);

            // when
            decompressorUnderTest.extract(tempDir);

            // then
            Compress4JAssertions.assertThat(inMemoryLogAppender).contains("Retying because of exception", DEBUG);
            assertThat(tempDir).isEmptyDirectory();
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsAborts() throws IOException {
        // given
        var entry1 = new MemoryArchiveEntry("../test1", "content1");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {
            decompressorUnderTest.setErrorHandler((entry, exception) -> ABORT);

            // when
            decompressorUnderTest.extract(tempDir);

            // then
            assertThat(tempDir).isEmptyDirectory();
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsAbortsAfterSomeFilesExtractedAlready() throws IOException {
        // given
        var entry1 = new MemoryArchiveEntry("subdir/test1", "content1");
        var entry2 = new MemoryArchiveEntry("../test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {
            decompressorUnderTest.setErrorHandler((entry, exception) -> ABORT);

            // when
            decompressorUnderTest.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsBails() throws IOException {
        // given
        var entry1 = new MemoryArchiveEntry("../test1", "content1");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {
            decompressorUnderTest.setErrorHandler((entry, exception) -> BAIL_OUT);

            // when
            assertThatThrownBy(() -> decompressorUnderTest.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Invalid entry name: ../test1");

            // then
            assertThat(tempDir).isEmptyDirectory();
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsBailsAfterSomeFilesExtractedAlready() throws IOException {
        // given
        var entry1 = new MemoryArchiveEntry("subdir/test1", "content1");
        var entry2 = new MemoryArchiveEntry("../test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(List.of(entry1, entry2))) {
            decompressorUnderTest.setErrorHandler((entry, exception) -> BAIL_OUT);

            // when
            assertThatThrownBy(() -> decompressorUnderTest.extract(tempDir))
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
        var entry1 = new MemoryArchiveEntry("subdir/../test1", "content1");
        var entry1a = new MemoryArchiveEntry("subdir/some/../test1a", "content1a");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(List.of(entry1, entry1a, entry2))) {
            decompressorUnderTest.setErrorHandler((entry, exception) -> SKIP);

            // when
            decompressorUnderTest.extract(tempDir);

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
        var entry1 = new MemoryArchiveEntry("subdir/../test1", "content1");
        var entry1a = new MemoryArchiveEntry("subdir/some/../test1a", "content1a");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");
        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(List.of(entry1, entry1a, entry2))) {
            decompressorUnderTest.setErrorHandler((entry, exception) -> SKIP_ALL);

            // when
            decompressorUnderTest.extract(tempDir);

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
        var subdir = new MemoryArchiveEntry("subdir", null, true, 0);
        var entry1 = new MemoryArchiveEntry("subdir/test1", "content1");
        var entry1a = new MemoryArchiveEntry("subdir/some/test1a", "content1a");
        var entry2 = new MemoryArchiveEntry("subdir/test2", "content2");

        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(List.of(subdir, entry1, entry1a, entry2))) {
            decompressorUnderTest.setEntryFilter(entry -> !entry.name.contains("some"));

            // when
            decompressorUnderTest.extract(tempDir);

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
        var subdir = new MemoryArchiveEntry("subdir", null, true, 0);
        var entry1 = new MemoryArchiveEntry("subdir/test1", "content1");
        var entry1a = new MemoryArchiveEntry("test1a", null, SYMLINK, NO_MODE, "subdir/test1", 0);

        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(List.of(subdir, entry1, entry1a))) {

            // when
            decompressorUnderTest.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).isSymbolicLink().hasContent("content1");
        }
    }

    @Test
    void shouldExtractSymlinksInRelativizeAbsoluteMode() throws IOException {
        // given
        var subdir = new MemoryArchiveEntry("subdir", null, true, 0);
        var entry1 = new MemoryArchiveEntry("/subdir/test1", "content1");
        var entry1a = new MemoryArchiveEntry("test1a", null, SYMLINK, NO_MODE, "/subdir/test1", 0);

        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(List.of(subdir, entry1, entry1a))) {
            decompressorUnderTest.setEscapingSymlinkPolicy(RELATIVIZE_ABSOLUTE);

            // when
            decompressorUnderTest.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/some")).doesNotExist();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
            assertThat(tempDir.resolve("test1a")).isSymbolicLink().hasContent("content1");
        }
    }

    @Test
    void shouldNotExtractSymlinksInDisallowMode() throws IOException {
        // given
        var subdir = new MemoryArchiveEntry("subdir", null, true, 0);
        var entry1 = new MemoryArchiveEntry("/subdir/test1", "content1");
        var entry1a = new MemoryArchiveEntry("test1a", null, SYMLINK, NO_MODE, "/subdir/test1", 0);

        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(List.of(subdir, entry1, entry1a))) {
            decompressorUnderTest.setEscapingSymlinkPolicy(DISALLOW);

            // when
            assertThatThrownBy(() -> decompressorUnderTest.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Invalid symlink (absolute path): test1a -> /subdir/test1");

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
        var subdir = new MemoryArchiveEntry("subdir", null, true, 0);
        var entry1 = new MemoryArchiveEntry("/subdir/test1", "content1");
        var entry1a = new MemoryArchiveEntry("test1a", null, SYMLINK, NO_MODE, null, 0);

        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(List.of(subdir, entry1, entry1a))) {

            // when
            assertThatThrownBy(() -> decompressorUnderTest.extract(tempDir))
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
        var subdir = new MemoryArchiveEntry("subdir", null, true, 0);
        var entry1 = new MemoryArchiveEntry("/subdir/test1", "content1");
        var entry1a = new MemoryArchiveEntry("test1a", null, SYMLINK, NO_MODE, "", 0);

        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(List.of(subdir, entry1, entry1a))) {

            // when
            assertThatThrownBy(() -> decompressorUnderTest.extract(tempDir))
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

        var subdir = new MemoryArchiveEntry("subdir", null, true, 0);
        var entry1 = new MemoryArchiveEntry("/subdir/test1", "content1");
        var entry1a = new MemoryArchiveEntry("test1a", null, SYMLINK, NO_MODE, "some", 0);

        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(List.of(subdir, entry1, entry1a))) {

            // when
            decompressorUnderTest.extract(tempDir);

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

        var subdir = new MemoryArchiveEntry("subdir", null, true, 0);
        var entry1 = new MemoryArchiveEntry("subdir/test1", "content1");
        var entry1a = new MemoryArchiveEntry("test1a", null, SYMLINK, NO_MODE, "subdir/test1", 0);

        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(List.of(subdir, entry1, entry1a))) {
            decompressorUnderTest.setOverwrite(true);

            // when
            decompressorUnderTest.extract(tempDir);

            // then
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
        var result = Decompressor.normalizePathAndSplit(path);

        // then
        assertThat(result).isNotEmpty();
    }

    // ######################################################
    // #  Utility classes                                   #
    // ######################################################
    public static class DecompressorUnderTest extends Decompressor<MemoryArchiveInputStream> {
        public DecompressorUnderTest(final List<MemoryArchiveEntry> entries) throws IOException {
            super(MemoryArchiveInputStream.toInputStream(entries));
        }

        @Override
        protected MemoryArchiveInputStream buildArchiveInputStream(InputStream inputStream) throws IOException {
            return new MemoryArchiveInputStream(inputStream);
        }

        @Override
        protected void closeEntryStream(InputStream stream) {
            // do nothing
        }

        @Nullable
        @Override
        protected Entry nextEntry() {
            MemoryArchiveEntry nextEntry = archiveInputStream.getNextEntry();
            if (nextEntry == null) {
                return null;
            }
            return new Entry(
                    nextEntry.getName(),
                    nextEntry.getType(),
                    nextEntry.getMode(),
                    nextEntry.getLinkName(),
                    nextEntry.getSize());
        }

        @Override
        protected InputStream openEntryStream(Entry entry) {
            return new ByteArrayInputStream(archiveInputStream.readString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
