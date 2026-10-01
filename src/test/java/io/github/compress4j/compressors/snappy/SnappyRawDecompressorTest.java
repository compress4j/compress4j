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
package io.github.compress4j.compressors.snappy;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SnappyRawDecompressorTest {

    private static final String CONTENT = "Raw snappy decompression test. ".repeat(100);

    @TempDir
    Path tempDir;

    private byte[] compressed() throws IOException {
        var source = Files.writeString(tempDir.resolve("source.txt"), CONTENT, UTF_8);
        var bytes = new ByteArrayOutputStream();
        try (var compressor =
                SnappyRawCompressor.builder(bytes, Files.size(source)).build()) {
            compressor.write(source);
        }
        return bytes.toByteArray();
    }

    private byte[] decompress(byte[] bytes) throws IOException {
        var output = tempDir.resolve("out.bin");
        try (var decompressor =
                SnappyRawDecompressor.builder(new ByteArrayInputStream(bytes)).build()) {
            decompressor.write(output);
        }
        return Files.readAllBytes(output);
    }

    @Test
    void shouldDecompressFromStream() throws IOException {
        assertThat(decompress(compressed())).asString(UTF_8).isEqualTo(CONTENT);
    }

    @Test
    void shouldDecompressFromPath() throws IOException {
        var file = Files.write(tempDir.resolve("data.snappy"), compressed());
        var output = tempDir.resolve("path-out.txt");

        try (var decompressor = SnappyRawDecompressor.builder(file).build()) {
            decompressor.write(output);
        }

        assertThat(output).hasContent(CONTENT);
    }

    @Test
    void shouldExposeInputStreamBuilderParent() {
        var builder = SnappyRawDecompressor.builder(new ByteArrayInputStream(new byte[0]));

        assertThat(builder.compressorInputStreamBuilder().parentBuilder()).isSameAs(builder);
    }

    @Test
    void shouldRejectTruncatedInput() throws IOException {
        var data = compressed();
        var truncated = Arrays.copyOf(data, data.length / 2);

        assertThatThrownBy(() -> decompress(truncated)).isInstanceOf(IOException.class);
    }
}
