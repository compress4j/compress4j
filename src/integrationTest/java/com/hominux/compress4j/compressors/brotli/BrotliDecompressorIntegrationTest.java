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
package com.hominux.compress4j.compressors.brotli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hominux.compress4j.UpstreamSamples;
import com.hominux.compress4j.archivers.tar.TarArchiveExtractor;
import com.hominux.compress4j.compressors.brotli.BrotliDecompressor.BrotliDecompressorBuilder;
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

class BrotliDecompressorIntegrationTest {

    @TempDir
    Path tempDir;

    private Path sample() throws IOException {
        return UpstreamSamples.copy("/compression/upstream-brotli.testdata.compressed", tempDir);
    }

    private byte[] expected() throws IOException {
        return Files.readAllBytes(UpstreamSamples.copy("/compression/upstream-brotli.testdata.uncompressed", tempDir));
    }

    private byte[] decompress(byte[] bytes) throws IOException {
        var output = tempDir.resolve(UUID.randomUUID() + ".bin");
        try (var decompressor =
                BrotliDecompressor.builder(new ByteArrayInputStream(bytes)).build()) {
            decompressor.write(output);
        }
        return Files.readAllBytes(output);
    }

    @Test
    void shouldDecompressUpstreamSampleFromPath() throws IOException {
        var output = tempDir.resolve("out.txt");

        try (var decompressor = BrotliDecompressor.builder(sample()).build()) {
            decompressor.write(output);
        }

        assertThat(Files.readAllBytes(output)).isEqualTo(expected());
    }

    @Test
    void shouldDecompressUpstreamSampleFromFile() throws IOException {
        var output = tempDir.resolve("out-file.txt");

        try (var decompressor = new BrotliDecompressorBuilder(sample().toFile()).build()) {
            decompressor.write(output);
        }

        assertThat(Files.readAllBytes(output)).isEqualTo(expected());
    }

    @Test
    void shouldDecompressUpstreamSampleFromStream() throws IOException {
        assertThat(decompress(Files.readAllBytes(sample()))).isEqualTo(expected());
    }

    @Test
    void shouldExtractUpstreamTarSample() throws IOException {
        var tarBr = UpstreamSamples.copy("/archives/upstream-bla.tar.br", tempDir);
        var tar = tempDir.resolve("bla.tar");
        try (var decompressor = BrotliDecompressor.builder(tarBr).build()) {
            decompressor.write(tar);
        }
        var out = Files.createDirectory(tempDir.resolve("out"));

        try (var extractor = TarArchiveExtractor.builder(tar).build()) {
            extractor.extract(out);
        }

        assertThat(out.resolve("test1.xml")).isNotEmptyFile();
        assertThat(out.resolve("test2.xml")).isNotEmptyFile();
    }

    @Test
    void shouldExposeInputStreamBuilderParent() {
        var builder = BrotliDecompressor.builder(new ByteArrayInputStream(new byte[0]));

        assertThat(builder.compressorInputStreamBuilder().parentBuilder()).isSameAs(builder);
    }

    @Test
    void shouldRejectNonFormatInput() throws IOException {
        var gzip = new ByteArrayOutputStream();
        try (var out = new GZIPOutputStream(gzip)) {
            out.write("not brotli, long enough to be rejected".getBytes(StandardCharsets.UTF_8));
        }
        var bytes = gzip.toByteArray();

        assertThatThrownBy(() -> decompress(bytes)).isInstanceOf(IOException.class);
    }

    @Test
    void shouldRejectTruncatedInput() throws IOException {
        var bytes = Files.readAllBytes(UpstreamSamples.copy("/archives/upstream-bla.tar.br", tempDir));
        var truncated = Arrays.copyOf(bytes, bytes.length / 2);

        assertThatThrownBy(() -> decompress(truncated)).isInstanceOf(IOException.class);
    }
}
