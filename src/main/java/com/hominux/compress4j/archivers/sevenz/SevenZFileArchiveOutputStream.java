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
package com.hominux.compress4j.archivers.sevenz;

import jakarta.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

/** Wraps a {@link SevenZOutputFile} to make it usable as an {@link ArchiveOutputStream}. */
public class SevenZFileArchiveOutputStream extends ArchiveOutputStream<SevenZArchiveEntry> {

    private final SevenZOutputFile file;

    /**
     * Creates a new {@code SevenZFileArchiveOutputStream} wrapping the given {@link SevenZOutputFile}.
     *
     * @param file the {@code SevenZOutputFile} to wrap
     */
    public SevenZFileArchiveOutputStream(SevenZOutputFile file) {
        super(OutputStream.nullOutputStream());
        this.file = Objects.requireNonNull(file);
    }

    /** {@inheritDoc} */
    @Override
    public void putArchiveEntry(SevenZArchiveEntry entry) throws IOException {
        file.putArchiveEntry(entry);
    }

    /** {@inheritDoc} */
    @Override
    public void closeArchiveEntry() throws IOException {
        file.closeArchiveEntry();
    }

    /** {@inheritDoc} */
    @Override
    public SevenZArchiveEntry createArchiveEntry(File inputFile, String entryName) {
        return file.createArchiveEntry(inputFile, entryName);
    }

    /** {@inheritDoc} */
    @Override
    public void write(@Nonnull byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        file.write(b, off, len);
        count(len);
    }

    /** {@inheritDoc} */
    @Override
    public void write(int b) throws IOException {
        file.write(b);
        count(1);
    }

    /** {@inheritDoc} */
    @Override
    public void finish() throws IOException {
        file.finish();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        file.close();
        super.close();
    }
}
