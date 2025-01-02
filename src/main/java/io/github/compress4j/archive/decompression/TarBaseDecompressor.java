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

import io.github.compress4j.utils.SystemUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Tar Base Decompressor
 *
 * @param <D> the type of the {@code TarBaseDecompressor}
 * @param <B> the type of the {@code TarBaseDecompressor.Builder}
 */
public abstract class TarBaseDecompressor<
                D extends TarBaseDecompressor<D, B>, B extends TarBaseDecompressor.Builder<D, B>>
        extends Decompressor<D, B> {
    private final InputStream sourceStream;
    private TarArchiveInputStream archiveInputStream;

    /**
     * Creates a new {@code TarBaseDecompressor} with the given {@code Builder}
     *
     * @param builder the {@code Builder} to build the {@code TarBaseDecompressor}
     */
    protected TarBaseDecompressor(Builder<D, B> builder) {
        super(builder);
        sourceStream = builder.sourceStream;
    }

    /** {@inheritDoc} */
    @Override
    protected void closeEntryStream(InputStream stream) {
        // no-op
    }

    /** {@inheritDoc} */
    @Override
    protected void closeStream() throws IOException {
        if (sourceStream instanceof Path) {
            archiveInputStream.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Entry nextEntry() throws IOException {
        TarArchiveEntry te;
        te = getNextTarArchiveEntry();
        if (te == null) return null;
        if (!SystemUtils.IS_OS_WINDOWS)
            return new Entry(te.getName(), type(te), te.getMode(), te.getLinkName(), te.getSize());
        if (te.isSymbolicLink()) return new Entry(te.getName(), Entry.Type.SYMLINK, 0, te.getLinkName(), te.getSize());
        return new Entry(te.getName(), te.isDirectory(), te.getSize());
    }

    /** {@inheritDoc} */
    @Override
    protected InputStream openEntryStream(Entry entry) {
        return archiveInputStream;
    }

    /** {@inheritDoc} */
    @Override
    protected void openStream() throws IOException {
        archiveInputStream = this.buildArchiveInputStream(sourceStream);
    }

    /**
     * Build a {@code TarArchiveInputStream} from the given {@code InputStream}. If you want to combine an archive
     * format with a compression format - like when reading a `tar.gz` file - you wrap the {@code ArchiveInputStream}
     * around {@code CompressorInputStream} for example:
     *
     * <pre>{@code
     * return new TarArchiveInputStream(new GzipCompressorInputStream(input));
     * }</pre>
     *
     * @param input - the {@code InputStream} to the tar file
     * @return a {@code TarArchiveInputStream} from the given {@code InputStream}
     * @throws IOException - if the {@code TarArchiveInputStream} could not be created
     */
    abstract TarArchiveInputStream buildArchiveInputStream(InputStream input) throws IOException;

    /**
     * Get the next {@code TarArchiveEntry} from the {@code TarArchiveInputStream}. Skip hardlinks, directories, and
     * symbolic links.
     *
     * @return the next {@code TarArchiveEntry}
     * @throws IOException – if the next entry could not be read
     */
    private TarArchiveEntry getNextTarArchiveEntry() throws IOException {
        TarArchiveEntry te;
        if ((te = archiveInputStream.getNextEntry()) != null
                && !((te.isFile() && !te.isLink()) // ignore hardlink
                        || te.isDirectory()
                        || te.isSymbolicLink())) {
            getNextTarArchiveEntry();
        }
        return te;
    }

    private static Entry.Type type(TarArchiveEntry te) {
        if (te.isSymbolicLink()) {
            return Entry.Type.SYMLINK;
        } else if (te.isDirectory()) {
            return Entry.Type.DIR;
        } else {
            return Entry.Type.FILE;
        }
    }

    /**
     * A builder for {@code TarBaseDecompressor}.
     *
     * @param <T> the type of the {@code TarBaseDecompressor}
     * @param <O> the type of the {@code TarBaseDecompressor.Builder}
     */
    public abstract static class Builder<T extends Decompressor<T, O>, O extends Decompressor.Builder<T, O>>
            extends Decompressor.Builder<T, O> {
        private final InputStream sourceStream;

        /**
         * Creates a new {@code TarBaseDecompressor.Builder} with the given {@code Path}
         *
         * @param path the {@code Path} to the tar file
         * @throws IOException if an I/O error occurs
         */
        protected Builder(Path path) throws IOException {
            super();
            this.sourceStream = Files.newInputStream(path);
        }

        /**
         * Creates a new {@code TarBaseDecompressor.Builder} with the given {@code InputStream}
         *
         * @param stream the {@code InputStream} to the tar file
         */
        protected Builder(InputStream stream) {
            super();
            this.sourceStream = stream;
        }
    }
}
