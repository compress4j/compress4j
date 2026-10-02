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
package com.hominux.compress4j.compressors.deflate64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hominux.compress4j.UpstreamSamples;
import com.hominux.compress4j.compressors.deflate64.Deflate64Decompressor.Deflate64DecompressorBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Deflate64DecompressorIntegrationTest {

    private static final int UNCOMPRESSED_LENGTH = 144060;

    @TempDir
    Path tempDir;

    private Path sample() throws IOException {
        return UpstreamSamples.copy("/compression/lorem.deflate64", tempDir);
    }

    private byte[] decompress(byte[] bytes) throws IOException {
        var output = tempDir.resolve(UUID.randomUUID() + ".bin");
        try (var decompressor =
                Deflate64Decompressor.builder(new ByteArrayInputStream(bytes)).build()) {
            decompressor.write(output);
        }
        return Files.readAllBytes(output);
    }

    @Test
    void shouldDecompressUpstreamSampleFromPath() throws IOException {
        var output = tempDir.resolve("lorem.txt");

        try (var decompressor = Deflate64Decompressor.builder(sample()).build()) {
            decompressor.write(output);
        }

        assertThat(output).hasSize(UNCOMPRESSED_LENGTH);
        assertThat(Files.readString(output, StandardCharsets.UTF_8)).contains("Lorem ipsum dolor sit amet");
    }

    @Test
    void shouldDecompressUpstreamSampleFromFile() throws IOException {
        var output = tempDir.resolve("lorem-file.txt");

        try (var decompressor = new Deflate64DecompressorBuilder(sample().toFile()).build()) {
            decompressor.write(output);
        }

        assertThat(output).hasSize(UNCOMPRESSED_LENGTH);
    }

    @Test
    void shouldDecompressUpstreamSampleFromStream() throws IOException {
        assertThat(decompress(Files.readAllBytes(sample()))).hasSize(UNCOMPRESSED_LENGTH);
    }

    @Test
    void shouldExposeInputStreamBuilderParent() {
        var builder = Deflate64Decompressor.builder(new ByteArrayInputStream(new byte[0]));

        assertThat(builder.compressorInputStreamBuilder().parentBuilder()).isSameAs(builder);
    }

    @Test
    void shouldRejectNonFormatInput() throws IOException {
        var gzip = new ByteArrayOutputStream();
        try (var out = new GZIPOutputStream(gzip)) {
            out.write("not deflate64".getBytes(StandardCharsets.UTF_8));
        }
        var bytes = gzip.toByteArray();

        assertThatThrownBy(() -> decompress(bytes)).isInstanceOf(IOException.class);
    }

    @Test
    void shouldRejectTruncatedInput() throws IOException {
        var bytes = Files.readAllBytes(sample());
        var truncated = Arrays.copyOf(bytes, bytes.length / 2);

        assertThatThrownBy(() -> decompress(truncated)).isInstanceOf(IOException.class);
    }
}
