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
package io.github.compress4j.compressors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.compress4j.compressors.memory.InMemoryCompressor;
import io.github.compress4j.compressors.memory.InMemoryCompressor.InMemoryCompressorBuilder;
import io.github.compress4j.compressors.memory.InMemoryCompressorOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompressorBuilderTest {

    @Mock
    private OutputStream mockOutputStream;

    private InMemoryCompressorBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new InMemoryCompressorBuilder(mockOutputStream);
    }

    @Test
    @DisplayName("Should construct CompressorBuilder with OutputStream")
    void constructor_WithOutputStream_SetsField() {
        assertThat(builder).isNotNull();
        assertThat(builder.outputStream).isEqualTo(mockOutputStream);
    }

    @Test
    @DisplayName("Should return the builder instance itself from getThis()")
    void getThis_ReturnsThisInstance() {
        assertThat(builder.getThis()).isEqualTo(builder);
    }

    @Test
    @DisplayName("Should build a CompressorOutputStream instance")
    void buildCompressorOutputStream_ReturnsCompressorOutputStreamInstance() throws IOException {
        // when
        CompressorOutputStream<?> compressorOutputStream = builder.buildCompressorOutputStream();

        // then
        assertThat(compressorOutputStream).isNotNull();
    }

    @Test
    @DisplayName("Should build a Compressor instance")
    void build_ReturnsCompressorInstance() throws IOException {
        // when
        InMemoryCompressor compressor = builder.build();

        // then
        assertThat(compressor).isNotNull();
    }

    @Test
    @DisplayName("Should throw IOException when building CompressorOutputStream fails")
    void buildCompressorOutputStream_ThrowsIOException_WhenOutputStreamFails() {
        // given
        OutputStream throwingOutputStream = mock(OutputStream.class);
        InMemoryCompressorBuilder failingBuilder = new InMemoryCompressorBuilder(throwingOutputStream) {
            @Override
            public InMemoryCompressorOutputStream buildCompressorOutputStream() throws IOException {
                throw new IOException("Simulated CompressorOutputStream build error");
            }
        };

        // when & then
        assertThatThrownBy(failingBuilder::buildCompressorOutputStream)
                .isInstanceOf(IOException.class)
                .hasMessage("Simulated CompressorOutputStream build error");
    }

    @Test
    @DisplayName("Should throw IOException when building Compressor fails due to CompressorOutputStream build error")
    void build_ThrowsIOException_WhenCompressorOutputStreamBuildFails() {
        // given
        OutputStream throwingOutputStream = mock(OutputStream.class);
        InMemoryCompressorBuilder failingBuilder = new InMemoryCompressorBuilder(throwingOutputStream) {
            @Override
            public InMemoryCompressorOutputStream buildCompressorOutputStream() throws IOException {
                throw new IOException("Simulated CompressorOutputStream build error during Compressor build");
            }
        };

        // when & then
        assertThatThrownBy(failingBuilder::build)
                .isInstanceOf(IOException.class)
                .hasMessage("Simulated CompressorOutputStream build error during Compressor build");
    }
}
