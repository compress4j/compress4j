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

import io.github.compress4j.archive.decompression.builder.TarArchiveInputStreamBuilder;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/** Tar Base Decompressor */
public abstract class TarBaseDecompressor extends Decompressor<TarArchiveInputStream> {

    /**
     * Creates a new {@code TarBaseDecompressor}
     *
     * @param tarArchiveInputStream - the {@code TarArchiveInputStream} to the tar file
     */
    protected TarBaseDecompressor(TarArchiveInputStream tarArchiveInputStream) {
        super(tarArchiveInputStream);
    }

    /**
     * Creates a new {@code TarBaseDecompressor}.
     *
     * @param builder - the {@code ArchiveInputStreamBuilder} to build the {@code TarArchiveInputStreamBuilder}.
     * @throws IOException - if the {@code TarArchiveInputStreamBuilder} could not be created
     */
    protected TarBaseDecompressor(TarArchiveInputStreamBuilder builder) throws IOException {
        super(builder);
    }

    /** {@inheritDoc} */
    @Override
    protected void closeEntryStream(InputStream stream) {
        // no-op
    }

    /** {@inheritDoc} */
    @Override
    protected Entry nextEntry() throws IOException {
        TarArchiveEntry te = getNextTarArchiveEntry();
        if (te == null) return null;
        if (!isIsOsWindows()) return new Entry(te.getName(), type(te), te.getMode(), te.getLinkName(), te.getSize());
        if (te.isSymbolicLink()) return new Entry(te.getName(), Entry.Type.SYMLINK, 0, te.getLinkName(), te.getSize());
        return new Entry(te.getName(), te.isDirectory(), te.getSize());
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
}
