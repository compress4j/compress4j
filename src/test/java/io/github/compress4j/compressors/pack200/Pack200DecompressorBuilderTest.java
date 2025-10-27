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

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.compress.compressors.pack200.Pack200Strategy;
import org.junit.jupiter.api.Test;

class Pack200DecompressorBuilderTest {

    @Test
    void shouldBuildPack200DecompressorWithInputStream() {
        // Given
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);

        // When
        Pack200Decompressor.Pack200DecompressorBuilder builder = Pack200Decompressor.builder(inputStream);

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    void shouldSetModeOnInputStreamBuilder() {
        // Given
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);

        // When
        Pack200Decompressor.Pack200DecompressorBuilder builder = Pack200Decompressor.builder(inputStream)
                .compressorInputStreamBuilder()
                .mode(Pack200Strategy.TEMP_FILE)
                .parentBuilder();

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    void shouldSetPropertiesOnInputStreamBuilder() {
        // Given
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        Map<String, String> properties = new HashMap<>();
        properties.put("pack.deflate.hint", "true");
        properties.put("pack.modification.time", "latest");

        // When
        Pack200Decompressor.Pack200DecompressorBuilder builder = Pack200Decompressor.builder(inputStream)
                .compressorInputStreamBuilder()
                .properties(properties)
                .parentBuilder();

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    void shouldBuildDecompressorWithProperties() throws Exception {
        // Given
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        Map<String, String> properties = new HashMap<>();
        properties.put("pack.deflate.hint", "false");

        // When
        Pack200Decompressor decompressor = Pack200Decompressor.builder(inputStream)
                .compressorInputStreamBuilder()
                .properties(properties)
                .parentBuilder()
                .build();

        // Then
        assertThat(decompressor).isNotNull();
    }

    @Test
    void shouldHandleNullPropertiesGracefully() {
        // Given
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);

        // When
        Pack200Decompressor.Pack200DecompressorBuilder builder = Pack200Decompressor.builder(inputStream)
                .compressorInputStreamBuilder()
                .properties(null)
                .parentBuilder();

        // Then
        assertThat(builder).isNotNull();
    }
}
