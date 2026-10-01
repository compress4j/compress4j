/*
 * Copyright 2026 The Compress4J Project
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
package io.github.compress4j.compressors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.compress4j.compressors.memory.InMemoryCompressor.InMemoryCompressorBuilder;
import io.github.compress4j.compressors.memory.InMemoryCompressorOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

class CompressorBuilderOwnershipTest {

    private final IOException failure = new IOException("build failed");

    private InMemoryCompressorBuilder failingBuilder(OutputStream stream, boolean owned) {
        return new InMemoryCompressorBuilder(stream, owned) {
            @Override
            public InMemoryCompressorOutputStream buildCompressorOutputStream() throws IOException {
                throw failure;
            }
        };
    }

    @Test
    void shouldCloseOwnedStreamWhenBuildFails() throws IOException {
        var stream = mock(OutputStream.class);
        var builder = failingBuilder(stream, true);

        assertThatThrownBy(builder::build).isSameAs(failure);

        verify(stream).close();
    }

    @Test
    void shouldKeepCallerSuppliedStreamOpenWhenBuildFails() throws IOException {
        var stream = mock(OutputStream.class);
        var builder = failingBuilder(stream, false);

        assertThatThrownBy(builder::build).isSameAs(failure);

        verify(stream, never()).close();
    }
}
