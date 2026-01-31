/*
 * Copyright 2024-2026 The Compress4J Project
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
package io.github.compress4j;

import io.github.compress4j.archivers.ArchiveExtractor;
import io.github.compress4j.archivers.ArchiveFormat;
import io.github.compress4j.archivers.CompressionType;
import io.github.compress4j.archivers.FileType;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main entry point for Compress4J library providing simple static methods for
 * common compression and archiving
 * operations.
 *
 * <p>
 * This class serves as a facade that simplifies access to the underlying
 * archive and compression functionality. It
 * provides both simple one-liner operations and access to more advanced
 * builder-based APIs.
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * // Extract an archive (auto-detects format)
 * Compress4J.extractAll(Paths.get("archive.tar.gz"), Paths.get("/output"));
 *
 * // List archive contents
 * List<String> entries = Compress4J.list(Paths.get("archive.zip"));
 *
 * // Open for streaming access
 * try (var handle = Compress4J.open(Paths.get("archive.tar"))) {
 *     handle.stream()
 *             .filter(e -> e.name().endsWith(".txt"))
 *             .forEach(e -> System.out.println(e.name()));
 * }
 * }</pre>
 *
 * @since 3.0
 */
public final class Compress4J {

    private Compress4J() {
        // Prevent instantiation
    }

    /**
     * Opens an archive file for reading with automatic format detection based on
     * file extension.
     *
     * <p>
     * This method auto-detects the archive format and compression type from the
     * file extension (e.g., .tar.gz,
     * .zip). The returned handle can be used for streaming, iteration, or selective
     * extraction.
     *
     * <p>
     * Example:
     *
     * <pre>{@code
     * try (var handle = Compress4J.open(Paths.get("data.tar.gz"))) {
     *     for (var entry : handle) {
     *         System.out.println(entry.name());
     *     }
     * }
     * }</pre>
     *
     * @param archivePath path to the archive file
     * @return an ArchiveHandle for reading the archive
     * @throws IOException if an I/O error occurs or format cannot be detected
     * @since 3.0
     */
    public static ArchiveHandle open(@Nonnull Path archivePath) throws IOException {
        FileType fileType = FileType.get(archivePath.toFile());
        if (fileType == null || fileType.getArchiveFormat() == null) {
            throw new IOException(
                    "Cannot detect archive format from file: " + archivePath + ". Please specify format explicitly.");
        }

        return new ArchiveHandleImpl(archivePath, fileType);
    }

    /**
     * Opens an archive from an input stream with explicit format specification.
     *
     * <p>
     * Use this method when reading archives from streams where the format cannot be
     * auto-detected from a file
     * extension.
     *
     * @param inputStream the input stream containing the archive data
     * @param format      the archive format
     * @return an ArchiveHandle for reading the archive
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    public static ArchiveHandle open(@Nonnull InputStream inputStream, @Nonnull ArchiveFormat format)
            throws IOException {
        // TODO: Implement stream-based opening
        throw new UnsupportedOperationException("Stream-based opening not yet implemented in prototype");
    }

    /**
     * Extracts all contents of an archive to the specified directory. This is a
     * convenience method for simple
     * extraction without needing to create a handle.
     *
     * <p>
     * The archive format and compression are auto-detected from the file extension.
     *
     * <p>
     * Example:
     *
     * <pre>{@code
     * Compress4J.extractAll(Paths.get("backup.tar.gz"), Paths.get("/restore"));
     * }</pre>
     *
     * @param archivePath path to the archive file
     * @param destination directory to extract files to
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    public static void extractAll(@Nonnull Path archivePath, @Nonnull Path destination) throws IOException {
        try (var handle = open(archivePath)) {
            handle.extractAll(destination);
        }
    }

    /**
     * Lists all entry names in an archive. This is a convenience method that
     * returns a simple list of filenames.
     *
     * <p>
     * Example:
     *
     * <pre>{@code
     * List<String> files = Compress4J.list(Paths.get("archive.zip"));
     * files.forEach(System.out::println);
     * }</pre>
     *
     * @param archivePath path to the archive file
     * @return list of entry names in the archive
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    public static List<String> list(@Nonnull Path archivePath) throws IOException {
        try (var handle = open(archivePath)) {
            return handle.stream().map(ArchiveExtractor.Entry::name).collect(Collectors.toList());
        }
    }

    /**
     * Creates a new archive builder for the specified format.
     *
     * <p>
     * Example:
     *
     * <pre>{@code
     * try (var builder = Compress4J.create(Paths.get("output.tar.gz"), ArchiveFormat.TAR)
     *         .withCompression(CompressionType.GZIP)) {
     *     builder.add(Paths.get("file1.txt"))
     *             .addDirectory(Paths.get("docs"))
     *             .build();
     * }
     * }</pre>
     *
     * @param outputPath path where the archive will be created
     * @param format     the archive format to use
     * @return a new archive builder
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    public static ArchiveBuilder create(@Nonnull Path outputPath, @Nonnull ArchiveFormat format) throws IOException {
        return new ArchiveBuilderImpl(outputPath, format);
    }

    /**
     * Creates a new archive builder writing to an output stream.
     *
     * @param outputStream the output stream to write the archive to
     * @param format       the archive format to use
     * @return an ArchiveBuilder for creating the archive
     * @since 3.0
     */
    public static ArchiveBuilder create(@Nonnull OutputStream outputStream, @Nonnull ArchiveFormat format) {
        // TODO: Implement builder creation
        throw new UnsupportedOperationException("Archive creation not yet implemented in prototype");
    }

    /**
     * Compresses a single file using the specified compression type.
     *
     * <p>
     * The output file will have the appropriate extension added (e.g., .gz, .bz2,
     * .xz).
     *
     * <p>
     * Example:
     *
     * <pre>{@code
     * // Creates large_file.txt.gz
     * Compress4J.compress(Paths.get("large_file.txt"), CompressionType.GZIP);
     * }</pre>
     *
     * @param inputPath       path to the file to compress
     * @param compressionType the compression type to use
     * @return path to the compressed file
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    public static Path compress(@Nonnull Path inputPath, @Nonnull CompressionType compressionType) throws IOException {
        // TODO: Implement standalone compression
        throw new UnsupportedOperationException("Standalone compression not yet implemented in prototype");
    }

    /**
     * Decompresses a compressed file.
     *
     * <p>
     * The compression type is auto-detected from the file extension.
     *
     * <p>
     * Example:
     *
     * <pre>{@code
     * // Creates large_file.txt from large_file.txt.gz
     * Compress4J.decompress(Paths.get("large_file.txt.gz"));
     * }</pre>
     *
     * @param compressedPath path to the compressed file
     * @return path to the decompressed file
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    public static Path decompress(@Nonnull Path compressedPath) throws IOException {
        // TODO: Implement standalone decompression
        throw new UnsupportedOperationException("Standalone decompression not yet implemented in prototype");
    }
}
