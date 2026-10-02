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
import static com.hominux.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.RETRY;
import static com.hominux.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.SKIP;
import static com.hominux.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice.SKIP_ALL;

import com.hominux.compress4j.archivers.ArchiveExtractor.Entry;
import com.hominux.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice;
import java.io.IOException;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Turns an {@link IOException} raised while extracting one entry into what the extraction loop does next. */
final class ExtractionErrorPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractionErrorPolicy.class);

    private final BiFunction<Entry, ? super IOException, ErrorHandlerChoice> errorHandler;

    ExtractionErrorPolicy(BiFunction<Entry, ? super IOException, ErrorHandlerChoice> errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Handles an {@link IOException} that occurred during extraction.
     *
     * @param ioException the exception that occurred
     * @param ignoreErrors whether {@link ErrorHandlerChoice#SKIP_ALL} was selected for an earlier entry
     * @param entry the entry that caused the exception
     * @return ErrorHandlerChoice - the decision on how to handle the exception
     * @throws IOException if an I/O error occurs
     */
    ErrorHandlerChoice handle(IOException ioException, boolean ignoreErrors, Entry entry) throws IOException {
        if (ignoreErrors) {
            LOGGER.debug("Skipped exception because {} was selected earlier", SKIP_ALL, ioException);
            return SKIP_ALL;
        } else {
            return switch (errorHandler.apply(entry, ioException)) {
                case ABORT -> ABORT;
                case BAIL_OUT -> throw ioException;
                case RETRY -> {
                    LOGGER.debug("Retying because of exception", ioException);
                    yield RETRY;
                }
                case SKIP -> {
                    LOGGER.debug("Skipped exception", ioException);
                    yield SKIP;
                }
                case SKIP_ALL -> {
                    LOGGER.debug("SKIP_ALL is selected", ioException);
                    yield SKIP_ALL;
                }
            };
        }
    }

    static EntryOutcome outcomeOf(ErrorHandlerChoice choice) {
        return switch (choice) {
            case ABORT -> new EntryOutcome.Abort();
            case SKIP_ALL -> new EntryOutcome.IgnoreFurtherErrors();
            case SKIP, RETRY, BAIL_OUT -> new EntryOutcome.Continue();
        };
    }

    sealed interface EntryOutcome {
        record Continue() implements EntryOutcome {}

        record Abort() implements EntryOutcome {}

        record IgnoreFurtherErrors() implements EntryOutcome {}
    }
}
