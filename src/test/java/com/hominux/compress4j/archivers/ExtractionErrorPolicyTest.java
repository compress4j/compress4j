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

import static com.hominux.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.ABORT;
import static com.hominux.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.BAIL_OUT;
import static com.hominux.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.RETRY;
import static com.hominux.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.SKIP;
import static com.hominux.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.SKIP_ALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hominux.compress4j.archivers.ExtractionErrorPolicy.EntryOutcome;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ExtractionErrorPolicyTest {

    private static final ArchiveExtractor.Entry ENTRY = new ArchiveExtractor.Entry("a.txt", false);
    private static final IOException FAILURE = new IOException("boom");

    @Test
    void handle_skipsWithoutAskingTheHandlerAfterSkipAll() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        ExtractionErrorPolicy policy = new ExtractionErrorPolicy((e, x) -> {
            calls.incrementAndGet();
            return ABORT;
        });
        assertThat(policy.handle(FAILURE, true, ENTRY)).isEqualTo(SKIP_ALL);
        assertThat(calls).hasValue(0);
    }

    @Test
    void handle_rethrowsOnBailOut() {
        ExtractionErrorPolicy policy = new ExtractionErrorPolicy((e, x) -> BAIL_OUT);
        assertThatThrownBy(() -> policy.handle(FAILURE, false, ENTRY)).isSameAs(FAILURE);
    }

    @Test
    void handle_passesThroughTheHandlerChoice() throws IOException {
        assertThat(new ExtractionErrorPolicy((e, x) -> RETRY).handle(FAILURE, false, ENTRY))
                .isEqualTo(RETRY);
        assertThat(new ExtractionErrorPolicy((e, x) -> SKIP).handle(FAILURE, false, ENTRY))
                .isEqualTo(SKIP);
    }

    @Test
    void outcomeOf_mapsEveryChoice() {
        assertThat(ExtractionErrorPolicy.outcomeOf(ABORT)).isInstanceOf(EntryOutcome.Abort.class);
        assertThat(ExtractionErrorPolicy.outcomeOf(SKIP_ALL)).isInstanceOf(EntryOutcome.IgnoreFurtherErrors.class);
        assertThat(ExtractionErrorPolicy.outcomeOf(SKIP)).isInstanceOf(EntryOutcome.Continue.class);
        assertThat(ExtractionErrorPolicy.outcomeOf(RETRY)).isInstanceOf(EntryOutcome.Continue.class);
        assertThat(ExtractionErrorPolicy.outcomeOf(BAIL_OUT)).isInstanceOf(EntryOutcome.Continue.class);
    }
}
