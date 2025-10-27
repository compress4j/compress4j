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
package io.github.compress4j.compressors.pack200;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.compress.compressors.pack200.Pack200Strategy;
import org.junit.jupiter.api.Test;

class Pack200CompressorBuilderTest {

    @Test
    void shouldBuildPack200CompressorWithOutputStream() {
        // Given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        Pack200Compressor.Pack200CompressorBuilder builder = Pack200Compressor.builder(outputStream);

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    void shouldSetModeOnOutputStreamBuilder() {
        // Given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        Pack200Compressor.Pack200CompressorBuilder builder = Pack200Compressor.builder(outputStream)
                .compressorOutputStreamBuilder()
                .mode(Pack200Strategy.TEMP_FILE)
                .parentBuilder();

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    void shouldBuildCompressorOutputStream() throws Exception {
        // Given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        Pack200Compressor compressor = Pack200Compressor.builder(outputStream).build();

        // Then
        assertThat(compressor).isNotNull();
    }

    @Test
    void shouldSetPropertiesOnOutputStreamBuilder() {
        // Given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<String, String> properties = new HashMap<>();
        properties.put("pack.effort", "9");
        properties.put("pack.segment.limit", "-1");

        // When
        Pack200Compressor.Pack200CompressorBuilder builder = Pack200Compressor.builder(outputStream)
                .compressorOutputStreamBuilder()
                .properties(properties)
                .parentBuilder();

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    void shouldBuildCompressorWithProperties() throws Exception {
        // Given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<String, String> properties = new HashMap<>();
        properties.put("pack.effort", "5");

        // When
        Pack200Compressor compressor = Pack200Compressor.builder(outputStream)
                .compressorOutputStreamBuilder()
                .properties(properties)
                .parentBuilder()
                .build();

        // Then
        assertThat(compressor).isNotNull();
    }

    @Test
    void shouldHandleNullPropertiesGracefully() {
        // Given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        Pack200Compressor.Pack200CompressorBuilder builder = Pack200Compressor.builder(outputStream)
                .compressorOutputStreamBuilder()
                .properties(null)
                .parentBuilder();

        // Then
        assertThat(builder).isNotNull();
    }
}
