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
package io.github.compress4j.archivers;

import io.github.compress4j.exceptions.ArchiveLimitExceededException;

/**
 * Extraction limits of an {@link ArchiveExtractor}; a negative value disables the corresponding limit.
 *
 * @param maxEntries maximum number of entries to extract
 * @param maxEntrySize maximum number of bytes a single entry may expand to
 * @param maxTotalSize maximum number of bytes the whole archive may expand to
 */
record ExtractionLimits(long maxEntries, long maxEntrySize, long maxTotalSize) {

    static final ExtractionLimits NONE =
            new ExtractionLimits(ArchiveExtractor.UNLIMITED, ArchiveExtractor.UNLIMITED, ArchiveExtractor.UNLIMITED);

    ExtractionLimits withMaxEntries(long value) {
        return new ExtractionLimits(value, maxEntrySize, maxTotalSize);
    }

    ExtractionLimits withMaxEntrySize(long value) {
        return new ExtractionLimits(maxEntries, value, maxTotalSize);
    }

    ExtractionLimits withMaxTotalSize(long value) {
        return new ExtractionLimits(maxEntries, maxEntrySize, value);
    }

    void checkDeclaredSize(String entryName, long declaredSize) throws ArchiveLimitExceededException {
        if (maxEntrySize >= 0 && declaredSize > maxEntrySize) {
            throw new ArchiveLimitExceededException(
                    "Entry '" + entryName + "' expands beyond the maximum entry size of " + maxEntrySize + " bytes");
        }
    }
}
