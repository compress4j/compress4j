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

import static org.apache.commons.io.IOUtils.closeQuietly;

import jakarta.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * Archiver that overwrites the extraction of Zip archives. It provides a wrapper for ZipFile as an ArchiveInputStream
 * to retrieve file attributes properly.
 */
class ZipFileArchiver extends CommonsArchiver<ZipArchiveEntry> {

    ZipFileArchiver() {
        super(ArchiveFormat.ZIP);
    }

    @Override
    protected ArchiveInputStream<ZipArchiveEntry> createArchiveInputStream(File archive) throws IOException {
        return new ZipFileArchiveInputStream(ZipFile.builder().setFile(archive).get());
    }

    /** Wraps a ZipFile to make it usable as an ArchiveInputStream. */
    static class ZipFileArchiveInputStream extends ArchiveInputStream<ZipArchiveEntry> {

        private final ZipFile file;

        private Enumeration<ZipArchiveEntry> entries;
        private ZipArchiveEntry currentEntry;
        private InputStream currentEntryStream;

        public ZipFileArchiveInputStream(ZipFile file) {
            this.file = file;
        }

        @Override
        public ZipArchiveEntry getNextEntry() throws IOException {
            Enumeration<ZipArchiveEntry> enumerationEntries = getEntries();

            closeCurrentEntryStream();

            currentEntry = (enumerationEntries.hasMoreElements()) ? enumerationEntries.nextElement() : null;
            currentEntryStream = (currentEntry != null) ? file.getInputStream(currentEntry) : null;

            return currentEntry;
        }

        @Override
        public int read(@Nonnull byte[] b, int off, int len) throws IOException {
            int read = getCurrentEntryStream().read(b, off, len);

            if (read == -1) {
                closeQuietly(getCurrentEntryStream());
            }

            count(read);

            return read;
        }

        @Override
        public boolean canReadEntryData(ArchiveEntry archiveEntry) {
            return archiveEntry == getCurrentEntry();
        }

        public ZipArchiveEntry getCurrentEntry() {
            return currentEntry;
        }

        public InputStream getCurrentEntryStream() {
            return currentEntryStream;
        }

        private Enumeration<ZipArchiveEntry> getEntries() {
            if (entries == null) {
                entries = file.getEntriesInPhysicalOrder();
            }
            return entries;
        }

        private void closeCurrentEntryStream() {
            closeQuietly(getCurrentEntryStream());

            currentEntryStream = null;
        }

        private void closeFile() {
            try {
                file.close();
            } catch (IOException e) {
                // close quietly
            }
        }

        @Override
        public void close() throws IOException {
            closeCurrentEntryStream();
            closeFile();

            super.close();
        }
    }
}
