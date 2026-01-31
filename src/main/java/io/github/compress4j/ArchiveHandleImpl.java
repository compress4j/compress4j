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
import io.github.compress4j.archivers.FileType;
import io.github.compress4j.archivers.tar.TarArchiveExtractor;
import io.github.compress4j.archivers.zip.ZipArchiveExtractor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Default implementation of {@link ArchiveHandle} that delegates to {@link ArchiveExtractor} implementations.
 *
 * @since 3.0
 */
class ArchiveHandleImpl implements ArchiveHandle {

    private final ArchiveExtractor<?> extractor;
    private final ArchiveFormat format;
    private final Path archivePath;

    /**
     * Creates a new archive handle for the given file and type.
     *
     * @param archivePath the path to the archive file
     * @param fileType the detected file type
     * @throws IOException if an I/O error occurs opening the archive
     */
    ArchiveHandleImpl(@Nonnull Path archivePath, @Nonnull FileType fileType) throws IOException {
        this.archivePath = archivePath;
        this.format = fileType.getArchiveFormat();
        if (this.format == null) {
            throw new IOException("No archive format detected for file: " + archivePath);
        }

        // Create appropriate extractor based on format
        this.extractor = createExtractor(archivePath, fileType);
    }

    private ArchiveExtractor<?> createExtractor(Path path, FileType fileType) throws IOException {
        ArchiveFormat archiveFormat = fileType.getArchiveFormat();
        if (archiveFormat == null) {
            throw new IOException("No archive format for file type: " + fileType);
        }

        // For prototype, we'll only support uncompressed TAR and ZIP
        // Compressed archives (tar.gz, etc.) will be added in full implementation
        if (fileType.getCompressionType() != null) {
            throw new UnsupportedOperationException("Compressed archives not yet supported in prototype. "
                    + "Use uncompressed TAR or ZIP for testing.");
        }

        InputStream inputStream = Files.newInputStream(path);

        return switch (archiveFormat) {
            case TAR -> TarArchiveExtractor.builder(inputStream).build();
            case ZIP -> ZipArchiveExtractor.builder(path).build();
            default ->
                throw new UnsupportedOperationException(
                        "Format " + archiveFormat + " not yet implemented in prototype");
        };
    }

    @Override
    public Stream<ArchiveExtractor.Entry> stream() {
        return extractor.stream();
    }

    @Override
    public Iterator<ArchiveExtractor.Entry> iterator() {
        return extractor.iterator();
    }

    @Override
    public @Nullable ArchiveExtractor.Entry next() throws IOException {
        return extractor.nextEntry();
    }

    @Override
    public void extractAll(@Nonnull Path destination) throws IOException {
        extractor.extract(destination);
    }

    @Override
    public @Nullable Path extract(@Nonnull String entryName, @Nonnull Path destination) throws IOException {
        // Iterate through entries to find the matching one
        ArchiveExtractor.Entry entry;
        while ((entry = extractor.nextEntry()) != null) {
            if (entry.name().equals(entryName)) {
                // Found it - extract this entry
                // TODO: This is simplified - need proper extraction logic
                Path outputFile = destination.resolve(entry.name());
                if (entry.type() == ArchiveExtractor.Entry.Type.DIR) {
                    Files.createDirectories(outputFile);
                } else {
                    Files.createDirectories(outputFile.getParent());
                    try (InputStream in = extractor.openEntryStream(entry);
                            var out = Files.newOutputStream(outputFile)) {
                        in.transferTo(out);
                    }
                }
                return outputFile;
            }
        }
        return null; // Entry not found
    }

    @Override
    public List<Path> extract(@Nonnull Predicate<ArchiveExtractor.Entry> filter, @Nonnull Path destination)
            throws IOException {
        List<Path> extracted = new ArrayList<>();
        ArchiveExtractor.Entry entry;

        while ((entry = extractor.nextEntry()) != null) {
            if (filter.test(entry)) {
                Path outputFile = destination.resolve(entry.name());
                if (entry.type() == ArchiveExtractor.Entry.Type.DIR) {
                    Files.createDirectories(outputFile);
                } else {
                    Files.createDirectories(outputFile.getParent());
                    try (InputStream in = extractor.openEntryStream(entry);
                            var out = Files.newOutputStream(outputFile)) {
                        in.transferTo(out);
                    }
                }
                extracted.add(outputFile);
            }
        }

        return extracted;
    }

    @Override
    public InputStream getEntryContent(@Nonnull ArchiveExtractor.Entry entry) throws IOException {
        return extractor.openEntryStream(entry);
    }

    @Override
    public ArchiveFormat getFormat() {
        return format;
    }

    @Override
    public void close() throws IOException {
        extractor.close();
    }
}
