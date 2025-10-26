/*
 * Copyright 2025 The Compress4J Project
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
package io.github.compress4j.archivers.zip;

import static org.apache.commons.io.IOUtils.closeQuietly;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/** Wraps a {@link ZipFile} to make it usable as an {@link ArchiveInputStream}. */
public class ZipFileArchiveInputStream extends ArchiveInputStream<ZipArchiveEntry> {

    private final ZipFile file;

    private Enumeration<ZipArchiveEntry> entries;
    private ZipArchiveEntry currentEntry;
    private InputStream currentEntryStream;

    /**
     * Creates a new {@code ZipFileArchiveInputStream} wrapping the given {@link ZipFile}.
     *
     * @param file the {@code ZipFile} to wrap
     */
    public ZipFileArchiveInputStream(ZipFile file) {
        this.file = file;
    }

    /** {@inheritDoc} */
    @Override
    public ZipArchiveEntry getNextEntry() throws IOException {
        Enumeration<ZipArchiveEntry> enumerationEntries = getEntries();

        closeCurrentEntryStream();

        currentEntry = (enumerationEntries.hasMoreElements()) ? enumerationEntries.nextElement() : null;
        currentEntryStream = (currentEntry != null) ? file.getInputStream(currentEntry) : null;

        return currentEntry;
    }

    /**
     * Gets the entry's content as a String if isUnixSymlink() returns true for it, otherwise returns null.
     *
     * <p>This method assumes the symbolic link's file name uses the same encoding that as been specified for this
     * ZipFile.
     *
     * @param entry ZipArchiveEntry object that represents the symbolic link
     * @return entry's content as a String
     * @throws IOException problem with content's input stream
     */
    public String getUnixSymlink(final ZipArchiveEntry entry) throws IOException {
        return file.getUnixSymlink(entry);
    }

    /** {@inheritDoc} */
    @Override
    public int read(@Nonnull byte[] b, int off, int len) throws IOException {
        int read = getCurrentEntryStream().read(b, off, len);

        if (read == -1) {
            closeQuietly(getCurrentEntryStream());
        }

        count(read);

        return read;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canReadEntryData(ArchiveEntry archiveEntry) {
        return archiveEntry == getCurrentEntry();
    }

    /**
     * Get the current entry.
     *
     * @return the current entry
     */
    public ZipArchiveEntry getCurrentEntry() {
        return currentEntry;
    }

    /**
     * Get the input stream for the current entry.
     *
     * @return the input stream for the current entry
     */
    public InputStream getCurrentEntryStream() {
        return currentEntryStream;
    }

    /**
     * Get the entries' enumeration.
     *
     * @return the entries enumeration
     */
    private Enumeration<ZipArchiveEntry> getEntries() {
        if (entries == null) {
            entries = file.getEntriesInPhysicalOrder();
        }
        return entries;
    }

    /** Close the current entry stream. */
    private void closeCurrentEntryStream() {
        closeQuietly(getCurrentEntryStream());

        currentEntryStream = null;
    }

    /** Close the underlying ZipFile. */
    private void closeFile() {
        try {
            file.close();
        } catch (IOException e) {
            // close quietly
        }
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream.
     *
     * @throws IOException if an I/O error occurs.
     * @see java.io.FilterInputStream#in
     */
    @Override
    public void close() throws IOException {
        closeCurrentEntryStream();
        closeFile();

        super.close();
    }
}
