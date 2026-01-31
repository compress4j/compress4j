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

import io.github.compress4j.archivers.CompressionType;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.BiPredicate;

/**
 * Fluent builder interface for creating archives.
 *
 * <p>Provides a fluent API for adding files and directories to an archive, optionally with compression.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (var builder = Compress4J.create(Paths.get("output.tar.gz"), ArchiveFormat.TAR)
 *         .withCompression(CompressionType.GZIP)) {
 *     builder.add(Paths.get("file1.txt"))
 *            .add("docs/readme.txt", Paths.get("README.txt"))
 *            .addDirectory(Paths.get("src"))
 *            .build();
 * }
 * }</pre>
 *
 * @since 3.0
 */
public interface ArchiveBuilder extends AutoCloseable {

    /**
     * Adds a file to the archive.
     *
     * @param file path to the file to add
     * @return this builder for method chaining
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    ArchiveBuilder add(@Nonnull Path file) throws IOException;

    /**
     * Adds a file to the archive with a custom entry name.
     *
     * @param entryName the name to use for the entry in the archive
     * @param file path to the file to add
     * @return this builder for method chaining
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    ArchiveBuilder add(@Nonnull String entryName, @Nonnull Path file) throws IOException;

    /**
     * Adds content from an input stream to the archive.
     *
     * @param entryName the name to use for the entry in the archive
     * @param content the input stream containing the content
     * @return this builder for method chaining
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    ArchiveBuilder add(@Nonnull String entryName, @Nonnull InputStream content) throws IOException;

    /**
     * Adds content from a byte array to the archive.
     *
     * @param entryName the name to use for the entry in the archive
     * @param content the byte array containing the content
     * @return this builder for method chaining
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    ArchiveBuilder add(@Nonnull String entryName, @Nonnull byte[] content) throws IOException;

    /**
     * Adds a directory recursively to the archive.
     *
     * @param directory path to the directory to add
     * @return this builder for method chaining
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    ArchiveBuilder addDirectory(@Nonnull Path directory) throws IOException;

    /**
     * Adds a directory recursively to the archive with a custom entry name.
     *
     * @param entryName the name to use for the directory entry in the archive
     * @param directory path to the directory to add
     * @return this builder for method chaining
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    ArchiveBuilder addDirectory(@Nonnull String entryName, @Nonnull Path directory) throws IOException;

    /**
     * Enables compression for this archive.
     *
     * @param compressionType the compression type to use
     * @return this builder for method chaining
     * @since 3.0
     */
    ArchiveBuilder withCompression(@Nonnull CompressionType compressionType);

    /**
     * Sets a filter for which files to include when adding directories.
     *
     * <p>Example:
     *
     * <pre>{@code
     * builder.withFilter((name, path) -> !name.startsWith("."))
     *        .addDirectory(Paths.get("src"));
     * }</pre>
     *
     * @param filter predicate to filter files (entry name, file path) -> include
     * @return this builder for method chaining
     * @since 3.0
     */
    ArchiveBuilder withFilter(@Nonnull BiPredicate<String, Path> filter);

    /**
     * Finalizes and writes the archive.
     *
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    void build() throws IOException;

    /**
     * Closes this builder and releases any system resources. If build() has not been called, the archive may be
     * incomplete.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    void close() throws IOException;
}
