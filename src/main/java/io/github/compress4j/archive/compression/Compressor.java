/*
 * Copyright 2024 The Compress4J Project
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

import static io.github.compress4j.utils.FileUtils.DOS_HIDDEN;
import static io.github.compress4j.utils.FileUtils.DOS_READ_ONLY;

import io.github.compress4j.utils.PosixFilePermissionsMapper;
import io.github.compress4j.utils.StringUtil;
import io.github.compress4j.utils.SystemUtils;
import jakarta.annotation.Nullable;
import java.beans.Statement;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is the superclass of all classes providing compression. This class provides functionality to add
 * files and directories to an archive.
 *
 * @param <T> The type of {@link ArchiveOutputStream} to write entries to.
 */
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public abstract class Compressor<T extends ArchiveOutputStream<? extends ArchiveEntry>> implements Closeable {
    /** Compression-level for the archive file. Only values in [0-9] are allowed. */
    protected static final String COMPRESSION_LEVEL = "compression-level";

    private static final Logger LOGGER = LoggerFactory.getLogger(Compressor.class);
    private Optional<BiPredicate<? super String, ? super Path>> entryFilter = Optional.empty();

    /**
     * Filtering entries being added to the archive.
     *
     * @param filter the BiPredicate to filter entries to be added to the archive. The first parameter is the entry name
     *     and the second is the {@code Path} to the file on disk, which might be {@code null} when it is applied to an
     *     entry not present on a disk, i.e. via {@link #addFile(String, byte[])}.
     * @return this compressor
     */
    public Compressor<T> withFilter(@Nullable BiPredicate<? super String, ? super Path> filter) {
        entryFilter = Optional.ofNullable(filter);
        return this;
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
    private void addFile(String entryName, Path path, Optional<FileTime> modTime) throws IOException {
        entryName = entryName(entryName);
        if (accept(entryName, path)) {
            addFile(path, Files.readAttributes(path, BasicFileAttributes.class), entryName, modTime);
        }
    }

    /**
     * Sanitise the name.
     *
     * <p>Replace `\` with `/` and remove leading and trailing `/` characters.
     *
     * @param name name to be sanitised
     */
    private static String entryName(String name) {
        StringUtils.stripEnd(name.replace('\\', '/'), "/");
        String entryName = StringUtil.trimLeading(StringUtil.trimTrailing(name.replace('\\', '/'), '/'), '/');
        if (entryName.isEmpty()) throw new IllegalArgumentException("Invalid entry name: " + name);
        return entryName;
    }

    /**
     * Add a directory recursively to the archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param path {@code Path} to add
     */
    private boolean accept(String entryName, @Nullable Path path) {
        return entryFilter.map(f -> f.test(entryName, path)).orElse(true);
    }

    /**
     * Add a {@code Path} to the archive.
     *
     * @param path {@code Path} to add
     * @param attrs attributes of the {@code Path}
     * @param name name of the entry
     * @param modTime last modification time of the {@code Path}
     * @throws IOException if an I/O error occurred
     */
    private void addFile(Path path, BasicFileAttributes attrs, String name, Optional<FileTime> modTime)
            throws IOException {
        try (InputStream source = Files.newInputStream(path)) {
            FileTime fileTime = modTime.orElse(attrs.lastModifiedTime());
            if (attrs.isSymbolicLink()) {
                writeFileEntry(name, source, attrs.size(), fileTime, mode(path), Files.readSymbolicLink(path));
            } else {
                writeFileEntry(name, source, attrs.size(), fileTime, mode(path));
            }
        }
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
    protected abstract void writeFileEntry(
            String name, InputStream source, long length, FileTime modTime, int mode, Path symlinkTarget)
            throws IOException;

    /**
     * Get mode of the {@code Path}.
     *
     * @param path {@code Path} to get the mode of
     * @return the {@code Path} mode
     */
    private static int mode(Path path) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            DosFileAttributeView attrs = Files.getFileAttributeView(path, DosFileAttributeView.class);
            if (attrs != null) {
                DosFileAttributes dosAttrs = attrs.readAttributes();
                int mode = 0;
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
        return 0;
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
    protected abstract void writeFileEntry(String name, InputStream source, long length, FileTime modTime, int mode)
            throws IOException;

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
    private void addFile(String entryName, byte[] content, Optional<FileTime> modTime) throws IOException {
        entryName = entryName(entryName);
        if (accept(entryName, null)) {
            writeFileEntry(
                    entryName,
                    new ByteArrayInputStream(content),
                    content.length,
                    modTime.orElse(FileTime.from(Instant.now())),
                    0);
        }
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
    private void addFile(String entryName, InputStream content, Optional<FileTime> modTime) throws IOException {
        entryName = entryName(entryName);
        if (accept(entryName, null)) {
            writeFileEntry(entryName, content, -1, modTime.orElse(FileTime.from(Instant.now())), 0);
        }
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
     * Add a directory to the archive. The last modification time of the directory will be used as the last modification
     * time of the entry.
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
     * Add a directory to the archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param entryName name of the entry
     * @param modTime last modification time to be used for the entry
     * @throws IOException if an I/O error occurred
     */
    private void addDirectory(String entryName, Optional<FileTime> modTime) throws IOException {
        entryName = entryName(entryName);
        if (accept(entryName, null)) {
            writeDirectoryEntry(entryName, modTime.orElse(FileTime.from(Instant.now())));
        }
    }

    /**
     * Write a directory entry to the archive.
     *
     * @param name name of the entry
     * @param modTime last modification time of the directory
     * @throws IOException if an I/O error occurred
     */
    protected abstract void writeDirectoryEntry(String name, FileTime modTime) throws IOException;

    /**
     * Add a directory to the archive.
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

    /**
     * Add a directory recursively to the archive. The last modification time of the directory will be used as the last
     * modification time of the entry.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param directory directory to add
     * @throws IOException if an I/O error occurred
     */
    public final void addDirectory(Path directory) throws IOException {
        addDirectory("", directory);
    }

    /**
     * Add a directory recursively to the archive. The last modification time of the directory will be used as the last
     * modification time of the entry.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param prefix prefix to add to the directory name
     * @param directory directory to add
     * @throws IOException if an I/O error occurred
     */
    public final void addDirectory(String prefix, Path directory) throws IOException {
        prefix = prefix.isEmpty() ? "" : entryName(prefix);
        addRecursively(prefix, directory, Optional.empty());
    }

    /**
     * Add a directory recursively to the archive.
     *
     * @param prefix prefix to add to the directory name
     * @param root directory to add
     * @param modTime last modification time of the directory
     * @throws IOException if an I/O error occurred
     */
    private void addRecursively(String prefix, Path root, Optional<FileTime> modTime) throws IOException {
        LOGGER.atTrace().log("dir={} prefix={}", root, prefix);

        Files.walkFileTree(root, new PathSimpleFileVisitor<>(this, root, prefix, modTime));

        LOGGER.atTrace().log(".");
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
    public final void addDirectory(Path directory, FileTime modTime) throws IOException {
        addDirectory("", directory, modTime);
    }

    /**
     * Add a directory recursively to the archive.
     *
     * <p>Predicate {@link #entryFilter} will be applied.
     *
     * @param prefix prefix to add to the directory name
     * @param directory directory to add
     * @param modTime last modification time of the directory
     * @throws IOException if an I/O error occurred
     */
    public final void addDirectory(String prefix, Path directory, FileTime modTime) throws IOException {
        prefix = prefix.isEmpty() ? "" : entryName(prefix);
        addRecursively(prefix, directory, Optional.of(modTime));
    }

    /**
     * Apply options to archive output stream
     *
     * @param stream stream to apply options to
     * @param options options map
     * @return stream with option applied
     * @throws IOException if an IO error occurred
     */
    protected T applyFormatOptions(T stream, Map<String, Object> options) throws IOException {
        for (Map.Entry<String, Object> option : options.entrySet()) {
            try {
                if (option.getKey().equals(COMPRESSION_LEVEL)) {
                    continue;
                }
                new Statement(stream, "set" + StringUtils.capitalize(option.getKey()), new Object[] {option.getValue()})
                        .execute();
            } catch (Exception e) {
                throw new IOException("Cannot set option " + option.getKey(), e);
            }
        }
        return stream;
    }

    /**
     * Removes and returns the {@link #COMPRESSION_LEVEL} key from the input map parameter if it exists, or -1 if this
     * key does not exist.
     *
     * @param o options map
     * @return The compression level if it exists in the map, or -1 instead.
     * @throws IllegalArgumentException if the {@link #COMPRESSION_LEVEL} option does not parse to an Integer.
     */
    protected int getCompressionLevel(Map<String, Object> o) {
        return Optional.ofNullable(o.remove(COMPRESSION_LEVEL))
                .map(value -> {
                    try {
                        return (Integer) value;
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("Cannot set compression level " + value, e);
                    }
                })
                .orElse(-1);
    }

    /**
     * Start a new archive. Entries can be included in the archive using the putEntry method, and then the archive
     * should be closed using its close method.
     *
     * @param s underlying output stream to which to write the archive.
     * @return new archive object for use in putEntry
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    protected abstract T createArchiveOutputStream(OutputStream s) throws IOException;

    /**
     * Start a new archive. Entries can be included in the archive using the putEntry method, and then the archive
     * should be closed using its close method. In addition, options can be applied to the underlying stream. E.g.
     * compression level.
     *
     * @param s underlying output stream to which to write the archive.
     * @param o options to apply to the underlying output stream. Keys are option names and values are option values.
     * @return new archive object for use in putEntry
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    protected abstract T createArchiveOutputStream(OutputStream s, Map<String, Object> o) throws IOException;

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
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            String name = dir == root ? prefix : entryName(dir);
            if (name.isEmpty()) {
                return FileVisitResult.CONTINUE;
            } else if (compressor.accept(name, dir)) {
                LOGGER.atTrace().log("  {} -> {}/", dir, name);
                compressor.writeDirectoryEntry(name, modTime.orElse(attrs.lastModifiedTime()));
                return FileVisitResult.CONTINUE;
            } else {
                return FileVisitResult.SKIP_SUBTREE;
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String name = entryName(file);
            if (compressor.accept(name, file)) {
                LOGGER.atTrace()
                        .log("  {} -> {}{}", file, name, attrs.isSymbolicLink() ? " symlink" : " size=" + attrs.size());
                compressor.addFile(file, attrs, name, modTime);
            }
            return FileVisitResult.CONTINUE;
        }

        private String entryName(Path fileOrDir) {
            String relativeName =
                    Compressor.entryName(root.relativize(fileOrDir).toString());
            return prefix.isEmpty() ? relativeName : prefix + '/' + relativeName;
        }
    }
}
