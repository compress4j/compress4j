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

import io.github.compress4j.compressors.memory.InMemoryDecompressor;
import io.github.compress4j.compressors.memory.InMemoryDecompressorInputStream;
import java.io.IOException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DecompressorBuilderTest {

    @Mock
    private InMemoryDecompressorInputStream mockCompressorInputStream;

    private InMemoryDecompressor.InMemoryDecompressorBuilder builder;

    @BeforeEach
    void setUp() {
        builder = InMemoryDecompressor.builder(mockCompressorInputStream);
    }

    @Test
    @DisplayName("Should construct DecompressorBuilder with CompressorInputStream")
    void constructor_WithCompressorInputStream_SetsField() {
        assertThat(builder).isNotNull();
        assertThat(builder.buildCompressorInputStream()).isInstanceOf(CompressorInputStream.class);
    }

    @Test
    @DisplayName("Should return the builder instance itself from getThis()")
    void getThis_ReturnsThisInstance() {
        assertThat(builder.getThis()).isEqualTo(builder);
    }

    @Test
    @DisplayName("Should build a Decompressor instance")
    void build_ReturnsDecompressorInstance() throws IOException {
        // when
        InMemoryDecompressor decompressor = builder.build();

        // then
        assertThat(decompressor).isNotNull();
        assertThat(decompressor.compressorInputStream).isInstanceOf(CompressorInputStream.class);
    }

    @Test
    @DisplayName("Should throw IOException when building if CompressorInputStream causes an error")
    void build_ThrowsIOException_WhenCompressorInputStreamFails() {
        // given
        InMemoryDecompressorInputStream throwingStream = mock(InMemoryDecompressorInputStream.class);
        InMemoryDecompressor.InMemoryDecompressorBuilder failingBuilder =
                new InMemoryDecompressor.InMemoryDecompressorBuilder(throwingStream) {
                    @Override
                    public InMemoryDecompressor build() throws IOException {
                        if (inputStream == throwingStream) {
                            throw new IOException("Simulated build error");
                        }
                        return super.build();
                    }
                };

        // when & then
        assertThatThrownBy(failingBuilder::build)
                .isInstanceOf(IOException.class)
                .hasMessage("Simulated build error");
    }
}
