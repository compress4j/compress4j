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
package io.github.compress4j.exceptions;

import java.io.IOException;

/**
 * Exception thrown when an archive exceeds one of the extraction limits configured on the extractor.
 *
 * <p>This exception bypasses the error handler: a breached limit aborts the extraction regardless of the
 * {@code io.github.compress4j.archivers.ArchiveExtractor.ErrorHandlerChoice} the handler would return, so that a
 * handler which skips or ignores errors cannot be used to keep feeding a decompression bomb.
 *
 * @since 3.1
 */
public class ArchiveLimitExceededException extends IOException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public ArchiveLimitExceededException(String message) {
        super(message);
    }
}
