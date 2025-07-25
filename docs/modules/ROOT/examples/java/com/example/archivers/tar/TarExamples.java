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
package com.example.archivers.tar;

import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.RETRY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.zip.Deflater.BEST_COMPRESSION;
import static java.util.zip.Deflater.HUFFMAN_ONLY;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_ERROR;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_POSIX;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX;

import io.github.compress4j.archivers.ArchiveExtractor;
import io.github.compress4j.archivers.tar.TarArchiveCreator;
import io.github.compress4j.archivers.tar.TarArchiveExtractor;
import io.github.compress4j.archivers.tar.TarBZip2ArchiveCreator;
import io.github.compress4j.archivers.tar.TarBZip2ArchiveExtractor;
import io.github.compress4j.archivers.tar.TarGzArchiveCreator;
import io.github.compress4j.archivers.tar.TarGzArchiveExtractor;
import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings({"java:S1192", "unused"})
public class TarExamples {

    private TarExamples() {
        // Usage example
    }

    public static void tarCreator() throws IOException {
        // tag::tar-creator[]
        try (TarArchiveCreator tarCreator = TarArchiveCreator.builder(Path.of("example.tar"))
                .blockSize(1024)
                .encoding(UTF_8.name())
                .addPaxHeadersForNonAsciiNames(true)
                .bigNumberMode(BIGNUMBER_ERROR)
                .longFileMode(LONGFILE_GNU)
                .filter((name, p) -> !name.endsWith("some_file.txt"))
                .build()) {
            tarCreator.addDirectoryRecursively(Path.of("exampleDir"));
            tarCreator.addFile(Path.of("path/to/file.txt"));
        }
        // end::tar-creator[]
    }

    public static void tarExtractor() throws IOException {
        // tag::tar-extractor[]
        try (TarArchiveExtractor tarExtractor = TarArchiveExtractor.builder(Path.of("example.tar"))
                .filter(entry -> !entry.name.startsWith("bad"))
                .errorHandler((entry, exception) -> RETRY)
                .escapingSymlinkPolicy(ArchiveExtractor.EscapingSymlinkPolicy.DISALLOW)
                .postProcessor((entry, exception) -> {})
                .stripComponents(1)
                .overwrite(true)
                .build()) {
            tarExtractor.extract(Path.of("outputDir"));
        }
        // end::tar-extractor[]
    }

    public static void tarGzCreator() throws IOException {
        // tag::tar-gz-creator[]
        try (TarGzArchiveCreator tarGzCreator = TarGzArchiveCreator.builder(Path.of("example.tar.gz"))
                .compressorOutputStreamBuilder()
                .bufferSize(1024)
                .compressionLevel(BEST_COMPRESSION)
                .comment("comment")
                .deflateStrategy(HUFFMAN_ONLY)
                .operatingSystem(0)
                .parentBuilder()
                .longFileMode(LONGFILE_POSIX)
                .bigNumberMode(BIGNUMBER_POSIX)
                .blockSize(1024)
                .encoding(UTF_8.name())
                .addPaxHeadersForNonAsciiNames(true)
                .filter((name, p) -> !name.endsWith("some_file.txt"))
                .build()) {
            tarGzCreator.addDirectoryRecursively(Path.of("exampleDir"));
            tarGzCreator.addFile(Path.of("path/to/file.txt"));
        }
        // end::tar-gz-creator[]
    }

    public static void tarGzExtractor() throws IOException {
        // tag::tar-gz-extractor[]
        try (TarGzArchiveExtractor tarGzExtractor = TarGzArchiveExtractor.builder(Path.of("example.tar.gz"))
                .filter(entry -> !entry.name.startsWith("bad"))
                .errorHandler((entry, exception) -> RETRY)
                .escapingSymlinkPolicy(ArchiveExtractor.EscapingSymlinkPolicy.DISALLOW)
                .postProcessor((entry, exception) -> {})
                .stripComponents(1)
                .overwrite(true)
                .build()) {
            tarGzExtractor.extract(Path.of("outputDir"));
        }
        // end::tar-gz-extractor[]
    }

    public static void tarBzip2Creator() throws IOException {
        // tag::tar-bzip2-creator[]
        try (TarBZip2ArchiveCreator tarBzip2Creator = TarBZip2ArchiveCreator.builder(Path.of("example.tar.bz2"))
                .compressorOutputStreamBuilder()
                .blockSize(1024)
                .parentBuilder()
                .blockSize(1024)
                .encoding(UTF_8.name())
                .addPaxHeadersForNonAsciiNames(true)
                .bigNumberMode(BIGNUMBER_ERROR)
                .longFileMode(LONGFILE_GNU)
                .filter((name, p) -> !name.endsWith("some_file.txt"))
                .build()) {
            tarBzip2Creator.addDirectoryRecursively(Path.of("exampleDir"));
            tarBzip2Creator.addFile(Path.of("path/to/file.txt"));
        }
        // end::tar-bzip2-creator[]
    }

    public static void tarBzip2Extractor() throws IOException {
        // tag::tar-bzip2-extractor[]
        try (TarBZip2ArchiveExtractor tarBzip2Extractor = TarBZip2ArchiveExtractor.builder(Path.of("example.tar.bz2"))
                .filter(entry -> !entry.name.startsWith("bad"))
                .errorHandler((entry, exception) -> RETRY)
                .escapingSymlinkPolicy(ArchiveExtractor.EscapingSymlinkPolicy.DISALLOW)
                .postProcessor((entry, exception) -> {})
                .stripComponents(1)
                .overwrite(true)
                .build()) {
            tarBzip2Extractor.extract(Path.of("outputDir"));
        }
        // end::tar-bzip2-extractor[]
    }
}
