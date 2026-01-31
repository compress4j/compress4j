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

import io.github.compress4j.archivers.ArchiveCreator;
import io.github.compress4j.archivers.ArchiveFormat;
import io.github.compress4j.archivers.CompressionType;
import io.github.compress4j.archivers.ar.ArArchiveCreator;
import io.github.compress4j.archivers.cpio.CpioArchiveCreator;
import io.github.compress4j.archivers.tar.TarArchiveCreator;
import io.github.compress4j.archivers.tar.TarBZip2ArchiveCreator;
import io.github.compress4j.archivers.tar.TarGzArchiveCreator;
import io.github.compress4j.archivers.zip.ZipArchiveCreator;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Default implementation of {@link ArchiveBuilder} that wraps existing {@link ArchiveCreator} implementations.
 *
 * <p>This class uses the wrapper pattern to delegate to format-specific archive creators while providing a unified,
 * fluent API for archive creation. Operations are batched and executed when {@link #build()} is called.
 *
 * @since 3.0
 */
class ArchiveBuilderImpl implements ArchiveBuilder {

    private final Path outputPath;
    private final ArchiveFormat format;
    private CompressionType compressionType;
    private BiPredicate<String, Path> filter;
    private final List<EntrySpec> entries = new ArrayList<>();

    /**
     * Creates a new archive builder for the given output path and format.
     *
     * @param outputPath the path where the archive will be created
     * @param format the archive format to use
     */
    ArchiveBuilderImpl(@Nonnull Path outputPath, @Nonnull ArchiveFormat format) {
        this.outputPath = outputPath;
        this.format = format;
    }

    @Override
    public ArchiveBuilder add(@Nonnull Path file) throws IOException {
        return add(file.getFileName().toString(), file);
    }

    @Override
    public ArchiveBuilder add(@Nonnull String entryName, @Nonnull Path file) throws IOException {
        entries.add(new FileEntry(entryName, file));
        return this;
    }

    @Override
    public ArchiveBuilder add(@Nonnull String entryName, @Nonnull InputStream content) throws IOException {
        // Read stream immediately since we can't keep it open
        byte[] bytes = content.readAllBytes();
        entries.add(new ByteEntry(entryName, bytes));
        return this;
    }

    @Override
    public ArchiveBuilder add(@Nonnull String entryName, @Nonnull byte[] content) throws IOException {
        entries.add(new ByteEntry(entryName, content));
        return this;
    }

    @Override
    public ArchiveBuilder addDirectory(@Nonnull Path directory) throws IOException {
        return addDirectory(directory.getFileName().toString(), directory);
    }

    @Override
    public ArchiveBuilder addDirectory(@Nonnull String entryName, @Nonnull Path directory) throws IOException {
        entries.add(new DirectoryEntry(entryName, directory));
        return this;
    }

    @Override
    public ArchiveBuilder withCompression(@Nonnull CompressionType compressionType) {
        this.compressionType = compressionType;
        return this;
    }

    @Override
    public ArchiveBuilder withFilter(@Nonnull BiPredicate<String, Path> filter) {
        this.filter = filter;
        return this;
    }

    @Override
    public void build() throws IOException {
        try (ArchiveCreator<?> creator = createCreator()) {
            // Process all collected entries
            for (EntrySpec entry : entries) {
                entry.addTo(creator);
            }
        }
    }

    @Override
    public void close() throws IOException {
        // Resources are managed in build() method
        // This is a no-op but required by AutoCloseable interface
    }

    /**
     * Creates the appropriate ArchiveCreator based on format and compression settings.
     *
     * @return the configured ArchiveCreator
     * @throws IOException if creator cannot be initialized
     */
    private ArchiveCreator<?> createCreator() throws IOException {
        // Handle compressed TAR formats specially
        if (format == ArchiveFormat.TAR && compressionType != null) {
            return switch (compressionType) {
                case GZIP -> createTarGzCreator();
                case BZIP2 -> createTarBZip2Creator();
                default ->
                    throw new UnsupportedOperationException(
                            "Compression type " + compressionType + " not yet supported for TAR in ArchiveBuilder");
            };
        }

        // Handle uncompressed formats
        return switch (format) {
            case TAR -> createTarCreator();
            case ZIP -> createZipCreator();
            case AR -> createArCreator();
            case CPIO -> createCpioCreator();
            default -> throw new UnsupportedOperationException("Archive format " + format + " not yet supported");
        };
    }

    private ArchiveCreator<?> createTarCreator() throws IOException {
        var builder = TarArchiveCreator.builder(outputPath);
        if (filter != null) {
            builder.filter(filter);
        }
        return builder.build();
    }

    private ArchiveCreator<?> createTarGzCreator() throws IOException {
        var builder = TarGzArchiveCreator.builder(outputPath);
        if (filter != null) {
            builder.filter(filter);
        }
        return builder.build();
    }

    private ArchiveCreator<?> createTarBZip2Creator() throws IOException {
        var builder = TarBZip2ArchiveCreator.builder(outputPath);
        if (filter != null) {
            builder.filter(filter);
        }
        return builder.build();
    }

    private ArchiveCreator<?> createZipCreator() throws IOException {
        if (compressionType != null) {
            throw new UnsupportedOperationException("ZIP format does not support external compression");
        }
        var builder = ZipArchiveCreator.builder(outputPath);
        if (filter != null) {
            builder.filter(filter);
        }
        return builder.build();
    }

    private ArchiveCreator<?> createArCreator() throws IOException {
        if (compressionType != null) {
            throw new UnsupportedOperationException("AR format does not support external compression");
        }
        var builder = ArArchiveCreator.builder(outputPath);
        if (filter != null) {
            builder.filter(filter);
        }
        return builder.build();
    }

    private ArchiveCreator<?> createCpioCreator() throws IOException {
        if (compressionType != null) {
            throw new UnsupportedOperationException("CPIO compression not yet supported in v3 API");
        }
        var builder = CpioArchiveCreator.builder(outputPath);
        if (filter != null) {
            builder.filter(filter);
        }
        return builder.build();
    }

    /** Base class for entry specifications */
    private abstract static class EntrySpec {
        abstract void addTo(ArchiveCreator<?> creator) throws IOException;
    }

    /** File entry specification */
    private static class FileEntry extends EntrySpec {
        private final String name;
        private final Path file;

        FileEntry(String name, Path file) {
            this.name = name;
            this.file = file;
        }

        @Override
        void addTo(ArchiveCreator<?> creator) throws IOException {
            // Use the three-parameter version with FileTime
            creator.addFile(
                    name,
                    file,
                    FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis()));
        }
    }

    /** Byte array entry specification */
    private static class ByteEntry extends EntrySpec {
        private final String name;
        private final byte[] content;

        ByteEntry(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }

        @Override
        void addTo(ArchiveCreator<?> creator) throws IOException {
            creator.addFile(name, content);
        }
    }

    /** Directory entry specification */
    private static class DirectoryEntry extends EntrySpec {
        private final String name;
        private final Path directory;

        DirectoryEntry(String name, Path directory) {
            this.name = name;
            this.directory = directory;
        }

        @Override
        void addTo(ArchiveCreator<?> creator) throws IOException {
            creator.addDirectoryRecursively(name, directory);
        }
    }
}
