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
package io.github.compress4j.archivers;

import static io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.RETRY;
import static io.github.compress4j.archivers.ArchiveExtractor.EscapingSymlinkPolicy.DISALLOW;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.compress4j.archivers.ArchiveExtractor.Entry;
import io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice;
import io.github.compress4j.archivers.memory.InMemoryArchiveExtractor;
import io.github.compress4j.archivers.memory.InMemoryArchiveExtractor.InMemoryArchiveExtractorBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class ArchiveExtractorBuilderTest {

    @Test
    void shouldBuildArchiveExtractor() throws IOException {
        // given
        Predicate<Entry> filter = entry -> !entry.name.contains("some");
        BiFunction<Entry, IOException, ErrorHandlerChoice> errorHandler = (entry, exception) -> RETRY;
        AtomicInteger counter = new AtomicInteger();
        BiConsumer<Entry, Path> postProcessor = (entry, path) -> counter.incrementAndGet();
        InMemoryArchiveExtractorBuilder builder = InMemoryArchiveExtractor.builder(List.of())
                .filter(filter)
                .errorHandler(errorHandler)
                .overwrite(true)
                .escapingSymlinkPolicy(DISALLOW)
                .stripComponents(5)
                .postProcessor(postProcessor);

        // when
        try (InMemoryArchiveExtractor extractor = builder.build()) {
            // then
            assertThat(extractor)
                    .extracting(
                            "entryFilter",
                            "errorHandler",
                            "overwrite",
                            "escapingSymlinkPolicy",
                            "stripComponents",
                            "postProcessor")
                    .containsExactly(Optional.of(filter), errorHandler, true, DISALLOW, 5, postProcessor);
        }
    }
}
