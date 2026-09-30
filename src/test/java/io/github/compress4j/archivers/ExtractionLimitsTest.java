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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.compress4j.exceptions.ArchiveLimitExceededException;
import org.junit.jupiter.api.Test;

class ExtractionLimitsTest {

    @Test
    void checkDeclaredSize_allowsSizeAtTheLimit() {
        ExtractionLimits limits = ExtractionLimits.NONE.withMaxEntrySize(10);
        assertThatCode(() -> limits.checkDeclaredSize("a", 10)).doesNotThrowAnyException();
    }

    @Test
    void checkDeclaredSize_rejectsSizeAboveTheLimit() {
        ExtractionLimits limits = ExtractionLimits.NONE.withMaxEntrySize(10);
        assertThatThrownBy(() -> limits.checkDeclaredSize("a", 11))
                .isInstanceOf(ArchiveLimitExceededException.class)
                .hasMessage("Entry 'a' expands beyond the maximum entry size of 10 bytes");
    }

    @Test
    void checkDeclaredSize_neverRejectsWhenUnlimited() {
        assertThatCode(() -> ExtractionLimits.NONE.checkDeclaredSize("a", Long.MAX_VALUE))
                .doesNotThrowAnyException();
    }
}
