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
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Handle for reading and iterating through archive contents.
 *
 * <p>Provides multiple ways to access archive entries: streaming, iteration, and extraction. Implements
 * {@link Iterable} to support for-each loops and {@link AutoCloseable} for try-with-resources.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Stream API
 * try (var handle = Compress4J.open(archivePath)) {
 *     handle.stream()
 *         .filter(e -> e.name().endsWith(".txt"))
 *         .forEach(e -> System.out.println(e.name()));
 * }
 *
 * // Iterator API
 * try (var handle = Compress4J.open(archivePath)) {
 *     for (var entry : handle) {
 *         System.out.println(entry.name());
 *     }
 * }
 * }</pre>
 *
 * @since 3.0
 */
public interface ArchiveHandle extends AutoCloseable, Iterable<ArchiveExtractor.Entry> {

    /**
     * Returns a stream of all entries in the archive. This allows functional-style operations.
     *
     * <p>Example:
     *
     * <pre>{@code
     * try (var handle = Compress4J.open(archivePath)) {
     *     long count = handle.stream()
     *         .filter(e -> !e.type().equals(ArchiveExtractor.Entry.Type.DIR))
     *         .count();
     *     System.out.println("File count: " + count);
     * }
     * }</pre>
     *
     * @return a stream of archive entries
     * @since 3.0
     */
    Stream<ArchiveExtractor.Entry> stream();

    /**
     * Returns an iterator over all entries in the archive.
     *
     * <p>Enables for-each loops:
     *
     * <pre>{@code
     * for (var entry : handle) {
     *     System.out.println(entry.name());
     * }
     * }</pre>
     *
     * @return an iterator over archive entries
     * @since 3.0
     */
    @Override
    Iterator<ArchiveExtractor.Entry> iterator();

    /**
     * Retrieves the next entry from the archive manually. Returns null when no more entries are available.
     *
     * <p>Traditional iteration pattern:
     *
     * <pre>{@code
     * var entry = handle.next();
     * while (entry != null) {
     *     System.out.println(entry.name());
     *     entry = handle.next();
     * }
     * }</pre>
     *
     * @return the next entry or null if no more entries
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    ArchiveExtractor.Entry next() throws IOException;

    /**
     * Extracts all archive contents to the specified directory.
     *
     * @param destination the directory to extract files to
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    void extractAll(@Nonnull Path destination) throws IOException;

    /**
     * Extracts a single entry by name.
     *
     * @param entryName the name of the entry to extract
     * @param destination the directory to extract to
     * @return the path to the extracted file, or null if entry not found
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    Path extract(@Nonnull String entryName, @Nonnull Path destination) throws IOException;

    /**
     * Extracts entries matching the given predicate.
     *
     * <p>Example:
     *
     * <pre>{@code
     * // Extract only .log files
     * List<Path> extracted = handle.extract(
     *     e -> e.name().endsWith(".log"),
     *     outputDir
     * );
     * }</pre>
     *
     * @param filter predicate to match entries
     * @param destination the directory to extract to
     * @return list of extracted file paths
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    List<Path> extract(@Nonnull Predicate<ArchiveExtractor.Entry> filter, @Nonnull Path destination) throws IOException;

    /**
     * Gets an input stream for reading the content of a specific entry.
     *
     * @param entry the entry to read
     * @return an input stream for the entry's content
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    InputStream getEntryContent(@Nonnull ArchiveExtractor.Entry entry) throws IOException;

    /**
     * Returns the archive format of this archive.
     *
     * @return the archive format
     * @since 3.0
     */
    ArchiveFormat getFormat();

    /**
     * Closes this archive handle and releases any system resources associated with it.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    void close() throws IOException;
}
