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
package io.github.compress4j.archivers;

import static io.github.compress4j.utils.FileUtils.DOS_HIDDEN;
import static io.github.compress4j.utils.FileUtils.DOS_READ_ONLY;
import static io.github.compress4j.utils.FileUtils.NO_MODE;
import static io.github.compress4j.utils.StringUtil.trimLeading;
import static io.github.compress4j.utils.StringUtil.trimTrailing;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import io.github.compress4j.utils.PosixFilePermissionsMapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
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
import java.util.Optional;
import java.util.function.BiPredicate;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is the superclass of all classes providing archiving. This class provides functionality to add
 * files and directories to an archive.
 *
 * @param <A> The type of {@link ArchiveOutputStream} to write entries to.
 * @since 2.2
 */
public abstract class ArchiveCreator<A extends ArchiveOutputStream<? extends ArchiveEntry>> implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveCreator.class);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<BiPredicate<? super String, ? super Path>> entryFilter = Optional.empty();

    /** Archive output stream to be used for archiving. */
    protected final A archiveOutputStream;

    /**
     * Create a new ArchiveCreator with the given output stream and options.
     *
     * @param builder the archive output stream builder
     * @param <B> The type of {@link ArchiveCreatorBuilder} to build from.
     * @param <C> The type of the {@link ArchiveCreator} to instantiate.
     * @throws IOException if an I/O error occurred
     */
    protected <B extends ArchiveCreatorBuilder<A, B, C>, C extends ArchiveCreator<A>> ArchiveCreator(B builder)
            throws IOException {
        this(builder.buildArchiveOutputStream());
        this.entryFilter = builder.entryFilter;
    }

    /**
     * Create a new ArchiveCreator.
     *
     * @param archiveOutputStream the archive output stream
     */
    protected ArchiveCreator(A archiveOutputStream) {
        this.archiveOutputStream = archiveOutputStream;
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
            String name,
            InputStream source,
            long length,
            FileTime modTime,
            int mode,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Path> symlinkTarget)
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
    private void addDirectory(
            String entryName, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<FileTime> modTime)
            throws IOException {
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
    private void addDirectoryRecursively(
            String topLevelDir,
            Path directory,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<FileTime> modTime)
            throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Path is not a directory: " + directory);
        }
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
    private void addFile(
            String entryName,
            Path path,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<FileTime> modTime)
            throws IOException {
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
    private void addFile(
            String entryName,
            byte[] content,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<FileTime> modTime)
            throws IOException {
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
    private void addFile(
            String entryName,
            InputStream content,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<FileTime> modTime)
            throws IOException {
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
    public final void addFile(
            String entryName,
            Path path,
            BasicFileAttributes attrs,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<FileTime> modTime)
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
     * @return boolean {@code true} if the entry is accepted, {@code false} otherwise
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
            } else {
                LOGGER.trace("Cannot get DOS file attributes for: {}", path);
            }
        } else {
            PosixFileAttributeView attrs = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            if (attrs != null) {
                return PosixFilePermissionsMapper.toUnixMode(
                        attrs.readAttributes().permissions());
            } else {
                LOGGER.trace("Cannot get POSIX file attributes for: {}", path);
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
        String entryName = trimLeading(trimTrailing(name.replaceAll("\\\\+", "/"), '/'), '/');
        if (StringUtils.isBlank(entryName)) throw new IllegalArgumentException("Invalid entry name: " + name);
        return entryName;
    }

    private static class PathSimpleFileVisitor<E extends ArchiveOutputStream<? extends ArchiveEntry>>
            extends SimpleFileVisitor<Path> {
        private final Path root;
        private final String prefix;

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private final Optional<FileTime> modTime;

        private final ArchiveCreator<E> archiveCreator;

        public PathSimpleFileVisitor(
                ArchiveCreator<E> archiveCreator,
                Path root,
                String prefix,
                @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<FileTime> modTime) {
            this.root = root;
            this.prefix = prefix;
            this.modTime = modTime;
            this.archiveCreator = archiveCreator;
        }

        @Override
        @Nonnull
        public FileVisitResult preVisitDirectory(@Nonnull Path dir, @Nonnull BasicFileAttributes attrs)
                throws IOException {
            String name = dir == root ? prefix : entryName(dir);
            if (name.isEmpty()) {
                return FileVisitResult.CONTINUE;
            } else if (archiveCreator.accept(name, dir)) {
                LOGGER.atTrace().log("  {} -> {}/", dir, name);
                archiveCreator.addDirectory(name, modTime.orElse(attrs.lastModifiedTime()));
                return FileVisitResult.CONTINUE;
            } else {
                return FileVisitResult.SKIP_SUBTREE;
            }
        }

        @Override
        @Nonnull
        public FileVisitResult visitFile(@Nonnull Path file, @Nonnull BasicFileAttributes attrs) throws IOException {
            String name = entryName(file);
            if (archiveCreator.accept(name, file)) {
                LOGGER.atTrace()
                        .log("  {} -> {}{}", file, name, attrs.isSymbolicLink() ? " symlink" : " size=" + attrs.size());
                archiveCreator.addFile(name, file, attrs, modTime);
            }
            return FileVisitResult.CONTINUE;
        }

        private String entryName(Path fileOrDir) {
            String relativeName =
                    ArchiveCreator.sanitiseName(root.relativize(fileOrDir).toString());
            return prefix.isEmpty() ? relativeName : prefix + '/' + relativeName;
        }
    }

    /**
     * Build and instance of {@link ArchiveCreator}
     *
     * @param <A> The type of {@link ArchiveOutputStream} to write entries to.
     * @param <B> The type of {@link ArchiveCreatorBuilder}
     * @param <C> The type of {@link ArchiveCreator}
     */
    public abstract static class ArchiveCreatorBuilder<
            A extends ArchiveOutputStream<? extends ArchiveEntry>,
            B extends ArchiveCreatorBuilder<A, B, C>,
            C extends ArchiveCreator<A>> {
        protected final OutputStream outputStream;

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        Optional<BiPredicate<? super String, ? super Path>> entryFilter = Optional.empty();

        /**
         * Create a new {@link ArchiveCreatorBuilder} with the given output stream.
         *
         * @param outputStream the output stream
         */
        protected ArchiveCreatorBuilder(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        /**
         * Filtering entries being added to the archive.
         *
         * @param predicate the BiPredicate to filter entries to be added to the archive. The first parameter is the
         *     entry name and the second is the {@code Path} to the file on disk, which might be {@code null} when it is
         *     applied to an entry not present on a disk, i.e. via {@link #addFile(String, byte[])}.
         * @return the instance of the {@link ArchiveCreatorBuilder}
         */
        public B filter(@Nullable BiPredicate<? super String, ? super Path> predicate) {
            this.entryFilter = Optional.ofNullable(predicate);
            return getThis();
        }

        /**
         * get the current instance of the object
         *
         * @return current instance
         */
        protected abstract B getThis();

        /**
         * Start a new archive. Entries can be included in the archive using the putEntry method, and then the archive
         * should be closed using its close method. In addition, options can be applied to the underlying stream. E.g.
         * archiving level.
         *
         * <ol>
         *   <li>Use {@link #outputStream} as underlying output stream to which to write the archive.
         * </ol>
         *
         * @return new archive object for use in putEntry
         * @throws IOException thrown by the underlying output stream for I/O errors
         */
        public abstract A buildArchiveOutputStream() throws IOException;

        /**
         * Use this method to build an instance of the {@link ArchiveCreator}, use
         * {@link ArchiveCreator#ArchiveCreator(ArchiveCreatorBuilder)} to pass in instance of this builder
         *
         * @return an instance of the {@link ArchiveCreator}
         * @throws IOException thrown by the underlying output stream for I/O errors
         */
        public abstract C build() throws IOException;
    }
}
