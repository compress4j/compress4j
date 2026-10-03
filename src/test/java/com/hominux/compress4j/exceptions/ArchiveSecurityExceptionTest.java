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
package com.hominux.compress4j.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ArchiveSecurityExceptionTest {

    @Test
    void limitAndUnsafeEntryAreSecurityExceptionsAndIOExceptions() {
        // Given
        IOException limit = new ArchiveLimitExceededException("too big");
        IOException unsafe = new UnsafeEntryException("../evil");

        // Then
        assertThat(limit).isInstanceOf(ArchiveSecurityException.class);
        assertThat(unsafe).isInstanceOf(ArchiveSecurityException.class);
    }

    @Test
    void unsafeEntryKeepsCause() {
        // Given
        var cause = new IOException("root");

        // When
        var unsafe = new UnsafeEntryException("bad link", cause);

        // Then
        assertThat(unsafe).hasMessage("bad link").hasCause(cause);
    }

    @Test
    void hierarchyIsClosed() {
        assertThat(ArchiveSecurityException.class.isSealed()).isTrue();
        assertThat(ArchiveSecurityException.class.getPermittedSubclasses())
                .containsExactlyInAnyOrder(ArchiveLimitExceededException.class, UnsafeEntryException.class);
    }
}
