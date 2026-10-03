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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Tracks what one {@link ArchiveExtractor#extract} run has consumed against its {@link ExtractionLimits}. */
final class ExtractionBudget {

    private final ExtractionLimits limits;
    private long entries = 0;
    long extractedBytes = 0;

    ExtractionBudget(ExtractionLimits limits) {
        this.limits = limits;
    }

    void countEntry() throws ArchiveLimitExceededException {
        if (limits.maxEntries() >= 0 && ++entries > limits.maxEntries()) {
            throw new ArchiveLimitExceededException(
                    "Archive holds more than the maximum of " + limits.maxEntries() + " entries allowed");
        }
    }

    InputStream meter(String entryName, InputStream in) {
        if (limits.maxEntrySize() < 0 && limits.maxTotalSize() < 0) {
            return in;
        }
        return new MeteredInputStream(entryName, in);
    }

    private final class MeteredInputStream extends FilterInputStream {
        private final String entryName;
        private long entryBytes;

        private MeteredInputStream(String entryName, InputStream in) {
            super(in);
            this.entryName = entryName;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) {
                count(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = super.read(buffer, offset, length);
            if (n > 0) {
                count(n);
            }
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = super.skip(n);
            count(skipped);
            return skipped;
        }

        private void count(long n) throws ArchiveLimitExceededException {
            entryBytes += n;
            extractedBytes += n;
            if (limits.maxEntrySize() >= 0 && entryBytes > limits.maxEntrySize()) {
                throw new ArchiveLimitExceededException("Entry '" + entryName + "' expands beyond the maximum entry "
                        + "size of " + limits.maxEntrySize() + " bytes");
            }
            if (limits.maxTotalSize() >= 0 && extractedBytes > limits.maxTotalSize()) {
                throw new ArchiveLimitExceededException("Archive expands beyond the maximum total size of "
                        + limits.maxTotalSize() + " bytes at entry '" + entryName + "'");
            }
        }
    }
}
