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

import static io.github.compress4j.utils.Assumptions.assumeSymLinkCreationIsSupported;
import static io.github.compress4j.utils.FileUtils.deleteRecursively;
import static io.github.compress4j.utils.FileUtils.write;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.getPosixFilePermissions;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.compress4j.archive.decompression.TarDecompressor;
import io.github.compress4j.assertion.CompressorAssertion;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class TarCompressorTest {
    @TempDir
    Path tempDir;

    protected Path compressFile;
    protected TarCompressor compressor;

    @BeforeEach
    void setup() throws IOException {
        compressFile = tempDir.resolve("test.tar");
        compressor = new TarCompressor(compressFile);
    }

    @AfterEach
    void tearDown() {
        compressFile = null;
        compressor = null;
    }

    @Test
    void shouldAddFile() throws IOException {
        String fileName = "file_name.txt";
        var path = tempDir.resolve(fileName);
        write(path, "789");
        compressor.addFile(path);
        compressor.close();

        CompressorAssertion.assertThat(compressFile).containsAllEntriesOf(Map.of(fileName, "789"));

        var out = tempDir.resolve("out");
        extract(compressFile, out);
        Assertions.assertThat(out.toFile()).isDirectory().isDirectoryContaining(f -> f.getName()
                .equals(fileName));
    }

    @Test
    void shouldAddFileWithLongName() throws IOException {
        String fileName =
                "looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong_filename.txt";
        var path = tempDir.resolve(fileName);
        write(path, "789");
        compressor.addFile(path);
        compressor.close();

        CompressorAssertion.assertThat(compressFile).containsAllEntriesOf(Map.of(fileName, "789"));

        var out = tempDir.resolve("out");
        extract(compressFile, out);
        Assertions.assertThat(out.toFile()).isDirectory().isDirectoryContaining(f -> f.getName()
                .equals(fileName));
    }

    @Test
    void shouldNotAddNonExistingFile() throws IOException {
        var path = Path.of("non-existing-file");
        assertThrows(NoSuchFileException.class, () -> compressor.addFile(path));
        compressor.close();
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void shouldNotAddNonReadableFile() throws IOException {
        var path = tempDir.resolve("non-readable-file.txt");
        write(path, "789");

        // Set file permissions to make it non-readable
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("---------");
        Files.setPosixFilePermissions(path, permissions);

        assertThrows(AccessDeniedException.class, () -> compressor.addFile(path));
        compressor.close();
    }

    @Test
    void shouldAddFiles() throws IOException {
        var path = tempDir.resolve("file.txt");
        write(path, "789");
        compressor.addFile("empty.txt", new byte[0]);
        compressor.addFile("file1.txt", "123".getBytes());
        compressor.addFile("file2.txt", "456".getBytes());
        compressor.addFile("file3.txt", path);
        compressor.close();

        CompressorAssertion.assertThat(compressFile)
                .containsAllEntriesOf(
                        Map.of("empty.txt", "", "file1.txt", "123", "file2.txt", "456", "file3.txt", "789"));

        var out = tempDir.resolve("out");
        extract(compressFile, out);
        Assertions.assertThat(out.toFile()).isDirectory().isDirectoryContaining(f -> f.getName()
                .equals("empty.txt"));
    }

    @Test
    void shouldAddDirectoriesRecursively() throws IOException {
        var base = tempDir.resolve("base");
        write(tempDir.resolve("base/file1"), "1");
        write(tempDir.resolve("base/file2"), "2");
        write(tempDir.resolve("base/subDir1/file11"), "11");
        write(tempDir.resolve("base/subDir1/file12"), "12");
        write(tempDir.resolve("base/subDir1/d11/file111"), "111");
        write(tempDir.resolve("base/subDir1/d11/file112"), "112");
        write(tempDir.resolve("base/subDir2/file21"), "21");
        write(tempDir.resolve("base/subDir2/file22"), "22");

        compressor.addDirectoryRecursively("tar/", base);
        CompressorAssertion.assertThat(compressFile)
                .containsAllEntriesOf(Map.ofEntries(
                        entry("tar/", ""),
                        entry("tar/subDir1/", ""),
                        entry("tar/subDir1/d11/", ""),
                        entry("tar/subDir2/", ""),
                        entry("tar/file1", "1"),
                        entry("tar/file2", "2"),
                        entry("tar/subDir1/file11", "11"),
                        entry("tar/subDir1/file12", "12"),
                        entry("tar/subDir1/d11/file111", "111"),
                        entry("tar/subDir1/d11/file112", "112"),
                        entry("tar/subDir2/file21", "21"),
                        entry("tar/subDir2/file22", "22")));
    }

    @Test
    void shouldAddDirectoriesRecursivelyWithEmptyPrefix() throws IOException {
        var base = tempDir.resolve("base");
        write(tempDir.resolve("base/file1"), "1");
        write(tempDir.resolve("base/file2"), "2");
        write(tempDir.resolve("base/subDir1/file11"), "11");
        write(tempDir.resolve("base/subDir1/file12"), "12");
        write(tempDir.resolve("base/subDir1/d11/file111"), "111");
        write(tempDir.resolve("base/subDir1/d11/file112"), "112");
        write(tempDir.resolve("base/subDir2/file21"), "21");
        write(tempDir.resolve("base/subDir2/file22"), "22");

        compressor.addDirectoryRecursively("", base);
        compressor.close();

        CompressorAssertion.assertThat(compressFile)
                .containsAllEntriesOf(Map.ofEntries(
                        entry("subDir1/", ""),
                        entry("subDir1/d11/", ""),
                        entry("subDir2/", ""),
                        entry("file1", "1"),
                        entry("file2", "2"),
                        entry("subDir1/file11", "11"),
                        entry("subDir1/file12", "12"),
                        entry("subDir1/d11/file111", "111"),
                        entry("subDir1/d11/file112", "112"),
                        entry("subDir2/file21", "21"),
                        entry("subDir2/file22", "22")));
    }

    @Test
    void shouldAddDirectoryRecursivelyWithEmptyPrefix() throws IOException {
        Path file = tempDir.resolve("base/file");
        createDirectories(file.getParent());
        createFile(file);
        compressor.addDirectoryRecursively("", file.getParent());
        CompressorAssertion.assertThat(compressFile)
                .containsAllEntriesOf(Map.of(file.getFileName().toString(), ""));
    }

    @Test
    void shouldAddRetainExecutableFilePermission() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        var base = tempDir.resolve("base");
        createDirectories(base);
        var regular = base.resolve("regular");
        createFile(regular);
        var executable = base.resolve("executable");
        createFile(executable, PosixFilePermissions.asFileAttribute(Set.of(PosixFilePermission.values())));
        compressor.addDirectoryRecursively(base);
        compressor.close();

        var out = tempDir.resolve("out");
        extract(compressFile, out);
        assertThat(getPosixFilePermissions(out.resolve(regular.getFileName())))
                .doesNotContain(PosixFilePermission.OWNER_EXECUTE);
        assertThat(getPosixFilePermissions(out.resolve(executable.getFileName())))
                .contains(PosixFilePermission.OWNER_EXECUTE);
    }

    @Test
    void shouldAddSymbolicLinks() throws IOException {
        assumeSymLinkCreationIsSupported();

        var baseDir = tempDir.resolve("base");
        createDirectories(baseDir);
        var origin = baseDir.resolve("origin");
        createFile(origin);
        var link = baseDir.resolve("link");
        Files.createSymbolicLink(link, origin.getFileName());
        compressor.addDirectoryRecursively(baseDir);
        compressor.close();

        deleteRecursively(baseDir);

        var out = tempDir.resolve("out");
        extract(compressFile, out);
        assertThat(out.resolve(link.getFileName()))
                .isSymbolicLink()
                .hasSameBinaryContentAs(out.resolve(origin.getFileName()));
    }

    protected void extract(Path in, Path out) throws IOException {
        TarDecompressor.builder(in).build().extract(out);
    }
}
