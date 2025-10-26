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
package io.github.compress4j.archivers.tar;

import io.github.compress4j.archivers.ArchiveExtractor;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Tar Base ArchiveExtractor
 *
 * @since 2.2
 */
public abstract class BaseTarArchiveExtractor extends ArchiveExtractor<TarArchiveInputStream> {
    /**
     * Create a new {@link BaseTarArchiveExtractor} with the given input stream.
     *
     * @param builder - the archive input stream builder
     * @param <B> The type of {@link BaseTarArchiveExtractorBuilder} to build a {@link BaseTarArchiveExtractor} from.
     * @param <C> The type of the {@link BaseTarArchiveExtractor} to build
     * @throws IOException if an I/O error occurred
     */
    protected <B extends BaseTarArchiveExtractorBuilder<B, C>, C extends ArchiveExtractor<TarArchiveInputStream>>
            BaseTarArchiveExtractor(B builder) throws IOException {
        super(builder);
    }

    /**
     * Creates a new {@code BaseTarArchiveExtractor}
     *
     * @param tarArchiveInputStream - the {@code TarArchiveInputStream} to the tar file
     */
    protected BaseTarArchiveExtractor(TarArchiveInputStream tarArchiveInputStream) {
        super(tarArchiveInputStream);
    }

    /** {@inheritDoc} */
    @Override
    protected Entry nextEntry() throws IOException {
        TarArchiveEntry te = getNextTarArchiveEntry();
        if (te == null) {
            return null;
        } else if (!isIsOsWindows()) {
            return new Entry(te.getName(), type(te), te.getMode(), te.getLinkName(), te.getSize());
        } else if (te.isSymbolicLink()) {
            return new Entry(te.getName(), Entry.Type.SYMLINK, 0, te.getLinkName(), te.getSize());
        } else {
            return new Entry(te.getName(), te.isDirectory(), te.getSize());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected InputStream openEntryStream(Entry entry) {
        return archiveInputStream;
    }

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
            return getNextTarArchiveEntry();
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
     * Base builder to build a TAR/TAR.GZ extractor
     *
     * @param <B> the type of the Builder.
     * @param <C> the type of the ArchiveExtractor.
     */
    public abstract static class BaseTarArchiveExtractorBuilder<
                    B extends BaseTarArchiveExtractorBuilder<B, C>, C extends ArchiveExtractor<TarArchiveInputStream>>
            extends ArchiveExtractorBuilder<TarArchiveInputStream, B, C> {

        /** Input stream to read from for extraction. */
        protected final InputStream inputStream;

        /**
         * Create a new ArchiveExtractorBuilder.
         *
         * @param inputStream the input stream
         */
        protected BaseTarArchiveExtractorBuilder(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        /**
         * Build a {@code A} from the given {@code InputStream}. If you want to combine an archive format with a
         * compression format - like when reading a `tar.gz` file - you wrap the {@code ArchiveInputStream} around
         * {@code CompressorInputStream} for example:
         *
         * <pre>{@code
         * return new TarArchiveInputStream(new GzipCompressorInputStream(inputStream));
         * }</pre>
         *
         * @param inputStream - the {@code InputStream} to the compressed file
         * @return a {@code A} from the given {@code InputStream}
         */
        protected TarArchiveInputStream buildTarArchiveInputStream(InputStream inputStream) {
            return new TarArchiveInputStream(inputStream);
        }
    }
}
