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
import static io.github.compress4j.utils.FileUtils.write;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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

    // ######################################################
    // # Add Files tests                                    #
    // ######################################################
    @Test
    void shouldAddFileWithPath() throws IOException {
        String fileName = "file_name.txt";
        var path = tempDir.resolve(fileName);
        write(path, "789");

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {
            compressor.addFile(path);

            BasicFileAttributes fileAttrs = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime modTime = fileAttrs.lastModifiedTime();

            InOrder inOrder = inOrder(compressor);
            inOrder.verify(compressor).addFile(path);
            inOrder.verify(compressor)
                    .addFile(eq(fileName), eq(path), any(BasicFileAttributes.class), eq(Optional.empty()));
            mockedStaticCompressor.verify(() -> sanitiseName(fileName));
            inOrder.verify(compressor).accept(fileName, path);
            inOrder.verify(compressor)
                    .writeFileEntry(eq(fileName), any(InputStream.class), eq(3L), eq(modTime), eq(420));
            inOrder.verify(compressor)
                    .writeFileEntry(
                            eq(fileName), any(InputStream.class), eq(3L), eq(modTime), eq(420), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithPathAppliesFilter() throws IOException {
        String fileName = "file_name.txt";
        var path = tempDir.resolve(fileName);
        write(path, "789");

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {
            compressor.withFilter((name, p) -> false);

            compressor.addFile(path);

            InOrder inOrder = inOrder(compressor);
            inOrder.verify(compressor).addFile(path);
            inOrder.verify(compressor)
                    .addFile(eq(fileName), eq(path), any(BasicFileAttributes.class), eq(Optional.empty()));
            mockedStaticCompressor.verify(() -> sanitiseName(fileName));
            inOrder.verify(compressor).accept(fileName, path);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldAddFileWithNameAndPath() throws IOException {
        String fileName = "file_name.txt";
        var path = tempDir.resolve(fileName);
        write(path, "789");
        String entryName = "additional_name.txt";

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {
            compressor.addFile(entryName, path);

            BasicFileAttributes fileAttrs = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime modTime = fileAttrs.lastModifiedTime();

            InOrder inOrder = inOrder(compressor);
            inOrder.verify(compressor).addFile(entryName, path);
            mockedStaticCompressor.verify(() -> sanitiseName(entryName));
            inOrder.verify(compressor)
                    .addFile(eq(entryName), eq(path), any(BasicFileAttributes.class), eq(Optional.empty()));
            inOrder.verify(compressor).accept(entryName, path);
            inOrder.verify(compressor)
                    .writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(420));
            inOrder.verify(compressor)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(420), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNamePathAndModTime() throws IOException {
        String fileName = "file_name.txt";
        var path = tempDir.resolve(fileName);
        write(path, "789");
        String entryName = "additional_name.txt";
        FileTime modTime = FileTime.from(Instant.now());

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {
            compressor.addFile(entryName, path, modTime);

            verify(compressor).accept(entryName, path);
            verify(compressor)
                    .addFile(eq(entryName), eq(path), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            mockedStaticCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(420));
            verify(compressor)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(420), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNameAndBytes() throws IOException {
        String entryName = "additional_name.txt";

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {

            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            byte[] content = "789".getBytes();

            compressor.addFile(entryName, content);

            mockedStaticCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).accept(entryName, null);
            FileTime modTime = FileTime.from(mockedInstant);
            verify(compressor).writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0));
            verify(compressor)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNameBytesAndModTime() throws IOException {
        String entryName = "additional_name.txt";
        FileTime modTime = FileTime.from(Instant.now());

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {

            byte[] content = "789".getBytes();

            compressor.addFile(entryName, content, modTime);

            mockedStaticCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).accept(entryName, null);
            verify(compressor).writeFileEntry(eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0));
            verify(compressor)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(3L), eq(modTime), eq(0), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNameAndInputStream() throws IOException {
        String entryName = "additional_name.txt";

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {

            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            FileTime modTime = FileTime.from(mockedInstant);
            var content = new ByteArrayInputStream("789".getBytes());

            compressor.addFile(entryName, content);

            mockedStaticCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).accept(entryName, null);
            verify(compressor).writeFileEntry(eq(entryName), any(InputStream.class), eq(-1L), eq(modTime), eq(0));
            verify(compressor)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(-1L), eq(modTime), eq(0), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddFileWithNameInputStreamAndModTime() throws IOException {
        String entryName = "additional_name.txt";
        FileTime modTime = FileTime.from(Instant.now());

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {

            var content = new ByteArrayInputStream("789".getBytes());

            compressor.addFile(entryName, content, modTime);

            mockedStaticCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).accept(entryName, null);
            verify(compressor).writeFileEntry(eq(entryName), any(InputStream.class), eq(-1L), eq(modTime), eq(0));
            verify(compressor)
                    .writeFileEntry(
                            eq(entryName), any(InputStream.class), eq(-1L), eq(modTime), eq(0), eq(Optional.empty()));
        }
    }

    // ######################################################
    // # Add Directory tests                                #
    // ######################################################
    @Test
    void shouldAddDirectoryWithName() throws IOException {
        String entryName = "dir_name";

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                MockedStatic<Instant> mockedStaticInstant = mockStatic(Instant.class, CALLS_REAL_METHODS);
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {
            var mockedInstant = Instant.now();
            mockedStaticInstant.when(Instant::now).thenReturn(mockedInstant);
            FileTime modTime = FileTime.from(mockedInstant);

            compressor.addDirectory(entryName);

            verify(compressor).accept(entryName, null);
            mockedStaticCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).writeDirectoryEntry(entryName, modTime);
        }
    }

    @Test
    void shouldAddDirectoryWithNameAndAppliesFilter() throws IOException {
        String entryName = "dir_name";

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {
            compressor.withFilter((name, p) -> false);

            compressor.addDirectory(entryName);

            InOrder inOrder = inOrder(compressor);
            inOrder.verify(compressor).accept(entryName, null);
            mockedStaticCompressor.verify(() -> sanitiseName(entryName));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldAddDirectoryWithNameAndModTime() throws IOException {
        String entryName = "dir_name";
        FileTime modTime = FileTime.from(Instant.now());

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {
            compressor.addDirectory(entryName, modTime);

            verify(compressor).accept(entryName, null);
            mockedStaticCompressor.verify(() -> sanitiseName(entryName));
            verify(compressor).writeDirectoryEntry(entryName, modTime);
        }
    }

    // ######################################################
    // # Add Directory Recursively tests                    #
    // ######################################################
    @Test
    void shouldAddDirectoryRecursivelyWithPath() throws IOException {
        var base = tempDir.resolve("base");
        var file1 = base.resolve("file1");
        var subDir1 = base.resolve("subDir1");
        var file11 = subDir1.resolve("file11");
        write(file1, "1");
        write(file11, "11");

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {

            compressor.addDirectoryRecursively(base);

            verify(compressor).addDirectoryRecursively("", base);
            mockedStaticCompressor.verify(() -> sanitiseName("file1"), times(2));
            mockedStaticCompressor.verify(() -> sanitiseName("subDir1"), times(2));
            mockedStaticCompressor.verify(() -> sanitiseName("subDir1/file11"), times(2));
            verify(compressor).accept("subDir1", subDir1);
            FileTime subDir1ModTime = Files.getLastModifiedTime(subDir1);
            verify(compressor).addDirectory("subDir1", subDir1ModTime);
            verify(compressor).accept("subDir1", null);
            verify(compressor).writeDirectoryEntry("subDir1", subDir1ModTime);
            verify(compressor, times(2)).accept("subDir1/file11", file11);
            verify(compressor)
                    .addFile(eq("subDir1/file11"), eq(file11), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file11ModTime = Files.getLastModifiedTime(file11);
            verify(compressor)
                    .writeFileEntry(eq("subDir1/file11"), any(InputStream.class), eq(2L), eq(file11ModTime), eq(420));
            verify(compressor)
                    .writeFileEntry(
                            eq("subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(file11ModTime),
                            eq(420),
                            eq(Optional.empty()));
            verify(compressor, times(2)).accept("file1", file1);
            verify(compressor).addFile(eq("file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file1ModTime = Files.getLastModifiedTime(file1);
            verify(compressor).writeFileEntry(eq("file1"), any(InputStream.class), eq(1L), eq(file1ModTime), eq(420));
            verify(compressor)
                    .writeFileEntry(
                            eq("file1"),
                            any(InputStream.class),
                            eq(1L),
                            eq(file1ModTime),
                            eq(420),
                            eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddDirectoryRecursivelyWithPathAppliesFilter() throws IOException {
        var base = tempDir.resolve("base");
        var file1 = base.resolve("file1");
        var subDir1 = base.resolve("subDir1");
        var file11 = subDir1.resolve("file11");
        write(file1, "1");
        write(file11, "11");

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {
            compressor.withFilter((name, p) -> false);

            compressor.addDirectoryRecursively(base);

            InOrder inOrder = inOrder(compressor);
            inOrder.verify(compressor).addDirectoryRecursively("", base);
            mockedStaticCompressor.verify(() -> sanitiseName("file1"));
            mockedStaticCompressor.verify(() -> sanitiseName("subDir1"));
            inOrder.verify(compressor).accept("subDir1", subDir1);
            inOrder.verify(compressor).accept("file1", file1);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldAddDirectoryRecursivelyWithPathAndTopLevelDir() throws IOException {
        String top = "top";

        var base = tempDir.resolve("base");
        var file1 = base.resolve("file1");
        var subDir1 = base.resolve("subDir1");
        var file11 = subDir1.resolve("file11");
        write(file1, "1");
        write(file11, "11");

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {

            compressor.addDirectoryRecursively(top, base);

            mockedStaticCompressor.verify(() -> sanitiseName("file1"));
            mockedStaticCompressor.verify(() -> sanitiseName("subDir1"));
            mockedStaticCompressor.verify(() -> sanitiseName("subDir1/file11"));
            verify(compressor).accept(top, null);
            FileTime baseModTime = Files.getLastModifiedTime(base);
            verify(compressor).writeDirectoryEntry("top", baseModTime);
            verify(compressor).accept(top, base);
            verify(compressor).accept("top/subDir1", subDir1);
            FileTime subDir1ModTime = Files.getLastModifiedTime(subDir1);
            verify(compressor).addDirectory("top/subDir1", subDir1ModTime);
            verify(compressor).accept("top/subDir1", null);
            verify(compressor).writeDirectoryEntry("top/subDir1", subDir1ModTime);
            verify(compressor, times(2)).accept("top/subDir1/file11", file11);
            verify(compressor)
                    .addFile(
                            eq("top/subDir1/file11"), eq(file11), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file11ModTime = Files.getLastModifiedTime(file11);
            verify(compressor)
                    .writeFileEntry(
                            eq("top/subDir1/file11"), any(InputStream.class), eq(2L), eq(file11ModTime), eq(420));
            verify(compressor)
                    .writeFileEntry(
                            eq("top/subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(file11ModTime),
                            eq(420),
                            eq(Optional.empty()));
            verify(compressor, times(2)).accept("top/file1", file1);
            verify(compressor)
                    .addFile(eq("top/file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.empty()));
            FileTime file1ModTime = Files.getLastModifiedTime(file1);
            verify(compressor)
                    .writeFileEntry(eq("top/file1"), any(InputStream.class), eq(1L), eq(file1ModTime), eq(420));
            verify(compressor)
                    .writeFileEntry(
                            eq("top/file1"),
                            any(InputStream.class),
                            eq(1L),
                            eq(file1ModTime),
                            eq(420),
                            eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddDirectoryRecursivelyWithPathAndModTime() throws IOException {
        FileTime modTime = FileTime.from(Instant.now());
        var base = tempDir.resolve("base");
        var file1 = base.resolve("file1");
        var subDir1 = base.resolve("subDir1");
        var file11 = subDir1.resolve("file11");
        write(file1, "1");
        write(file11, "11");

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {

            compressor.addDirectoryRecursively(base, modTime);

            verify(compressor).addDirectoryRecursively("", base, modTime);
            mockedStaticCompressor.verify(() -> sanitiseName("file1"), times(2));
            mockedStaticCompressor.verify(() -> sanitiseName("subDir1"), times(2));
            mockedStaticCompressor.verify(() -> sanitiseName("subDir1/file11"), times(2));
            verify(compressor).accept("subDir1", subDir1);
            verify(compressor).addDirectory("subDir1", modTime);
            verify(compressor).accept("subDir1", null);
            verify(compressor).writeDirectoryEntry("subDir1", modTime);
            verify(compressor, times(2)).accept("subDir1/file11", file11);
            verify(compressor)
                    .addFile(
                            eq("subDir1/file11"), eq(file11), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            verify(compressor)
                    .writeFileEntry(eq("subDir1/file11"), any(InputStream.class), eq(2L), eq(modTime), eq(420));
            verify(compressor)
                    .writeFileEntry(
                            eq("subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(modTime),
                            eq(420),
                            eq(Optional.empty()));
            verify(compressor, times(2)).accept("file1", file1);
            verify(compressor)
                    .addFile(eq("file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            verify(compressor).writeFileEntry(eq("file1"), any(InputStream.class), eq(1L), eq(modTime), eq(420));
            verify(compressor)
                    .writeFileEntry(
                            eq("file1"), any(InputStream.class), eq(1L), eq(modTime), eq(420), eq(Optional.empty()));
        }
    }

    @Test
    void shouldAddDirectoryRecursivelyWithPathTopLevelDirAndModTime() throws IOException {
        FileTime modTime = FileTime.from(Instant.now());
        String top = "top";
        var base = tempDir.resolve("base");
        var file1 = base.resolve("file1");
        var subDir1 = base.resolve("subDir1");
        var file11 = subDir1.resolve("file11");
        write(file1, "1");
        write(file11, "11");

        //noinspection rawtypes
        try (MockedStatic<Compressor> mockedStaticCompressor =
                        mockStatic(Compressor.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                ArchiveOutputStreamUnderTest outputStream = new ArchiveOutputStreamUnderTest(out);
                CompressorUnderTest compressor = spy(new CompressorUnderTest(outputStream))) {

            compressor.addDirectoryRecursively(top, base, modTime);

            mockedStaticCompressor.verify(() -> sanitiseName("file1"));
            mockedStaticCompressor.verify(() -> sanitiseName("subDir1"));
            mockedStaticCompressor.verify(() -> sanitiseName("subDir1/file11"));
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
                    .writeFileEntry(eq("top/subDir1/file11"), any(InputStream.class), eq(2L), eq(modTime), eq(420));
            verify(compressor)
                    .writeFileEntry(
                            eq("top/subDir1/file11"),
                            any(InputStream.class),
                            eq(2L),
                            eq(modTime),
                            eq(420),
                            eq(Optional.empty()));
            verify(compressor, times(2)).accept("top/file1", file1);
            verify(compressor)
                    .addFile(eq("top/file1"), eq(file1), any(BasicFileAttributes.class), eq(Optional.of(modTime)));
            verify(compressor).writeFileEntry(eq("top/file1"), any(InputStream.class), eq(1L), eq(modTime), eq(420));
            verify(compressor)
                    .writeFileEntry(
                            eq("top/file1"),
                            any(InputStream.class),
                            eq(1L),
                            eq(modTime),
                            eq(420),
                            eq(Optional.empty()));
        }
    }

    private static class CompressorUnderTest extends Compressor<ArchiveOutputStreamUnderTest> {

        private final ArchiveOutputStreamUnderTest outputStream;

        public CompressorUnderTest(ArchiveOutputStreamUnderTest outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void close() throws IOException {
            outputStream.close();
        }

        @Override
        protected ArchiveOutputStreamUnderTest createArchiveOutputStream(OutputStream s, Map<String, Object> o) {
            return outputStream;
        }

        @Override
        protected void writeDirectoryEntry(String name, FileTime modTime) {
            DumpArchiveEntry entry = new DumpArchiveEntry(name, name);
            entry.setLastModifiedDate(Date.from(modTime.toInstant()));
            outputStream.putArchiveEntry(entry);
            outputStream.closeArchiveEntry();
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
            outputStream.putArchiveEntry(entry);
            outputStream.closeArchiveEntry();
        }
    }

    private static class ArchiveOutputStreamUnderTest extends ArchiveOutputStream<DumpArchiveEntry> {
        @SuppressWarnings("unused")
        private boolean entryClosed = true;

        public ArchiveOutputStreamUnderTest(OutputStream out) {
            super(out);
        }

        @Override
        public void closeArchiveEntry() {
            entryClosed = true;
        }

        @Override
        public DumpArchiveEntry createArchiveEntry(File inputFile, String entryName) {
            return new DumpArchiveEntry(entryName, entryName);
        }

        @Override
        public void putArchiveEntry(DumpArchiveEntry entry) {
            entryClosed = false;
        }
    }
}
