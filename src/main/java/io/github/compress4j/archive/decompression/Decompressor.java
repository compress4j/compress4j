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
package io.github.compress4j.archive.decompression;

import io.github.compress4j.utils.PosixFilePermissionsMapper;
import io.github.compress4j.utils.StringUtil;
import io.github.compress4j.utils.SystemUtils;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is the superclass of all classes providing de-compression. This class provides functionality to
 * add files and directories to an archive.
 */
@SuppressWarnings("unused")
public abstract class Decompressor<D extends Decompressor<D, B>, B extends Decompressor.Builder<D, B>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Decompressor.class);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Predicate<? super Entry>> filter;

    private final BiFunction<? super Entry, ? super IOException, ErrorHandlerChoice> errorHandler;
    private final List<String> pathPrefix;
    private final EscapingSymlinkPolicy escapingSymlinkPolicy;
    private final BiConsumer<? super Entry, ? super Path> postProcessor;
    private final boolean overwrite;

    private boolean ignoreIOExceptions = false;

    protected Decompressor(Builder<D, B> builder) {
        this.filter = builder.filter;
        this.errorHandler = builder.errorHandler;
        this.pathPrefix = builder.pathPrefix;
        this.escapingSymlinkPolicy = builder.escapingSymlinkPolicy;
        this.postProcessor = builder.postProcessor;
        this.overwrite = builder.overwrite;
    }

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

    protected abstract void openStream() throws IOException;

    protected abstract @Nullable Entry nextEntry() throws IOException;

    /** @return Path to an extracted entity */
    private @Nullable Path processEntry(Path outputDir, Entry entry) throws IOException {
        if (!pathPrefix.isEmpty()) {
            entry = mapPathPrefix(entry, pathPrefix);
            if (entry == null) return null;
        }

        Path outputFile = entryFile(outputDir, entry.name);
        switch (entry.type) {
            case DIR -> extractDirectory(outputFile);
            case FILE -> extractFile(entry, outputFile);
            case SYMLINK -> extractSymlink(outputDir, entry, outputFile);
        }

        if (postProcessor != null) {
            postProcessor.accept(entry, outputFile);
        }

        return outputFile;
    }

    private boolean handleException(IOException ioException, Entry entry, Deque<Path> extractedPaths, boolean retry)
            throws IOException {
        if (ignoreIOExceptions) {
            LOGGER.debug("Skipped exception because {} was selected earlier", ErrorHandlerChoice.SKIP_ALL, ioException);
        } else {
            switch (errorHandler.apply(entry, ioException)) {
                case ABORT -> {
                    while (!extractedPaths.isEmpty()) {
                        Files.delete(extractedPaths.pop());
                    }
                }
                case BAIL_OUT -> throw ioException;
                case RETRY -> retry = true;
                case SKIP -> LOGGER.debug("Skipped exception", ioException);
                case SKIP_ALL -> {
                    ignoreIOExceptions = true;
                    LOGGER.debug("SKIP_ALL is selected", ioException);
                }
            }
        }
        return retry;
    }

    protected abstract void closeStream() throws IOException;

    private static @Nullable Entry mapPathPrefix(Entry e, List<String> prefix) throws IOException {
        List<String> ourPathSplit = normalizePathAndSplit(e.name);
        if (prefix.size() >= ourPathSplit.size()
                || !ourPathSplit.subList(0, prefix.size()).equals(prefix)) {
            return null;
        }
        String newName = String.join("/", ourPathSplit.subList(prefix.size(), ourPathSplit.size()));
        return new Entry(newName, e.type, e.mode, e.linkTarget, e.size);
    }

    public static Path entryFile(Path outputDir, String entryName) throws IOException {
        ensureValidPath(entryName);
        return outputDir.resolve(StringUtil.trimLeading(entryName, '/'));
    }

    private static void extractDirectory(Path outputFile) {
        //noinspection ResultOfMethodCallIgnored
        outputFile.toFile().mkdirs();
    }

    private void extractFile(Entry entry, Path outputFile) throws IOException {
        if (overwrite || !Files.exists(outputFile)) {
            InputStream inputStream = openEntryStream(entry);
            try {
                extractDirectory(outputFile.getParent());
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
                extractDirectory(outputFile.getParent());
                Files.deleteIfExists(outputFile);
                Files.createSymbolicLink(outputFile, outputTarget);
            } catch (InvalidPathException e) {
                throw new IOException("Invalid symlink entry: " + entry.name + " -> " + target, e);
            }
        }
    }

    private static List<String> normalizePathAndSplit(String path) throws IOException {
        ensureValidPath(path);
        String canonicalPath = Paths.get(path).toFile().getCanonicalPath();
        return Arrays.asList(StringUtil.trimLeading(canonicalPath, '/').split("/"));
    }

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

    protected abstract InputStream openEntryStream(Entry entry) throws IOException;

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

    protected abstract void closeEntryStream(InputStream stream) throws IOException;

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
    // </editor-fold>

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

    public static final class Entry {
        public static final int DOS_READ_ONLY = 0b01;
        public static final int DOS_HIDDEN = 0b010;
        /** An entry name with separators converted to '/' and trimmed; handle with care */
        public final String name;

        public final Type type;
        /** Depending on the source, could be POSIX permissions, DOS attributes, or just {@code 0} */
        public final int mode;

        public final long size;
        public final @Nullable String linkTarget;

        Entry(String name, boolean isDirectory, long size) {
            this(name, isDirectory ? Type.DIR : Type.FILE, 0, null, size);
        }

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

        public enum Type {
            FILE,
            DIR,
            SYMLINK
        }
    }

    protected abstract static class Builder<D, B> {
        protected BiFunction<? super Entry, ? super IOException, ErrorHandlerChoice> errorHandler =
                (x, y) -> ErrorHandlerChoice.BAIL_OUT;

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        protected Optional<Predicate<? super Decompressor.Entry>> filter;

        protected List<String> pathPrefix = List.of();
        protected Decompressor.EscapingSymlinkPolicy escapingSymlinkPolicy = EscapingSymlinkPolicy.ALLOW;
        protected BiConsumer<? super Decompressor.Entry, ? super Path> postProcessor;
        protected boolean overwrite = true;

        protected Builder() {}

        public B filter(Predicate<? super Decompressor.Entry> myFilter) {
            this.filter = Optional.ofNullable(myFilter);
            //noinspection unchecked
            return (B) this;
        }

        public B errorHandler(BiFunction<? super Entry, ? super IOException, ErrorHandlerChoice> errorHandler) {
            this.errorHandler = errorHandler;
            //noinspection unchecked
            return (B) this;
        }

        public B overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            //noinspection unchecked
            return (B) this;
        }

        public B pathPrefix(List<String> myPathPrefix) {
            this.pathPrefix = myPathPrefix;
            //noinspection unchecked
            return (B) this;
        }

        public B escapingSymlinkPolicy(Decompressor.EscapingSymlinkPolicy myEscapingSymlinkPolicy) {
            this.escapingSymlinkPolicy = myEscapingSymlinkPolicy;
            //noinspection unchecked
            return (B) this;
        }

        public B postProcessor(BiConsumer<? super Decompressor.Entry, ? super Path> postProcessor) {
            this.postProcessor = postProcessor;
            //noinspection unchecked
            return (B) this;
        }

        public B postProcessor(@Nullable Consumer<? super Path> consumer) {
            this.postProcessor = consumer != null ? (entry, path) -> consumer.accept(path) : null;
            //noinspection unchecked
            return (B) this;
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
        public B removePrefixPath(@Nullable String prefix) throws IOException {
            pathPrefix = prefix != null ? normalizePathAndSplit(prefix) : List.of();
            //noinspection unchecked
            return (B) this;
        }
        /**
         * Builds the MessageRequest.
         *
         * @return A MessageRequest, populated with all fields from this builder.
         */
        public abstract D build();
    }
}
