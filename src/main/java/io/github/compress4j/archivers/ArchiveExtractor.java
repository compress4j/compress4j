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
package io.github.compress4j.archivers;

import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.RETRY;
import static io.github.compress4j.utils.FileUtils.DOS_HIDDEN;
import static io.github.compress4j.utils.FileUtils.DOS_READ_ONLY;
import static io.github.compress4j.utils.PosixFilePermissionsMapper.fromUnixMode;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import io.github.compress4j.archivers.ExtractionErrorPolicy.EntryOutcome;
import io.github.compress4j.exceptions.ArchiveLimitExceededException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is the superclass of all classes providing archive extraction functionality. This class provides
 * the core functionality to extract files and directories from archives.
 *
 * @param <A> The type of {@link ArchiveInputStream} to read entries from.
 * @since 2.2
 */
public abstract class ArchiveExtractor<A extends ArchiveInputStream<? extends ArchiveEntry>>
        implements Closeable, Iterable<ArchiveExtractor.Entry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveExtractor.class);

    /**
     * Value disabling an extraction limit.
     *
     * @since 3.1
     */
    public static final long UNLIMITED = -1L;

    private static final Predicate<Entry> ACCEPT_ALL = entry -> true;

    /** Archive input stream to be used for extraction. */
    protected A archiveInputStream;
    /** Escaping symlink policy for the extractor. */
    protected ArchiveExtractor.EscapingSymlinkPolicy escapingSymlinkPolicy = EscapingSymlinkPolicy.ALLOW;
    /** Filter for the extractor. */
    private Predicate<Entry> entryFilter = ACCEPT_ALL;
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
     * Extraction limits, see {@link #setMaxEntries(long)}, {@link #setMaxEntrySize(long)} and
     * {@link #setMaxTotalSize(long)}.
     */
    private ExtractionLimits limits = ExtractionLimits.NONE;

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
        this.limits = new ExtractionLimits(builder.maxEntries, builder.maxEntrySize, builder.maxTotalSize);
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
     * Extracts the archive to the specified directory.
     *
     * @param outputDir the directory to extract the archive to
     * @throws IOException if an I/O error occurs
     * @throws ArchiveLimitExceededException if the archive breaches one of the configured extraction limits
     */
    public final void extract(Path outputDir) throws IOException {
        ExtractionBudget budget = new ExtractionBudget(limits);
        boolean ignoreErrors = false;
        Optional<Entry> next;
        while ((next = nextEntry()).isPresent()) {
            Entry entry = next.orElseThrow();
            if (!entryFilter.test(entry)) {
                continue;
            }
            budget.countEntry();
            switch (extractEntry(outputDir, entry, ignoreErrors, budget)) {
                case EntryOutcome.Abort() -> {
                    return;
                }
                case EntryOutcome.IgnoreFurtherErrors() -> ignoreErrors = true;
                case EntryOutcome.Continue() -> {
                    /* no-op */
                }
            }
        }
    }

    /**
     * Extracts a single entry, retrying for as long as the error handler asks for it.
     *
     * @param outputDir the directory to extract the archive to
     * @param entry the entry to extract
     * @param ignoreErrors whether {@link ErrorHandlerChoice#SKIP_ALL} was selected for an earlier entry
     * @param budget the budget of the current extraction
     * @return what the extraction loop does next
     * @throws IOException if an I/O error occurs and the error handler rethrows it
     * @throws ArchiveLimitExceededException if the entry breaches one of the configured extraction limits
     */
    private EntryOutcome extractEntry(Path outputDir, Entry entry, boolean ignoreErrors, ExtractionBudget budget)
            throws IOException {
        while (true) {
            try {
                processEntry(outputDir, entry, budget);
                return new EntryOutcome.Continue();
            } catch (ArchiveLimitExceededException unrecoverableLimitBreach) {
                throw unrecoverableLimitBreach;
            } catch (IOException ioException) {
                ErrorHandlerChoice choice =
                        new ExtractionErrorPolicy(errorHandler).handle(ioException, ignoreErrors, entry);
                if (choice != RETRY) {
                    return ExtractionErrorPolicy.outcomeOf(choice);
                }
            }
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
        this.entryFilter = filter != null ? filter : ACCEPT_ALL;
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
     * Sets the maximum number of entries {@link #extract(Path)} will process before aborting.
     *
     * @param maxEntries the maximum number of entries, or {@link #UNLIMITED} to disable the limit
     * @since 3.1
     */
    public void setMaxEntries(long maxEntries) {
        this.limits = limits.withMaxEntries(maxEntries);
    }

    /**
     * Sets the maximum number of bytes a single entry may expand to before {@link #extract(Path)} aborts.
     *
     * @param maxEntrySize the maximum size of a single entry in bytes, or {@link #UNLIMITED} to disable the limit
     * @since 3.1
     */
    public void setMaxEntrySize(long maxEntrySize) {
        this.limits = limits.withMaxEntrySize(maxEntrySize);
    }

    /**
     * Sets the maximum number of bytes the whole archive may expand to before {@link #extract(Path)} aborts.
     *
     * @param maxTotalSize the maximum total extracted size in bytes, or {@link #UNLIMITED} to disable the limit
     * @since 3.1
     */
    public void setMaxTotalSize(long maxTotalSize) {
        this.limits = limits.withMaxTotalSize(maxTotalSize);
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
        /* no-op */
    }

    /**
     * Retrieve the next entry from the archive.
     *
     * @return the next entry from the archive, or empty if there are no more entries
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    public abstract Optional<Entry> nextEntry() throws IOException;

    /**
     * Open the stream for the current entry. This method is called before the entry is processed and should open the
     * stream for the current entry.
     *
     * @param entry the entry to open the stream for
     * @return the InputStream for the current entry
     * @throws IOException if an I/O error occurs
     * @since 3.0
     */
    public abstract InputStream openEntryStream(Entry entry) throws IOException;

    /**
     * Creates a stream of entries from the archive. This allows functional-style operations on archive entries.
     *
     * <p>Example:
     *
     * <pre>{@code
     * extractor.stream()
     *         .filter(e -> e.name().endsWith(".txt"))
     *         .forEach(e -> System.out.println(e.name()));
     * }</pre>
     *
     * @return a stream of entries
     * @since 3.0
     */
    public Stream<Entry> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }

    /**
     * Returns an iterator over the entries in the archive. This allows traditional iteration patterns.
     *
     * <p>Example:
     *
     * <pre>{@code
     * for (Entry entry : extractor) {
     *     System.out.println(entry.name());
     * }
     * }</pre>
     *
     * @return an iterator over archive entries
     * @since 3.0
     */
    @Override
    public @Nonnull Iterator<Entry> iterator() {
        return new Iterator<>() {
            Optional<Entry> next = Optional.empty();
            boolean nextFetched = false;

            @Override
            public boolean hasNext() {
                if (!nextFetched) {
                    try {
                        next = nextEntry();
                        nextFetched = true;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                return next.isPresent();
            }

            @Override
            public Entry next() {
                if (!hasNext()) throw new NoSuchElementException();
                nextFetched = false;
                return next.orElseThrow();
            }
        };
    }

    private Optional<Entry> stripComponents(Entry e) {
        return EntryPaths.stripComponents(e.name(), stripComponents)
                .map(newName -> new Entry(newName, e.type(), e.mode(), e.linkTarget()));
    }

    /**
     * Writes the entry to the output file.
     *
     * @param entry the entry to write
     * @param outputFile the file to write the entry to
     * @param budget the budget of the current extraction
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("try")
    private void writeFile(Entry entry, Path outputFile, ExtractionBudget budget) throws IOException {
        if (outputFile == null) {
            LOGGER.warn("Output file is null for entry: {}. Skipping.", entry.name);
            return;
        }
        if (overwrite || !Files.exists(outputFile)) {
            InputStream inputStream = openEntryStream(entry);
            try (Closeable release = () -> closeEntryStream(inputStream)) {
                EntryPaths.makeDirectory(outputFile.getParent());
                try (OutputStream outputStream = Files.newOutputStream(outputFile)) {
                    budget.transfer(entry.name(), inputStream, outputStream);
                }
                if (entry.mode != 0) {
                    setAttributes(entry.mode, outputFile);
                }
            }
        } else {
            LOGGER.debug("Skipping file entry: {} (already exists)", entry.name);
        }
    }

    /**
     * Reads exactly {@code declaredSize} bytes from {@code in}, enforcing {@link #setMaxEntrySize(long)} first. Lets a
     * subclass safely read an entry's content into memory (e.g. a symlink target that has no dedicated header field)
     * without letting a crafted archive force an oversized allocation via its own declared size.
     *
     * @param entryName the name of the entry being read, used in the exception message
     * @param in the stream to read from
     * @param declaredSize the number of bytes to read, as declared by the archive entry
     * @return the bytes read
     * @throws IOException if an I/O error occurs
     * @throws ArchiveLimitExceededException if declaredSize exceeds the configured maximum entry size
     */
    protected byte[] readEntryContent(String entryName, InputStream in, long declaredSize) throws IOException {
        limits.checkDeclaredSize(entryName, declaredSize);
        return IOUtils.toByteArray(in, declaredSize);
    }

    /**
     * Processes the entry by creating the output file and setting the attributes.
     *
     * @param outputDir the directory to extract the archive to
     * @param entry the entry to process
     * @param budget the budget of the current extraction
     * @throws IOException if an I/O error occurs
     * @throws ArchiveLimitExceededException if the archive breaches one of the configured extraction limits
     */
    private void processEntry(Path outputDir, Entry entry, ExtractionBudget budget) throws IOException {
        if (stripComponents > 0) {
            Optional<Entry> stripped = stripComponents(entry);
            if (stripped.isEmpty()) return;
            entry = stripped.orElseThrow();
        }

        Path outputFile = EntryPaths.entryFile(outputDir, entry.name);
        switch (entry.type) {
            case DIR -> {
                EntryPaths.makeDirectory(outputFile);
                if (entry.mode != 0) {
                    setAttributes(entry.mode, outputFile);
                }
            }
            case FILE -> writeFile(entry, outputFile, budget);
            case SYMLINK ->
                new SymlinkExtractor(escapingSymlinkPolicy, overwrite).extract(outputDir, entry, outputFile);
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

        /** Check during extraction and throw exception. */
        DISALLOW,

        /**
         * Make absolute symbolic links relative from the extraction directory. For example, when archive contains link
         * to {@code /opt/foo} and archive is extracted to {@code /foo/bar} then the resulting link will be
         * {@code /foo/bar/opt/foo}
         */
        RELATIVIZE_ABSOLUTE
    }

    /** Specifies the action to be taken by the {@link ArchiveExtractor#setErrorHandler error handler}. */
    public enum ErrorHandlerChoice {
        /**
         * Stop the extraction and return normally. Entries extracted before the failure are left in place; the
         * extractor never deletes anything it has already written.
         */
        ABORT,

        /** Do not handle error, just rethrow the exception */
        BAIL_OUT,

        /** Retry failed entry extraction */
        RETRY,

        /** Skip this entry from extraction */
        SKIP,

        /**
         * Skip this entry and keep extracting the remaining ones, ignoring any further {@link IOException} without
         * consulting the error handler again.
         */
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

        Predicate<Entry> entryFilter = ACCEPT_ALL;

        BiFunction<Entry, ? super IOException, ErrorHandlerChoice> errorHandlerFunction =
                (x, y) -> ErrorHandlerChoice.BAIL_OUT;
        BiConsumer<Entry, ? super Path> postProcessor;
        int stripComponents = 0;
        boolean overwrite = false;
        long maxEntries = UNLIMITED;
        long maxEntrySize = UNLIMITED;
        long maxTotalSize = UNLIMITED;

        /**
         * Default constructor for ArchiveExtractor.
         *
         * <p><b>Warning:</b> Use of this constructor does not provide a comment or initialize required fields. It is
         * recommended to use the builder or parameterized constructors instead.
         */
        protected ArchiveExtractorBuilder() {
            /* no-op */
        }

        /**
         * Sets predicate to be used when entries are being extracted
         *
         * @param entryPredicate the Predicate to filter entries to be extract from the archive.
         * @return the instance of the {@link ArchiveExtractor.ArchiveExtractorBuilder}
         */
        public B filter(@Nullable Predicate<Entry> entryPredicate) {
            this.entryFilter = entryPredicate != null ? entryPredicate : ACCEPT_ALL;
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
         * Sets the maximum number of entries the extractor will process before aborting.
         *
         * @param maxEntries the maximum number of entries, or {@link ArchiveExtractor#UNLIMITED} to disable the limit
         * @return the instance of the {@link ArchiveExtractor.ArchiveExtractorBuilder}
         * @since 3.1
         */
        public B maxEntries(long maxEntries) {
            this.maxEntries = maxEntries;
            return getThis();
        }

        /**
         * Sets the maximum number of bytes a single entry may expand to before the extractor aborts.
         *
         * @param maxEntrySize the maximum size of a single entry in bytes, or {@link ArchiveExtractor#UNLIMITED} to
         *     disable the limit
         * @return the instance of the {@link ArchiveExtractor.ArchiveExtractorBuilder}
         * @since 3.1
         */
        public B maxEntrySize(long maxEntrySize) {
            this.maxEntrySize = maxEntrySize;
            return getThis();
        }

        /**
         * Sets the maximum number of bytes the whole archive may expand to before the extractor aborts.
         *
         * @param maxTotalSize the maximum total extracted size in bytes, or {@link ArchiveExtractor#UNLIMITED} to
         *     disable the limit
         * @return the instance of the {@link ArchiveExtractor.ArchiveExtractorBuilder}
         * @since 3.1
         */
        public B maxTotalSize(long maxTotalSize) {
            this.maxTotalSize = maxTotalSize;
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
