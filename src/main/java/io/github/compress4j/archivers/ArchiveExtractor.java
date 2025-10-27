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

import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.ABORT;
import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.RETRY;
import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.SKIP;
import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.SKIP_ALL;
import static io.github.compress4j.utils.FileUtils.DOS_HIDDEN;
import static io.github.compress4j.utils.FileUtils.DOS_READ_ONLY;
import static io.github.compress4j.utils.FileUtils.checkValidPath;
import static io.github.compress4j.utils.PosixFilePermissionsMapper.fromUnixMode;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import io.github.compress4j.utils.StringUtil;
import jakarta.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is the superclass of all classes providing archive extraction functionality. This class provides
 * the core functionality to extract files and directories from archives.
 *
 * @param <A> The type of {@link ArchiveInputStream} to read entries from.
 * @since 2.2
 */
public abstract class ArchiveExtractor<A extends ArchiveInputStream<? extends ArchiveEntry>> implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveExtractor.class);

    /** Archive input stream to be used for extraction. */
    protected A archiveInputStream;
    /** Escaping symlink policy for the extractor. */
    protected ArchiveExtractor.EscapingSymlinkPolicy escapingSymlinkPolicy = EscapingSymlinkPolicy.ALLOW;
    /** Filter for the extractor. */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<Predicate<Entry>> entryFilter = Optional.empty();
    /** Error handler for the extractor. */
    private BiFunction<Entry, ? super IOException, ErrorHandlerChoice> errorHandler =
            (x, y) -> ErrorHandlerChoice.BAIL_OUT;
    /** Post processor for the extractor. */
    private BiConsumer<Entry, ? super Path> postProcessor;

    /** Number of leading path components to strip from the extracted entries. */
    private int stripComponents = 0;

    /** Whether to overwrite existing files. */
    private boolean overwrite = false;

    /**
     * Creates a new {@code ArchiveExtractor}.
     *
     * @param builder - the archive input stream builder
     * @param <B> - the type of the {@code ArchiveExtractorBuilder} to build from
     * @param <C> The type of the {@link ArchiveExtractor} to instantiate.
     * @throws IOException - if the {@code A} could not be created
     */
    protected <B extends ArchiveExtractorBuilder<A, B, C>, C extends ArchiveExtractor<A>> ArchiveExtractor(B builder)
            throws IOException {
        this.archiveInputStream = builder.buildArchiveInputStream();
        this.entryFilter = builder.entryFilter;
        this.errorHandler = builder.errorHandlerFunction;
        this.postProcessor = builder.postProcessor;
        this.stripComponents = builder.stripComponents;
        this.overwrite = builder.overwrite;
        this.escapingSymlinkPolicy = builder.escapingSymlinkPolicy;
    }

    /**
     * Creates a new {@code ArchiveExtractor}.
     *
     * @param archiveInputStream - the {@code A} to the compressed file
     */
    protected ArchiveExtractor(A archiveInputStream) {
        this.archiveInputStream = archiveInputStream;
    }

    /**
     * Validates entry and returns the path using the output directory. This method protects against path traversal
     * vulnerabilities.
     *
     * @param outputDir the directory to extract the archive to
     * @param entryName the name of the entry
     * @return the path to the extracted entry
     * @throws IOException if an I/O error occurs or a path traversal vulnerability is detected
     */
    private static Path entryFile(Path outputDir, String entryName) throws IOException {
        Path destinationFile = outputDir.resolve(StringUtil.trimLeading(entryName, '/'));
        checkValidPath(destinationFile, outputDir);
        return destinationFile;
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
    @SuppressWarnings("removal")
    private static void makeDirectory(Path path) {
        //noinspection ResultOfMethodCallIgnored
        path.toFile().mkdirs();
    }

    private static List<String> splitPath(String canonicalPath) {
        return Arrays.asList(StringUtil.trimLeading(canonicalPath, '/').split("/"));
    }

    /**
     * Sets the attributes of the output file.
     *
     * @param mode the mode to set
     * @param outputFile the file to set the attributes of
     * @throws IOException if an I/O error occurs
     */
    protected static void setAttributes(int mode, Path outputFile) throws IOException {
        if (isIsOsWindows()) {
            DosFileAttributeView attrs = Files.getFileAttributeView(outputFile, DosFileAttributeView.class);
            if (attrs != null) {
                if ((mode & DOS_READ_ONLY) != 0) attrs.setReadOnly(true);
                if ((mode & DOS_HIDDEN) != 0) attrs.setHidden(true);
            } else {
                LOGGER.trace("Cannot set DOS attributes for file: {}", outputFile);
            }
        } else {
            PosixFileAttributeView attrs = Files.getFileAttributeView(outputFile, PosixFileAttributeView.class);
            if (attrs != null) {
                attrs.setPermissions(fromUnixMode(mode));
            } else {
                LOGGER.trace("Cannot set POSIX attributes for file: {}", outputFile);
            }
        }
    }

    /**
     * Check if the OS is Windows.
     *
     * @return {@code true} if the OS is Windows, {@code false} otherwise
     */
    public static boolean isIsOsWindows() {
        return IS_OS_WINDOWS;
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
        Path outputTarget = Paths.get(linkTarget);
        if (outputTarget.isAbsolute()) {
            throw new IOException("Invalid symlink (absolute path): " + entryName + " -> " + linkTarget);
        }

        Path linkTargetPath = outputFile.getParent().resolve(outputTarget);

        try {
            checkValidPath(linkTargetPath, outputDir);
        } catch (IOException e) {
            throw new IOException(
                    "Invalid symlink (points outside of output directory): " + entryName + " -> " + linkTarget, e);
        }
    }

    /**
     * Extracts the archive to the specified directory.
     *
     * @param outputDir the directory to extract the archive to
     * @throws IOException if an I/O error occurs
     */
    public final void extract(Path outputDir) throws IOException {
        var errorHandlerChoice = SKIP;
        Entry entry;
        while ((entry = nextEntry()) != null) {
            // Skip entry if filter does not match
            if (!entryFilter.orElse(e -> true).test(entry)) {
                continue;
            }
            boolean retry;
            do {
                retry = false;
                try {
                    processEntry(outputDir, entry);
                } catch (IOException ioException) {
                    // Only consult errorHandlerChoice on exception
                    errorHandlerChoice = handleException(ioException, errorHandlerChoice, entry);
                    if (errorHandlerChoice.equals(ABORT)) {
                        return;
                    } else if (errorHandlerChoice.equals(RETRY)) {
                        retry = true;
                    } else if (errorHandlerChoice.equals(SKIP_ALL)) {
                        // Skip all remaining entries
                        return;
                    } // SKIP just skips this entry
                }
            } while (retry);
        }
    }

    /**
     * Handles an {@link IOException} that occurred during extraction.
     *
     * @param ioException the exception that occurred
     * @param entry the entry that caused the exception
     * @return ErrorHandlerChoice - the decision on how to handle the exception
     * @throws IOException if an I/O error occurs
     */
    private ErrorHandlerChoice handleException(IOException ioException, ErrorHandlerChoice decision, Entry entry)
            throws IOException {
        if (decision.equals(SKIP_ALL)) {
            LOGGER.debug("Skipped exception because {} was selected earlier", SKIP_ALL, ioException);
            return SKIP_ALL;
        } else {
            return switch (errorHandler.apply(entry, ioException)) {
                case ABORT -> ABORT;
                case BAIL_OUT -> throw ioException;
                case RETRY -> {
                    LOGGER.debug("Retying because of exception", ioException);
                    yield RETRY;
                }
                case SKIP -> {
                    LOGGER.debug("Skipped exception", ioException);
                    yield SKIP;
                }
                case SKIP_ALL -> {
                    LOGGER.debug("SKIP_ALL is selected", ioException);
                    yield SKIP_ALL;
                }
            };
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        archiveInputStream.close();
    }

    /**
     * Sets the error handler for the extractor.
     *
     * @param errorHandler the error handler to set
     */
    public void setErrorHandler(BiFunction<Entry, ? super IOException, ErrorHandlerChoice> errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Sets the escaping symlink policy for the extractor.
     *
     * @param escapingSymlinkPolicy the escaping symlink policy to set
     */
    public void setEscapingSymlinkPolicy(ArchiveExtractor.EscapingSymlinkPolicy escapingSymlinkPolicy) {
        this.escapingSymlinkPolicy = escapingSymlinkPolicy;
    }

    /**
     * Sets the filter for the extractor.
     *
     * @param filter Predicate to be used when entries are being extracted
     */
    public void setEntryFilter(@Nullable Predicate<Entry> filter) {
        this.entryFilter = Optional.ofNullable(filter);
    }

    /**
     * Sets the post processor for the extractor.
     *
     * @param consumer the post processor to set
     */
    public void setPostProcessor(@Nullable Consumer<? super Path> consumer) {
        this.postProcessor = consumer != null ? (entry, path) -> consumer.accept(path) : null;
    }

    /**
     * Sets the post processor for the extractor.
     *
     * @param postProcessor the post processor to set
     */
    public void setPostProcessor(BiConsumer<Entry, ? super Path> postProcessor) {
        this.postProcessor = postProcessor;
    }

    /**
     * Sets the number of leading path components to strip from the extracted entries.
     *
     * @param stripComponents the number of leading path components to strip
     */
    public void setStripComponents(int stripComponents) {
        this.stripComponents = stripComponents;
    }

    /**
     * Sets whether to overwrite existing files.
     *
     * @param overwrite whether to overwrite existing files
     */
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Close the stream for the current entry. This method is called after the entry has been processed and should close
     * stream opened by {@link #openEntryStream(Entry)}.
     *
     * @param stream the InputStream for the current entry
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("RedundantThrows")
    protected void closeEntryStream(@SuppressWarnings("unused") InputStream stream) throws IOException {
        // Default implementation does nothing
    }

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

    private @Nullable Entry stripComponents(Entry e) {
        List<String> ourPathSplit = splitPath(e.name);
        if (ourPathSplit.size() <= stripComponents) {
            return null;
        }
        String newName = String.join("/", ourPathSplit.subList(stripComponents, ourPathSplit.size()));
        return new Entry(newName, e.type, e.mode, e.linkTarget);
    }

    /**
     * Writes the entry to the output file.
     *
     * @param entry the entry to write
     * @param outputFile the file to write the entry to
     * @throws IOException if an I/O error occurs
     */
    private void writeFile(Entry entry, Path outputFile) throws IOException {
        if (outputFile == null) {
            LOGGER.warn("Output file is null for entry: {}. Skipping.", entry.name);
            return;
        }
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
        } else {
            LOGGER.debug("Skipping file entry: {} (already exists)", entry.name);
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
        if (entry.linkTarget == null || StringUtils.isBlank(entry.linkTarget)) {
            throw new IOException("Invalid symlink entry: " + entry.name + " (empty target)");
        }

        String target = entry.linkTarget;

        switch (escapingSymlinkPolicy) {
            case DISALLOW -> verifySymlinkTarget(entry.name, entry.linkTarget, outputDir, outputFile);
            case RELATIVIZE_ABSOLUTE -> {
                if (Paths.get(target).isAbsolute()) {
                    target = Paths.get(outputDir.toString(), entry.linkTarget.substring(1))
                            .toString();
                }
            }
            case ALLOW -> LOGGER.debug("Skipping symlink entry: {} (already exists)", entry.name);
        }

        if (overwrite || !Files.exists(outputFile, LinkOption.NOFOLLOW_LINKS)) {
            Path outputTarget = Paths.get(target);
            makeDirectory(outputFile.getParent());
            Files.deleteIfExists(outputFile);
            Files.createSymbolicLink(outputFile, outputTarget);
        } else {
            LOGGER.debug("Skipping symlink entry: {} -> {} (already exists)", entry.name, target);
        }
    }

    /**
     * Processes the entry by creating the output file and setting the attributes.
     *
     * @param outputDir the directory to extract the archive to
     * @param entry the entry to process
     * @throws IOException if an I/O error occurs
     */
    private void processEntry(Path outputDir, Entry entry) throws IOException {
        if (stripComponents > 0) {
            entry = stripComponents(entry);
            if (entry == null) return;
        }

        Path outputFile = entryFile(outputDir, entry.name);
        switch (entry.type) {
            case DIR -> makeDirectory(outputFile);
            case FILE -> writeFile(entry, outputFile);
            case SYMLINK -> extractSymlink(outputDir, entry, outputFile);
        }

        if (postProcessor != null) {
            postProcessor.accept(entry, outputFile);
        }
    }

    /**
     * Policy for handling symbolic links which point to outside of archive.
     *
     * <p>This is needed to prevent directory traversal attacks when extracting archives from untrusted sources.
     *
     * <p>For example, if an archive contains a symlink {@code foo -> /opt/foo} and the archive is extracted to
     * {@code /foo/bar}, then the symlink should not point to {@code /opt/foo} but rather to {@code /foo/bar/opt/foo}.
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

        /** Check during extraction and throw exception. See {@link ArchiveExtractor#verifySymlinkTarget} */
        DISALLOW,

        /**
         * Make absolute symbolic links relative from the extraction directory. For example, when archive contains link
         * to {@code /opt/foo} and archive is extracted to {@code /foo/bar} then the resulting link will be
         * {@code /foo/bar/opt/foo}
         */
        RELATIVIZE_ABSOLUTE
    }

    /** Specifies action to be taken from the {@code com.intellij.util.io.ArchiveExtractor#errorHandler} */
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
     * Builder for creating an {@link ArchiveExtractor}.
     *
     * @param <A> The type of {@link ArchiveInputStream} to read entries from.
     * @param <B> The type of the {@code ArchiveExtractorBuilder} to build from.
     * @param <C> The type of the {@link ArchiveExtractor} to instantiate.
     */
    public abstract static class ArchiveExtractorBuilder<
            A extends ArchiveInputStream<? extends ArchiveEntry>,
            B extends ArchiveExtractorBuilder<A, B, C>,
            C extends ArchiveExtractor<A>> {
        /** Input stream to read from for extraction. */
        protected ArchiveExtractor.EscapingSymlinkPolicy escapingSymlinkPolicy = EscapingSymlinkPolicy.ALLOW;

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        Optional<Predicate<Entry>> entryFilter = Optional.empty();

        BiFunction<Entry, ? super IOException, ErrorHandlerChoice> errorHandlerFunction =
                (x, y) -> ErrorHandlerChoice.BAIL_OUT;
        BiConsumer<Entry, ? super Path> postProcessor;
        int stripComponents = 0;
        boolean overwrite = false;

        /**
         * Default constructor for ArchiveExtractor.
         *
         * <p><b>Warning:</b> Use of this constructor does not provide a comment or initialize required fields. It is
         * recommended to use the builder or parameterized constructors instead.
         */
        protected ArchiveExtractorBuilder() {
            // Default constructor for subclassing or frameworks. Not recommended for direct use.
        }

        /**
         * Sets predicate to be used when entries are being extracted
         *
         * @param entryPredicate the Predicate to filter entries to be extract from the archive.
         * @return the instance of the {@link ArchiveExtractor.ArchiveExtractorBuilder}
         */
        public B filter(@Nullable Predicate<Entry> entryPredicate) {
            this.entryFilter = Optional.ofNullable(entryPredicate);
            return getThis();
        }

        /**
         * Sets the error handler for the extractor.
         *
         * @param errorHandlerFunction the error handler to set
         * @return the instance of the {@link ArchiveExtractor.ArchiveExtractorBuilder}
         */
        public B errorHandler(BiFunction<Entry, ? super IOException, ErrorHandlerChoice> errorHandlerFunction) {
            this.errorHandlerFunction = errorHandlerFunction;
            return getThis();
        }

        /**
         * Sets the escaping symlink policy for the extractor.
         *
         * @param policy the escaping symlink policy to set
         * @return the instance of the {@link ArchiveExtractor.ArchiveExtractorBuilder}
         */
        public B escapingSymlinkPolicy(ArchiveExtractor.EscapingSymlinkPolicy policy) {
            this.escapingSymlinkPolicy = policy;
            return getThis();
        }

        /**
         * Sets the post processor for the extractor.
         *
         * @param entryBiConsumer the post processor to set
         * @return the instance of the {@link ArchiveExtractor.ArchiveExtractorBuilder}
         */
        public B postProcessor(BiConsumer<Entry, ? super Path> entryBiConsumer) {
            this.postProcessor = entryBiConsumer;
            return getThis();
        }

        /**
         * Sets the number of leading path components to strip from the extracted entries.
         *
         * @param level the number of leading path components to strip
         * @return the instance of the {@link ArchiveExtractor.ArchiveExtractorBuilder}
         */
        public B stripComponents(int level) {
            this.stripComponents = level;
            return getThis();
        }

        /**
         * Sets whether to overwrite existing files.
         *
         * @param overwrite whether to overwrite existing files
         * @return the instance of the {@link ArchiveExtractor.ArchiveExtractorBuilder}
         */
        public B overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return getThis();
        }

        /**
         * get the current instance of the object
         *
         * @return current instance
         */
        protected abstract B getThis();

        /**
         * Build a {@code A} from the given {@code InputStream}. If you want to combine an archive format with a
         * compression format - like when reading a `tar.gz` file - you wrap the {@code ArchiveInputStream} around
         *
         * <pre>{@code
         * return new TarArchiveInputStream(new GzipCompressorInputStream(inputStream));
         * }</pre>
         *
         * @return a {@code A} from the given {@code InputStream}
         * @throws IOException - if the {@code A} could not be created
         */
        public abstract A buildArchiveInputStream() throws IOException;

        /**
         * Use this method to build an instance of the {@link ArchiveExtractor}, use
         * {@link ArchiveExtractor#ArchiveExtractor(ArchiveExtractor.ArchiveExtractorBuilder)} to pass in instance of
         * this builder
         *
         * @return an instance of the {@link ArchiveExtractor}
         * @throws IOException thrown by the underlying output stream for I/O errors
         */
        public abstract C build() throws IOException;
    }

    /**
     * Represents an entry in the archive.
     *
     * <p>It is recommended to use {@link #name} as a key for the entry, as it is normalized and trimmed.
     *
     * @param name the name of the entry
     * @param type the type of the entry
     * @param mode the mode of the entry
     * @param linkTarget the target of the symbolic link
     */
    public record Entry(
            String name, Type type, int mode, @Nullable String linkTarget) {
        /**
         * Creates a new entry with the specified name, type, mode, link target, and size.
         *
         * @param name the name of the entry
         * @param isDirectory whether the entry is a directory
         */
        public Entry(String name, boolean isDirectory) {
            this(name, isDirectory ? Type.DIR : Type.FILE, 0, null);
        }

        /**
         * Creates a new entry with the specified name, type, mode, link target, and size.
         *
         * @param name the name of the entry
         * @param type the type of the entry
         * @param mode the mode of the entry
         */
        public Entry(String name, Type type, int mode) {
            this(name, type, mode, null);
        }

        /** Normalizes the name of the entry by trimming whitespace and replacing backslashes with forward slashes. */
        public Entry {
            name = name.trim().replace('\\', '/');
            int s = 0;
            int e = name.length() - 1;
            while (s < e && name.charAt(s) == '/') s++;
            while (e >= s && name.charAt(e) == '/') e--;
            name = name.substring(s, e + 1);
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
}
