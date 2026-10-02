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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hominux.compress4j.exceptions.ArchiveLimitExceededException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ExtractionBudgetTest {

    private static ByteArrayInputStream bytes(int n) {
        return new ByteArrayInputStream(new byte[n]);
    }

    @Test
    void countEntry_throwsOnceMaxEntriesIsExceeded() throws IOException {
        ExtractionBudget budget = new ExtractionBudget(ExtractionLimits.NONE.withMaxEntries(2));
        budget.countEntry();
        budget.countEntry();
        assertThatThrownBy(budget::countEntry)
                .isInstanceOf(ArchiveLimitExceededException.class)
                .hasMessage("Archive holds more than the maximum of 2 entries allowed");
    }

    @Test
    void countEntry_neverThrowsWhenUnlimited() {
        ExtractionBudget budget = new ExtractionBudget(ExtractionLimits.NONE);
        assertThatCode(() -> {
                    for (int i = 0; i < 1000; i++) budget.countEntry();
                })
                .doesNotThrowAnyException();
    }

    @Test
    void transfer_copiesEverythingWhenUnlimited() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ExtractionBudget(ExtractionLimits.NONE).transfer("a", bytes(50_000), out);
        assertThat(out.size()).isEqualTo(50_000);
    }

    @Test
    void transfer_allowsAnEntryAtTheEntryLimit() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ExtractionBudget(ExtractionLimits.NONE.withMaxEntrySize(4)).transfer("a", bytes(4), out);
        assertThat(out.size()).isEqualTo(4);
    }

    @Test
    void transfer_throwsWhenAnEntryExceedsTheEntryLimit() {
        ExtractionBudget budget = new ExtractionBudget(ExtractionLimits.NONE.withMaxEntrySize(4));
        assertThatThrownBy(() -> budget.transfer("big.bin", bytes(5), new ByteArrayOutputStream()))
                .isInstanceOf(ArchiveLimitExceededException.class)
                .hasMessage("Entry 'big.bin' expands beyond the maximum entry size of 4 bytes");
    }

    @Test
    void transfer_throwsWhenTheArchiveExceedsTheTotalLimit() throws IOException {
        ExtractionBudget budget = new ExtractionBudget(ExtractionLimits.NONE.withMaxTotalSize(6));
        budget.transfer("a", bytes(4), new ByteArrayOutputStream());
        assertThatThrownBy(() -> budget.transfer("b", bytes(4), new ByteArrayOutputStream()))
                .isInstanceOf(ArchiveLimitExceededException.class)
                .hasMessage("Archive expands beyond the maximum total size of 6 bytes at entry 'b'");
    }

    @Test
    void newBudget_startsFromZero() throws IOException {
        ExtractionLimits limits = ExtractionLimits.NONE.withMaxTotalSize(4).withMaxEntries(1);
        ExtractionBudget first = new ExtractionBudget(limits);
        first.countEntry();
        first.transfer("a", bytes(4), new ByteArrayOutputStream());

        ExtractionBudget second = new ExtractionBudget(limits);
        assertThatCode(() -> {
                    second.countEntry();
                    second.transfer("a", bytes(4), new ByteArrayOutputStream());
                })
                .doesNotThrowAnyException();
    }
}
