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
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ExtractionBudgetTest {

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
    void meterThrowsOnTheReadThatCrossesTheEntryLimit() throws IOException {
        // Given
        var budget = new ExtractionBudget(new ExtractionLimits(-1, 4, -1));
        var in = budget.meter("e", new ByteArrayInputStream(new byte[10]));

        // When
        int first = in.read(new byte[4]);

        // Then
        assertThat(first).isEqualTo(4);
        assertThatThrownBy(() -> in.read())
                .isInstanceOf(ArchiveLimitExceededException.class)
                .hasMessageContaining("'e'");
    }

    @Test
    void meterCountsTotalAcrossEntries() throws IOException {
        // Given
        var budget = new ExtractionBudget(new ExtractionLimits(-1, -1, 6));

        // When
        budget.meter("a", new ByteArrayInputStream(new byte[4])).readAllBytes();
        var second = budget.meter("b", new ByteArrayInputStream(new byte[4]));

        // Then
        assertThatThrownBy(second::readAllBytes).isInstanceOf(ArchiveLimitExceededException.class);
    }

    @Test
    void meterCountsSkippedBytes() throws IOException {
        // Given
        var budget = new ExtractionBudget(new ExtractionLimits(-1, 4, -1));
        var in = budget.meter("s", new ByteArrayInputStream(new byte[10]));

        // Then
        assertThatThrownBy(() -> in.skip(10)).isInstanceOf(ArchiveLimitExceededException.class);
    }

    @Test
    void unlimitedBudgetReturnsTheSameStream() {
        var raw = new ByteArrayInputStream(new byte[1]);
        assertThat(new ExtractionBudget(ExtractionLimits.NONE).meter("x", raw)).isSameAs(raw);
    }

    @Test
    void entryOfExactlyMaxEntrySizeReadsFullyThenReturnsEof() throws IOException {
        // Given
        var budget = new ExtractionBudget(new ExtractionLimits(-1, 4, -1));
        var in = budget.meter("e", new ByteArrayInputStream(new byte[4]));

        // Then
        assertThat(in.readNBytes(4)).hasSize(4);
        assertThat(in.read()).isEqualTo(-1);
    }

    @Test
    void totalOfExactlyMaxTotalSizeAcrossTwoEntriesReadsWithoutThrowing() {
        // Given
        var budget = new ExtractionBudget(new ExtractionLimits(-1, -1, 6));

        // Then
        assertThatCode(() -> {
                    budget.meter("a", new ByteArrayInputStream(new byte[3])).readAllBytes();
                    budget.meter("b", new ByteArrayInputStream(new byte[3])).readAllBytes();
                })
                .doesNotThrowAnyException();
    }
}
