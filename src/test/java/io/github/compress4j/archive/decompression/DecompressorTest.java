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
import static io.github.compress4j.archive.decompression.Decompressor.ErrorHandlerChoice.*;
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
        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(new String[][] {{"test1", "content1"}, {"test2", "content2"}})) {
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
        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(new String[][] {{"test1", "content1"}, {"subdir/test2", "content2"}})) {

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
        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(new String[][] {{"test1", "content1"}, {"subdir/test2", "content2"}})) {
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
        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(new String[][] {{"test1", "content1"}, {"subdir/test2", "content2"}})) {
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
        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(new String[][] {{"../test1", "content1"}, {"subdir/test2", "content2"}})) {

            // when && then
            assertThatThrownBy(() -> decompressorUnderTest.extract(tempDir))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Invalid entry name: ../test1");
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsRetries() throws IOException {
        // given
        AtomicInteger retries = new AtomicInteger(3);
        BiFunction<Decompressor.Entry, IOException, Decompressor.ErrorHandlerChoice> errorHandler =
                (entry, exception) -> {
                    if (retries.get() == 0) {
                        return ABORT;
                    }
                    retries.getAndDecrement();
                    return RETRY;
                };

        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(new String[][] {{"../test1", "content1"}, {"subdir/test2", "content2"}})) {
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
        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(new String[][] {{"../test1", "content1"}, {"subdir/test2", "content2"}})) {
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
        try (DecompressorUnderTest decompressorUnderTest =
                new DecompressorUnderTest(new String[][] {{"subdir/test1", "content1"}, {"../test2", "content2"}})) {
            decompressorUnderTest.setErrorHandler((entry, exception) -> ABORT);

            // when
            decompressorUnderTest.extract(tempDir);

            // then
            assertThat(tempDir).isDirectory();
            assertThat(tempDir.resolve("subdir/test1")).hasContent("content1");
        }
    }

    @Test
    void shouldFailExtractFilesWithInvalidPathsSkipsEntry() throws IOException {
        // given
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(new String[][] {
            {"subdir/../test1", "content1"}, {"subdir/some/../test1a", "content1a"}, {"subdir/test2", "content2"}
        })) {
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
        try (DecompressorUnderTest decompressorUnderTest = new DecompressorUnderTest(new String[][] {
            {"subdir/../test1", "content1"}, {"subdir/some/../test1a", "content1a"}, {"subdir/test2", "content2"}
        })) {
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
        public DecompressorUnderTest(final String[][] pFiles) throws IOException {
            super(MemoryArchiveInputStream.toInputStream(pFiles));
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
            return new Entry(nextEntry.getName(), nextEntry.isDirectory(), nextEntry.getSize());
        }

        @Override
        protected InputStream openEntryStream(Entry entry) {
            return new ByteArrayInputStream(archiveInputStream.readString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
