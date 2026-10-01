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
package io.github.compress4j.archivers.sevenz;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.Objects;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

/** Wraps a {@link SevenZFile} to make it usable as an {@link ArchiveInputStream}. */
public class SevenZFileArchiveInputStream extends ArchiveInputStream<SevenZArchiveEntry> {

    private final SevenZFile file;

    /**
     * Creates a new {@code SevenZFileArchiveInputStream} wrapping the given {@link SevenZFile}.
     *
     * @param file the {@code SevenZFile} to wrap
     */
    public SevenZFileArchiveInputStream(SevenZFile file) {
        this.file = Objects.requireNonNull(file);
    }

    /** {@inheritDoc} */
    @Override
    public SevenZArchiveEntry getNextEntry() throws IOException {
        return file.getNextEntry();
    }

    /** {@inheritDoc} */
    @Override
    public int read(@Nonnull byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        int read = file.read(b, off, len);
        if (read != -1) {
            count(read);
        }
        return read;
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        int value = file.read();
        if (value != -1) {
            count(1);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        file.close();
        super.close();
    }
}
