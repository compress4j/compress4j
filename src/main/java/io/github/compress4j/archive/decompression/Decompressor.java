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
package io.github.compress4j.archive.decompression;

import io.github.compress4j.archive.decompression.Decompressor.Builder;
import io.github.compress4j.utils.PosixFilePermissionsMapper;
import io.github.compress4j.utils.StringUtil;
import io.github.compress4j.utils.SystemUtils;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is the superclass of all classes providing de-compression. This class provides functionality to
 * add files and directories to an archive.
 *
 * @param <D> the type of the decompressor
 * @param <B> the type of the builder
 */
public abstract class Decompressor<D extends Decompressor<D, B>, B extends Builder<D, B>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Decompressor.class);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Predicate<? super Entry>> filter;

    private final BiFunction<? super Entry, ? super IOException, ErrorHandlerChoice> errorHandler;
    private final List<String> pathPrefix;
    private final EscapingSymlinkPolicy escapingSymlinkPolicy;
    private final BiConsumer<? super Entry, ? super Path> postProcessor;
    private final boolean overwrite;

    private boolean ignoreIOExceptions = false;

    /**
     * Creates a new {@code Decompressor} with the given {@code Builder}.
     *
     * @param builder the builder to create the decompressor with
     */
    protected Decompressor(Builder<D, B> builder) {
        this.filter = builder.filter;
        this.errorHandler = builder.errorHandler;
        this.pathPrefix = builder.pathPrefix;
        this.escapingSymlinkPolicy = builder.escapingSymlinkPolicy;
        this.postProcessor = builder.postProcessor;
        this.overwrite = builder.overwrite;
    }

    /**
     * Extracts the archive to the specified directory.
     *
     * @param outputDir the directory to extract the archive to
     * @throws IOException if an I/O error occurs
     */
    public final void extract(Path outputDir) throws IOException {
        openStream();
        try {
            Deque<Path> extractedPaths = new ArrayDeque<>();
            boolean retry = false;

            Entry entry = null;
            while (retry || (entry = nextEntry()) != null) {
                if (retry && !filter.orElse(e -> true).test(entry)) {
                    continue;
                }

                retry = false;
                try {
                    Path processedEntry = processEntry(outputDir, entry);
                    if (processedEntry != null) {
                        extractedPaths.push(processedEntry);
                    }
                } catch (IOException ioException) {
                    retry = handleException(ioException, entry, extractedPaths, retry);
                }
            }
        } finally {
            closeStream();
        }
    }

    /**
     * Close the stream for the current entry. This method is called after the entry has been processed and should close
     * stream opened by {@link #openEntryStream(Entry)}.
     *
     * @param stream the InputStream for the current entry
     * @throws IOException if an I/O error occurs
     */
    protected abstract void closeEntryStream(InputStream stream) throws IOException;

    /**
     * Close the stream for the archive. This method is called after all entries have been processed and should close
     * the stream opened by {@link #openStream()}.
     *
     * @throws IOException if an I/O error occurs
     */
    protected abstract void closeStream() throws IOException;

    /**
     * Retrieve the next entry from the archive.
     *
     * @return the next entry from the archive, or {@code null} if there are no more entries
     * @throws IOException if an I/O error occurs
     */
    protected abstract @Nullable Entry nextEntry() throws IOException;

    /**
     * Open the stream for the current entry. This method is called before the entry is processed and should open the
     * stream for the current entry.
     *
     * @param entry the entry to open the stream for
     * @return the InputStream for the current entry
     * @throws IOException if an I/O error occurs
     */
    protected abstract InputStream openEntryStream(Entry entry) throws IOException;

    /**
     * Open the stream for the archive. This method is called before any entries are processed and should open the
     * stream for the archive.
     *
     * @throws IOException if an I/O error occurs
     */
    protected abstract void openStream() throws IOException;

    /**
     * Validates the path to prevent directory traversal attacks.
     *
     * @param entryName the path to validate
     * @throws IOException if the path is invalid
     */
    private static void ensureValidPath(String entryName) throws IOException {
        if (entryName.contains("..")
                && Arrays.asList(entryName.split("[/\\\\]")).contains("..")) {
            throw new IOException("Invalid entry name: " + entryName);
        }
    }

    /**
     * Validates entry and returns the path using the output directory.
     *
     * @param outputDir the directory to extract the archive to
     * @param entryName the name of the entry
     * @return the path to the extracted entry
     * @throws IOException if an I/O error occurs
     */
    private static Path entryFile(Path outputDir, String entryName) throws IOException {
        ensureValidPath(entryName);
        return outputDir.resolve(StringUtil.trimLeading(entryName, '/'));
    }

    /**
     * Creates the directory for the given path, including any necessary but nonexistent parent directories. Note that
     * if this operation fails it may have succeeded in creating some of the necessary parent directories.
     *
     * @param path the directory to be created
     * @throws SecurityException If a security manager exists and its
     *     {@link java.lang.SecurityManager#checkRead(java.lang.String)} method does not permit verification of the
     *     existence of the named directory and all necessary parent directories; or if the
     *     {@link java.lang.SecurityManager#checkWrite(java.lang.String)} method does not permit the named directory and
     *     all necessary parent directories to be created
     */
    private static void makeDirectory(Path path) {
        //noinspection ResultOfMethodCallIgnored
        path.toFile().mkdirs();
    }

    /**
     * Writes the entry to the output file.
     *
     * @param entry the entry to write
     * @param outputFile the file to write the entry to
     * @throws IOException if an I/O error occurs
     */
    private void writeFile(Entry entry, Path outputFile) throws IOException {
        if (overwrite || !Files.exists(outputFile)) {
            InputStream inputStream = openEntryStream(entry);
            try {
                makeDirectory(outputFile.getParent());
                try (OutputStream outputStream = Files.newOutputStream(outputFile)) {
                    inputStream.transferTo(outputStream);
                }
                if (entry.mode != 0) {
                    setAttributes(entry.mode, outputFile);
                }
            } finally {
                closeEntryStream(inputStream);
            }
        }
    }

    /**
     * Extracts the symlink to the output file.
     *
     * @param outputDir the directory to extract the archive to
     * @param entry the entry to extract
     * @param outputFile the file to extract the entry to
     * @throws IOException if an I/O error occurs
     */
    private void extractSymlink(Path outputDir, Entry entry, Path outputFile) throws IOException {
        if (entry.linkTarget == null || entry.linkTarget.isEmpty()) {
            throw new IOException("Invalid symlink entry: " + entry.name + " (empty target)");
        }

        String target = entry.linkTarget;

        switch (escapingSymlinkPolicy) {
            case DISALLOW: {
                verifySymlinkTarget(entry.name, entry.linkTarget, outputDir, outputFile);
                break;
            }
            case RELATIVIZE_ABSOLUTE: {
                if (Paths.get(target).isAbsolute()) {
                    target = Paths.get(outputDir.toString(), entry.linkTarget.substring(1))
                            .toString();
                }
                break;
            }
            case ALLOW:
                break;
        }

        if (overwrite || !Files.exists(outputFile, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Path outputTarget = Paths.get(target);
                makeDirectory(outputFile.getParent());
                Files.deleteIfExists(outputFile);
                Files.createSymbolicLink(outputFile, outputTarget);
            } catch (InvalidPathException e) {
                throw new IOException("Invalid symlink entry: " + entry.name + " -> " + target, e);
            }
        }
    }

    /**
     * Handles an {@link IOException} that occurred during extraction.
     *
     * @param ioException the exception that occurred
     * @param entry the entry that caused the exception
     * @param extractedPaths the paths that have been extracted so far
     * @param retry whether to retry the extraction
     * @return whether to retry the extraction
     * @throws IOException if an I/O error occurs
     */
    private boolean handleException(IOException ioException, Entry entry, Deque<Path> extractedPaths, boolean retry)
            throws IOException {
        if (ignoreIOExceptions) {
            LOGGER.debug("Skipped exception because {} was selected earlier", ErrorHandlerChoice.SKIP_ALL, ioException);
        } else {
            switch (errorHandler.apply(entry, ioException)) {
                case ABORT:
                    while (!extractedPaths.isEmpty()) {
                        Files.delete(extractedPaths.pop());
                    }
                    break;
                case BAIL_OUT:
                    throw ioException;
                case RETRY:
                    retry = true;
                    break;
                case SKIP:
                    LOGGER.debug("Skipped exception", ioException);
                    break;
                case SKIP_ALL:
                    ignoreIOExceptions = true;
                    LOGGER.debug("SKIP_ALL is selected", ioException);
                    break;
            }
        }
        return retry;
    }

    /**
     * Maps the path prefix to the entry.
     *
     * @param e the entry to map
     * @param prefix the prefix to map
     * @return the mapped entry
     * @throws IOException if an I/O error occurs
     */
    private static @Nullable Entry mapPathPrefix(Entry e, List<String> prefix) throws IOException {
        List<String> ourPathSplit = normalizePathAndSplit(e.name);
        if (prefix.size() >= ourPathSplit.size()
                || !ourPathSplit.subList(0, prefix.size()).equals(prefix)) {
            return null;
        }
        String newName = String.join("/", ourPathSplit.subList(prefix.size(), ourPathSplit.size()));
        return new Entry(newName, e.type, e.mode, e.linkTarget, e.size);
    }

    private static List<String> normalizePathAndSplit(String path) throws IOException {
        ensureValidPath(path);
        String canonicalPath = Paths.get(path).toFile().getCanonicalPath();
        return Arrays.asList(StringUtil.trimLeading(canonicalPath, '/').split("/"));
    }

    /** @return Path to an extracted entity */
    private @Nullable Path processEntry(Path outputDir, Entry entry) throws IOException {
        if (!pathPrefix.isEmpty()) {
            entry = mapPathPrefix(entry, pathPrefix);
            if (entry == null) return null;
        }

        Path outputFile = entryFile(outputDir, entry.name);
        switch (entry.type) {
            case DIR:
                makeDirectory(outputFile);
                break;
            case FILE:
                writeFile(entry, outputFile);
                break;
            case SYMLINK:
                extractSymlink(outputDir, entry, outputFile);
                break;
        }

        if (postProcessor != null) {
            postProcessor.accept(entry, outputFile);
        }

        return outputFile;
    }

    /**
     * Sets the attributes of the output file.
     *
     * @param mode the mode to set
     * @param outputFile the file to set the attributes of
     * @throws IOException if an I/O error occurs
     */
    private static void setAttributes(int mode, Path outputFile) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            DosFileAttributeView attrs = Files.getFileAttributeView(outputFile, DosFileAttributeView.class);
            if (attrs != null) {
                if ((mode & Entry.DOS_READ_ONLY) != 0) attrs.setReadOnly(true);
                if ((mode & Entry.DOS_HIDDEN) != 0) attrs.setHidden(true);
            }
        } else {
            PosixFileAttributeView attrs = Files.getFileAttributeView(outputFile, PosixFileAttributeView.class);
            if (attrs != null) {
                attrs.setPermissions(PosixFilePermissionsMapper.fromUnixMode(mode));
            }
        }
    }

    /**
     * Verifies that the symlink target is valid.
     *
     * @param entryName the name of the entry
     * @param linkTarget the target of the symlink
     * @param outputDir the directory to extract the archive to
     * @param outputFile the file to extract the entry to
     * @throws IOException if the symlink target is invalid
     */
    private static void verifySymlinkTarget(String entryName, String linkTarget, Path outputDir, Path outputFile)
            throws IOException {
        try {
            Path outputTarget = Paths.get(linkTarget);
            if (outputTarget.isAbsolute()) {
                throw new IOException("Invalid symlink (absolute path): " + entryName + " -> " + linkTarget);
            }
            Path linkTargetNormalized =
                    outputFile.getParent().resolve(outputTarget).normalize();
            if (!linkTargetNormalized.startsWith(outputDir.normalize())) {
                throw new IOException(
                        "Invalid symlink (points outside of output directory): " + entryName + " -> " + linkTarget);
            }
        } catch (InvalidPathException e) {
            throw new IOException("Failed to verify symlink entry scope: " + entryName + " -> " + linkTarget, e);
        }
    }

    /**
     * Policy for handling symbolic links which point to outside of archive.
     *
     * <p>Example: {@code foo -> /opt/foo}
     *
     * <p>or {@code foo -> ../foo}
     */
    public enum EscapingSymlinkPolicy {
        /**
         * Extract as is with no modification or check. Potentially can point to a completely different object if the
         * archive is transferred from some other host.
         */
        ALLOW,

        /** Check during extraction and throw exception. See {@link Decompressor#verifySymlinkTarget} */
        DISALLOW,

        /**
         * Make absolute symbolic links relative from the extraction directory. For example, when archive contains link
         * to {@code /opt/foo} and archive is extracted to {@code /foo/bar} then the resulting link will be
         * {@code /foo/bar/opt/foo}
         */
        RELATIVIZE_ABSOLUTE
    }

    /** Specifies action to be taken from the {@code com.intellij.util.io.Decompressor#errorHandler} */
    public enum ErrorHandlerChoice {
        /** Extraction should be aborted and already extracted entities should be cleaned */
        ABORT,

        /** Do not handle error, just rethrow the exception */
        BAIL_OUT,

        /** Retry failed entry extraction */
        RETRY,

        /** Skip this entry from extraction */
        SKIP,

        /** Skip this entry for extraction and ignore any further IOExceptions during this archive extraction */
        SKIP_ALL
    }

    /**
     * Represents an entry in the archive.
     *
     * <p>It is recommended to use {@link #name} as a key for the entry, as it is normalized and trimmed.
     */
    public static final class Entry {
        /** DOS read-only attribute */
        public static final int DOS_READ_ONLY = 0b01;

        /** DOS hidden attribute */
        public static final int DOS_HIDDEN = 0b010;

        /** An entry name with separators converted to '/' and trimmed; handle with care */
        public final String name;

        /** Type of the entry */
        public final Type type;

        /** Depending on the source, could be POSIX permissions, DOS attributes, or just {@code 0} */
        public final int mode;

        /** Size of the entry */
        public final long size;

        /** Target of the symbolic link, or {@code null} if not a symbolic link */
        public final @Nullable String linkTarget;

        /**
         * Creates a new entry with the specified name, type, mode, link target, and size.
         *
         * @param name the name of the entry
         * @param isDirectory whether the entry is a directory
         * @param size the size of the entry
         */
        Entry(String name, boolean isDirectory, long size) {
            this(name, isDirectory ? Type.DIR : Type.FILE, 0, null, size);
        }

        /**
         * Creates a new entry with the specified name, type, mode, link target, and size.
         *
         * @param name the name of the entry
         * @param type the type of the entry
         * @param mode the mode of the entry
         * @param linkTarget the target of the symbolic link
         * @param size the size of the entry
         */
        Entry(String name, Type type, int mode, @Nullable String linkTarget, long size) {
            name = name.trim().replace('\\', '/');
            int s = 0;
            int e = name.length() - 1;
            while (s < e && name.charAt(s) == '/') s++;
            while (e >= s && name.charAt(e) == '/') e--;
            this.name = name.substring(s, e + 1);
            this.type = type;
            this.mode = mode;
            this.linkTarget = linkTarget;
            this.size = size;
        }

        /** Type of the entry. */
        public enum Type {
            /** File */
            FILE,
            /** Directory */
            DIR,
            /** Symbolic link */
            SYMLINK
        }
    }

    /**
     * Builder for {@link Decompressor}.
     *
     * @param <T> the type of the decompressor
     * @param <O> the type of the builder
     */
    @SuppressWarnings("unused")
    protected abstract static class Builder<T extends Decompressor<T, O>, O extends Builder<T, O>> {
        /** Error handler for the decompressor. */
        protected BiFunction<? super Entry, ? super IOException, ErrorHandlerChoice> errorHandler =
                (x, y) -> ErrorHandlerChoice.BAIL_OUT;

        /** Filter for the decompressor. */
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        protected Optional<Predicate<? super Decompressor.Entry>> filter;

        /** Path prefix for the decompressor. */
        protected List<String> pathPrefix = List.of();

        /** Escaping symlink policy for the decompressor. */
        protected Decompressor.EscapingSymlinkPolicy escapingSymlinkPolicy = EscapingSymlinkPolicy.ALLOW;

        /** Post processor for the decompressor. */
        protected BiConsumer<? super Decompressor.Entry, ? super Path> postProcessor;

        /** Whether to overwrite existing files. */
        protected boolean overwrite = true;

        /** Creates a new builder. */
        protected Builder() {}

        /**
         * Builds the MessageRequest.
         *
         * @return A MessageRequest, populated with all fields from this builder.
         */
        public abstract T build();

        /**
         * Sets the error handler for the decompressor.
         *
         * @param errorHandler the error handler to set
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public O errorHandler(BiFunction<? super Entry, ? super IOException, ErrorHandlerChoice> errorHandler) {
            this.errorHandler = errorHandler;
            return (O) this;
        }

        /**
         * Sets the escaping symlink policy for the decompressor.
         *
         * @param myEscapingSymlinkPolicy the escaping symlink policy to set
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public O escapingSymlinkPolicy(Decompressor.EscapingSymlinkPolicy myEscapingSymlinkPolicy) {
            this.escapingSymlinkPolicy = myEscapingSymlinkPolicy;
            return (O) this;
        }

        /**
         * Sets the filter for the decompressor.
         *
         * @param myFilter the filter to set
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public O filter(Predicate<? super Decompressor.Entry> myFilter) {
            this.filter = Optional.ofNullable(myFilter);
            return (O) this;
        }

        /**
         * Sets whether to overwrite existing files.
         *
         * @param overwrite whether to overwrite existing files
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public O overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return (O) this;
        }

        /**
         * Sets the path prefix for the decompressor.
         *
         * @param myPathPrefix the path prefix to set
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public O pathPrefix(List<String> myPathPrefix) {
            this.pathPrefix = myPathPrefix;
            return (O) this;
        }

        /**
         * Sets the post processor for the decompressor.
         *
         * @param consumer the post processor to set
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public O postProcessor(@Nullable Consumer<? super Path> consumer) {
            this.postProcessor = consumer != null ? (entry, path) -> consumer.accept(path) : null;
            return (O) this;
        }

        /**
         * Sets the post processor for the decompressor.
         *
         * @param postProcessor the post processor to set
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public O postProcessor(BiConsumer<? super Decompressor.Entry, ? super Path> postProcessor) {
            this.postProcessor = postProcessor;
            return (O) this;
        }

        /**
         * Extracts only items whose path starts with the normalized prefix of {@code prefix + '/'}. Paths are
         * normalized before comparison. The prefix test is applied after {@link #filter} predicate is tested. Some
         * entries may clash, so use {@link #overwrite} to control it. Some items with a path that does not start from
         * the prefix could be ignored.
         *
         * @param prefix a prefix to remove from every archive entry path
         * @return self
         * @throws IOException if the prefix is invalid
         */
        @SuppressWarnings("unchecked")
        public O removePrefixPath(@Nullable String prefix) throws IOException {
            pathPrefix = prefix != null ? normalizePathAndSplit(prefix) : List.of();
            return (O) this;
        }
    }
}
