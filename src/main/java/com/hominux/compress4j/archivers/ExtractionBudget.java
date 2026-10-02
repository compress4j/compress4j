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
package com.hominux.compress4j.archivers;

import com.hominux.compress4j.exceptions.ArchiveLimitExceededException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Tracks what one {@link ArchiveExtractor#extract} run has consumed against its {@link ExtractionLimits}. */
final class ExtractionBudget {

    /**
     * Matches {@code InputStream.DEFAULT_BUFFER_SIZE}, so the counted copy reads in the same granularity as
     * {@link InputStream#transferTo(OutputStream)} does on the unlimited path.
     */
    private static final int TRANSFER_BUFFER_SIZE = 16384;

    private final ExtractionLimits limits;
    private long entries = 0;
    private long extractedBytes = 0;

    ExtractionBudget(ExtractionLimits limits) {
        this.limits = limits;
    }

    void countEntry() throws ArchiveLimitExceededException {
        if (limits.maxEntries() >= 0 && ++entries > limits.maxEntries()) {
            throw new ArchiveLimitExceededException(
                    "Archive holds more than the maximum of " + limits.maxEntries() + " entries allowed");
        }
    }

    /**
     * Copies the content of an entry, enforcing the entry and total size limits as the bytes go by rather than trusting
     * the size the archive declares.
     *
     * @param entryName the name of the entry being written
     * @param inputStream the stream to read the entry content from
     * @param outputStream the stream to write the entry content to
     * @throws IOException if an I/O error occurs
     * @throws ArchiveLimitExceededException if the entry, or the archive as a whole, expands beyond its limit
     */
    void transfer(String entryName, InputStream inputStream, OutputStream outputStream) throws IOException {
        if (limits.maxEntrySize() < 0 && limits.maxTotalSize() < 0) {
            extractedBytes += inputStream.transferTo(outputStream);
            return;
        }
        byte[] buffer = new byte[TRANSFER_BUFFER_SIZE];
        long entryBytes = 0;
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            entryBytes += read;
            extractedBytes += read;
            if (limits.maxEntrySize() >= 0 && entryBytes > limits.maxEntrySize()) {
                throw new ArchiveLimitExceededException("Entry '" + entryName + "' expands beyond the maximum entry "
                        + "size of " + limits.maxEntrySize() + " bytes");
            }
            if (limits.maxTotalSize() >= 0 && extractedBytes > limits.maxTotalSize()) {
                throw new ArchiveLimitExceededException("Archive expands beyond the maximum total size of "
                        + limits.maxTotalSize() + " bytes at entry '" + entryName + "'");
            }
            outputStream.write(buffer, 0, read);
        }
    }
}
