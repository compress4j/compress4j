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

import io.github.compress4j.utils.SystemUtils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

/** The Tar decompressor automatically detects the compression of an input file/stream. */
@SuppressWarnings("unused")
public final class TarDecompressor extends Decompressor<TarDecompressor, TarDecompressor.Builder> {
    private final InputStream sourceStream;
    private TarArchiveInputStream archiveInputStream;

    private TarDecompressor(Builder builder) {
        super(builder);
        sourceStream = builder.sourceStream;
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

    /** {@inheritDoc} */
    @Override
    protected void openStream() throws IOException {
        InputStream input = new BufferedInputStream(sourceStream);
        try {
            input = new CompressorStreamFactory().createCompressorInputStream(input);
        } catch (CompressorException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) throw ioException;
        }
        archiveInputStream = new TarArchiveInputStream(input);
    }

    /** {@inheritDoc} */
    @Override
    protected Entry nextEntry() throws IOException {
        TarArchiveEntry te;
        te = getNextTarArchiveEntry();
        if (te == null) return null;
        if (!SystemUtils.IS_OS_WINDOWS)
            return new Entry(te.getName(), type(te), te.getMode(), te.getLinkName(), te.getSize());
        // UNIX permissions are ignored on Windows
        if (te.isSymbolicLink()) return new Entry(te.getName(), Entry.Type.SYMLINK, 0, te.getLinkName(), te.getSize());
        return new Entry(te.getName(), te.isDirectory(), te.getSize());
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
            getNextTarArchiveEntry();
        }
        return te;
    }

    /** {@inheritDoc} */
    @Override
    protected InputStream openEntryStream(Entry entry) {
        return archiveInputStream;
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

    /**
     * Creates a new {@code TarDecompressor.Builder} with the given {@code Path}
     *
     * @param path the {@code Path} to the tar file
     */
    public static Builder builder(Path path) throws IOException {
        return new Builder(path);
    }

    /**
     * Creates a new {@code TarDecompressor.Builder} with the given {@code InputStream}
     *
     * @param stream the {@code InputStream} to the tar file
     */
    public static Builder builder(InputStream stream) {
        return new Builder(stream);
    }

    public static final class Builder extends Decompressor.Builder<TarDecompressor, Builder> {
        private final InputStream sourceStream;

        private Builder(Path path) throws IOException {
            super();
            this.sourceStream = Files.newInputStream(path);
        }

        private Builder(InputStream stream) {
            super();
            this.sourceStream = stream;
        }

        /**
         * Builds the MessageRequest.
         *
         * @return A MessageRequest, populated with all fields from this builder.
         */
        @Override
        public TarDecompressor build() {
            return new TarDecompressor(this);
        }
    }
}
