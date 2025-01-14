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

import static io.github.compress4j.utils.FileUtils.*;
import static io.github.compress4j.utils.StringUtil.trimLeading;
import static io.github.compress4j.utils.StringUtil.trimTrailing;
import static java.util.zip.Deflater.DEFAULT_COMPRESSION;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import io.github.compress4j.utils.PosixFilePermissionsMapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.beans.Statement;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.*;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.zip.Deflater;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is the superclass of all classes providing compression. This class provides functionality to add
 * files and directories to an archive.
 *
 * @param <A> The type of {@link ArchiveOutputStream} to write entries to.
 */
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType"})
public abstract class Compressor<A extends ArchiveOutputStream<? extends ArchiveEntry>> implements AutoCloseable {
    /** Compression-level for the archive file. Only values in [0-9] are allowed. */
    public static final String COMPRESSION_LEVEL = "compression-level";

    private static final Logger LOGGER = LoggerFactory.getLogger(Compressor.class);
    private Optional<BiPredicate<? super String, ? super Path>> entryFilter = Optional.empty();

    /** Archive output stream to be used for compression. */
    protected A archiveOutputStream;

    /**
     * Create a new Compressor.
     *
     * @param outputStream the output stream
     * @throws IOException if an I/O error occurred
     */
    protected Compressor(OutputStream outputStream) throws IOException {
        archiveOutputStream = buildArchiveOutputStream(outputStream);
    }

    /**
     * Create a new Compressor with the given output stream and options.
     *
     * @param outputStream the output outputStream
     * @param options the options for the compressor
     * @throws IOException if an I/O error occurred
     */
    protected Compressor(OutputStream outputStream, Map<String, Object> options) throws IOException {
        archiveOutputStream = buildArchiveOutputStream(outputStream, options);
    }

    /**
     * Start a new archive. Entries can be included in the archive using the putEntry method, and then the archive
     * should be closed using its close method. In addition, options can be applied to the underlying stream. E.g.
     * compression level.
     *
     * @param outputStream underlying output stream to which to write the archive.
     * @param options options to apply to the underlying output stream. Keys are option names and values are option
     *     values.
     * @return new archive object for use in putEntry
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    protected abstract A buildArchiveOutputStream(OutputStream outputStream, Map<String, Object> options)
            throws IOException;

    /**
     * Write a directory entry to the archive.
     *
     * @param name name of the entry
     * @param modTime last modification time of the directory
     * @throws IOException if an I/O error occurred
     */
    protected abstract void writeDirectoryEntry(String name, FileTime modTime) throws IOException;

    /**
     * Write a file entry to the archive.
     *
     * @param name name of the entry
     * @param source input stream to read the file from
     * @param length length of the file
     * @param modTime last modification time of the file
     * @param mode file mode
     * @param symlinkTarget target of the symbolic link, or {@code null} if the entry is not a symbolic link
     * @throws IOException if an I/O error occurred
     */
    protected abstract void writeFileEntry(
            String name, InputStream source, long length, FileTime modTime, int mode, Optional<Path> symlinkTarget)
            throws IOException;

    /**
     * Add a directory to the archive. The last modification time of the directory will be used as the last modification
     * time of the entry. This method creates a directory entry without any content.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @throws IOException if an I/O error occurred
     */
    public final void addDirectory(String entryName) throws IOException {
        addDirectory(entryName, Optional.empty());
    }

    /**
     * Add a directory to the archive. This method creates a directory entry without any content.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param modTime last modification time to be used for the entry
     * @throws IOException if an I/O error occurred
     */
    public final void addDirectory(String entryName, FileTime modTime) throws IOException {
        addDirectory(entryName, Optional.of(modTime));
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        archiveOutputStream.close();
    }

    /**
     * Add a directory to the archive. This method creates a directory entry without any content.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param modTime last modification time to be used for the entry
     * @throws IOException if an I/O error occurred
     */
    private void addDirectory(String entryName, Optional<FileTime> modTime) throws IOException {
        entryName = sanitiseName(entryName);
        if (accept(entryName, null)) {
            writeDirectoryEntry(entryName, modTime.orElse(FileTime.from(Instant.now())));
        }
    }

    /**
     * Add a directory recursively to the archive. The last modification time of the directory will be used as the last
     * modification time of the entry.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param directory directory to add
     * @throws IOException if an I/O error occurred
     */
    public final void addDirectoryRecursively(Path directory) throws IOException {
        addDirectoryRecursively("", directory);
    }

    /**
     * Add a directory recursively to the archive. The last modification time of the directory will be used as the last
     * modification time of the entry.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param topLevelDir topLevelDir to add to the directory name
     * @param directory directory to add
     * @throws IOException if an I/O error occurred
     */
    public final void addDirectoryRecursively(String topLevelDir, Path directory) throws IOException {
        addDirectoryRecursively(topLevelDir, directory, Optional.empty());
    }

    /**
     * Add a directory recursively to the archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param directory directory to add
     * @param modTime last modification time of the directory
     * @throws IOException if an I/O error occurred
     */
    public final void addDirectoryRecursively(Path directory, FileTime modTime) throws IOException {
        addDirectoryRecursively("", directory, modTime);
    }

    /**
     * Add a directory recursively to the archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param topLevelDir topLevelDir to add to the directory name
     * @param directory directory to add
     * @param modTime last modification time of the directory
     * @throws IOException if an I/O error occurred
     */
    public final void addDirectoryRecursively(String topLevelDir, Path directory, FileTime modTime) throws IOException {
        addDirectoryRecursively(topLevelDir, directory, Optional.of(modTime));
    }

    /**
     * Add a directory recursively to the archive using a {@code SimpleFileVisitor}.
     *
     * @param topLevelDir when a non-empty value specified, create a directory entry with this name and add all entries
     * @param directory directory to add
     * @param modTime last modification time of the directory
     * @throws IOException if an I/O error occurred
     */
    private void addDirectoryRecursively(String topLevelDir, Path directory, Optional<FileTime> modTime)
            throws IOException {
        topLevelDir = topLevelDir.isEmpty() ? "" : sanitiseName(topLevelDir);
        LOGGER.atTrace().log("dir={} topLevelDir={}", directory, topLevelDir);

        Files.walkFileTree(directory, new PathSimpleFileVisitor<>(this, directory, topLevelDir, modTime));

        LOGGER.atTrace().log(".");
    }

    /**
     * Add {@code Path} to archive. The last modification time of the file will be used as the last modification time of
     * the entry.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param path path to add
     * @throws IOException if an I/O error occurred
     */
    public final void addFile(Path path) throws IOException {
        addFile(path.getFileName().toString(), path, Optional.empty());
    }

    /**
     * Add {@code Path} to archive. The last modification time of the file will be used as the last modification time of
     * the entry.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param path path to add
     * @throws IOException if an I/O error occurred
     */
    public final void addFile(String entryName, Path path) throws IOException {
        addFile(entryName, path, Optional.empty());
    }

    /**
     * Add {@code Path} to archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param path {@code Path} to add
     * @param modTime last modification time to be used for the entry
     * @throws IOException if an I/O error occurred
     */
    public final void addFile(String entryName, Path path, FileTime modTime) throws IOException {
        addFile(entryName, path, Optional.of(modTime));
    }

    /**
     * Add {@code Path} to archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param path {@code Path} to add
     * @param modTime last modification time to be used for the entry
     * @throws IOException if an I/O error occurred
     */
    private void addFile(String entryName, Path path, Optional<FileTime> modTime) throws IOException {
        addFile(entryName, path, Files.readAttributes(path, BasicFileAttributes.class), modTime);
    }

    /**
     * Add {@code byte[]} to the archive. The last modification time of the file will be used as the last modification
     * time of the entry.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param content {@code byte[]} to add
     * @throws IOException if an I/O error occurred
     */
    public final void addFile(String entryName, byte[] content) throws IOException {
        addFile(entryName, content, Optional.empty());
    }

    /**
     * Add {@code byte[]} to the archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param content {@code byte[]} to add
     * @param modTime last modification time to be used for the entry
     * @throws IOException if an I/O error occurred
     */
    public final void addFile(String entryName, byte[] content, FileTime modTime) throws IOException {
        addFile(entryName, content, Optional.of(modTime));
    }

    /**
     * Add {@code byte[]} to the archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param content {@code byte[]} to add
     * @param modTime last modification time to be used for the entry
     * @throws IOException if an I/O error occurred
     */
    private void addFile(String entryName, byte[] content, Optional<FileTime> modTime) throws IOException {
        entryName = sanitiseName(entryName);
        if (accept(entryName, null)) {
            writeFileEntry(
                    entryName,
                    new ByteArrayInputStream(content),
                    content.length,
                    modTime.orElse(FileTime.from(Instant.now())),
                    NO_MODE);
        }
    }

    /**
     * Add {@code InputStream} to the archive. The last modification time of the file will be used as the last
     * modification time of the entry.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param content {@code InputStream} to add
     * @throws IOException if an I/O error occurred
     */
    public final void addFile(String entryName, InputStream content) throws IOException {
        addFile(entryName, content, Optional.empty());
    }

    /**
     * Add {@code InputStream} to the archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param content {@code InputStream} to add
     * @param modTime last modification time to be used for the entry
     * @throws IOException if an I/O error occurred
     */
    public final void addFile(String entryName, InputStream content, FileTime modTime) throws IOException {
        addFile(entryName, content, Optional.of(modTime));
    }

    /**
     * Add {@code InputStream} to the archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param content {@code InputStream} to add
     * @param modTime last modification time to be used for the entry
     * @throws IOException if an I/O error occurred
     */
    private void addFile(String entryName, InputStream content, Optional<FileTime> modTime) throws IOException {
        entryName = sanitiseName(entryName);
        if (accept(entryName, null)) {
            writeFileEntry(entryName, content, -1, modTime.orElse(FileTime.from(Instant.now())), NO_MODE);
        }
    }

    /**
     * Add a {@code Path} to the archive.
     *
     * @param path {@code Path} to add
     * @param attrs attributes of the {@code Path}
     * @param entryName entryName of the entry
     * @param modTime last modification time of the {@code Path}
     * @throws IOException if an I/O error occurred
     */
    public final void addFile(String entryName, Path path, BasicFileAttributes attrs, Optional<FileTime> modTime)
            throws IOException {
        entryName = sanitiseName(entryName);
        if (accept(entryName, path)) {
            try (InputStream source = Files.newInputStream(path)) {
                FileTime fileTime = modTime.orElse(attrs.lastModifiedTime());
                if (attrs.isSymbolicLink()) {
                    writeFileEntry(entryName, source, attrs.size(), fileTime, mode(path), Files.readSymbolicLink(path));
                } else {
                    writeFileEntry(entryName, source, attrs.size(), fileTime, mode(path));
                }
            }
        }
    }

    /**
     * Removes and returns the {@link #COMPRESSION_LEVEL} key from the input map parameter if it exists, or -1 if this
     * key does not exist.
     *
     * @param o options map
     * @return The compression level if it exists in the map, otherwise {@link Deflater#DEFAULT_COMPRESSION}.
     * @throws IllegalArgumentException if the {@link #COMPRESSION_LEVEL} option does not parse to an Integer.
     */
    public static int getCompressionLevel(Map<String, Object> o) {
        return Optional.ofNullable(o.get(COMPRESSION_LEVEL))
                .map(value -> {
                    try {
                        return (Integer) value;
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("Cannot set compression level " + value, e);
                    }
                })
                .orElse(DEFAULT_COMPRESSION);
    }

    /**
     * Filtering entries being added to the archive.
     *
     * @param filter the BiPredicate to filter entries to be added to the archive. The first parameter is the entry name
     *     and the second is the {@code Path} to the file on disk, which might be {@code null} when it is applied to an
     *     entry not present on a disk, i.e. via {@link #addFile(String, byte[])}.
     */
    public void withFilter(@Nullable BiPredicate<? super String, ? super Path> filter) {
        entryFilter = Optional.ofNullable(filter);
    }

    /**
     * Apply options to archive output stream
     *
     * @param stream stream to apply options to
     * @param options options map
     * @return stream with option applied
     * @throws IOException if an IO error occurred
     */
    protected A applyFormatOptions(A stream, Map<String, Object> options) throws IOException {
        for (Map.Entry<String, Object> option : options.entrySet()) {
            try {
                new Statement(stream, "set" + StringUtils.capitalize(option.getKey()), new Object[] {option.getValue()})
                        .execute();
            } catch (Exception e) {
                throw new IOException("Cannot set option: " + option.getKey(), e);
            }
        }
        return stream;
    }

    /**
     * Start a new archive. Entries can be included in the archive using the putEntry method, and then the archive
     * should be closed using its close method.
     *
     * @param s underlying output stream to which to write the archive.
     * @return new archive object for use in putEntry
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    protected A buildArchiveOutputStream(OutputStream s) throws IOException {
        return buildArchiveOutputStream(s, Collections.emptyMap());
    }

    /**
     * Write a file entry to the archive.
     *
     * @param name name of the entry
     * @param source input stream to read the file from
     * @param length length of the file
     * @param modTime last modification time of the file
     * @param mode file mode
     * @throws IOException if an I/O error occurred
     */
    protected void writeFileEntry(String name, InputStream source, long length, FileTime modTime, int mode)
            throws IOException {
        writeFileEntry(name, source, length, modTime, mode, Optional.empty());
    }

    /**
     * Write a file entry to the archive.
     *
     * @param name name of the entry
     * @param source input stream to read the file from
     * @param length length of the file
     * @param modTime last modification time of the file
     * @param mode file mode
     * @param symlinkTarget target of the symbolic link, or {@code null} if the entry is not a symbolic link
     * @throws IOException if an I/O error occurred
     */
    protected void writeFileEntry(
            String name, InputStream source, long length, FileTime modTime, int mode, Path symlinkTarget)
            throws IOException {
        writeFileEntry(name, source, length, modTime, mode, Optional.of(symlinkTarget));
    }

    /**
     * Add a directory recursively to the archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param path {@code Path} to add
     */
    protected boolean accept(String entryName, @Nullable Path path) {
        return entryFilter.map(f -> f.test(entryName, path)).orElse(true);
    }

    /**
     * Get mode of the {@code Path}.
     *
     * @param path {@code Path} to get the mode of
     * @return the {@code Path} mode
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    protected static int mode(Path path) throws IOException {
        if (isIsOsWindows()) {
            DosFileAttributeView attrs = Files.getFileAttributeView(path, DosFileAttributeView.class);
            if (attrs != null) {
                DosFileAttributes dosAttrs = attrs.readAttributes();
                int mode = NO_MODE;
                if (dosAttrs.isReadOnly()) mode |= DOS_READ_ONLY;
                if (dosAttrs.isHidden()) mode |= DOS_HIDDEN;
                return mode;
            }
        } else {
            PosixFileAttributeView attrs = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            if (attrs != null) {
                return PosixFilePermissionsMapper.toUnixMode(
                        attrs.readAttributes().permissions());
            }
        }
        return NO_MODE;
    }

    /**
     * Check if the OS is Windows.
     *
     * @return {@code true} if the OS is Windows, {@code false} otherwise
     */
    protected static boolean isIsOsWindows() {
        return IS_OS_WINDOWS;
    }

    /**
     * Sanitise the name.
     *
     * <p>Replace `\` with `/` and remove leading and trailing `/` characters.
     *
     * @param name name to be sanitised
     * @return sanitised name
     */
    @SuppressWarnings("java:S5361")
    public static String sanitiseName(String name) {
        String entryName = trimLeading(trimTrailing(name.replaceAll("\\\\", "/"), '/'), '/');
        if (StringUtils.isBlank(entryName)) throw new IllegalArgumentException("Invalid entry name: " + name);
        return entryName;
    }

    private static class PathSimpleFileVisitor<E extends ArchiveOutputStream<? extends ArchiveEntry>>
            extends SimpleFileVisitor<Path> {
        private final Path root;
        private final String prefix;

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private final Optional<FileTime> modTime;

        private final Compressor<E> compressor;

        public PathSimpleFileVisitor(Compressor<E> compressor, Path root, String prefix, Optional<FileTime> modTime) {
            this.root = root;
            this.prefix = prefix;
            this.modTime = modTime;
            this.compressor = compressor;
        }

        @Override
        @Nonnull
        public FileVisitResult preVisitDirectory(Path dir, @Nonnull BasicFileAttributes attrs) throws IOException {
            String name = dir == root ? prefix : entryName(dir);
            if (name.isEmpty()) {
                return FileVisitResult.CONTINUE;
            } else if (compressor.accept(name, dir)) {
                LOGGER.atTrace().log("  {} -> {}/", dir, name);
                compressor.addDirectory(name, modTime.orElse(attrs.lastModifiedTime()));
                return FileVisitResult.CONTINUE;
            } else {
                return FileVisitResult.SKIP_SUBTREE;
            }
        }

        @Override
        @Nonnull
        public FileVisitResult visitFile(Path file, @Nonnull BasicFileAttributes attrs) throws IOException {
            String name = entryName(file);
            if (compressor.accept(name, file)) {
                LOGGER.atTrace()
                        .log("  {} -> {}{}", file, name, attrs.isSymbolicLink() ? " symlink" : " size=" + attrs.size());
                compressor.addFile(name, file, attrs, modTime);
            }
            return FileVisitResult.CONTINUE;
        }

        private String entryName(Path fileOrDir) {
            String relativeName =
                    Compressor.sanitiseName(root.relativize(fileOrDir).toString());
            return prefix.isEmpty() ? relativeName : prefix + '/' + relativeName;
        }
    }
}
