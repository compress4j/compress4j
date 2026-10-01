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
package io.github.compress4j.compressors.z;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.compress4j.UpstreamSamples;
import io.github.compress4j.compressors.z.ZDecompressor.ZDecompressorBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZDecompressorIntegrationTest {

    @TempDir
    Path tempDir;

    private Path sample() throws IOException {
        return UpstreamSamples.copy("/archives/upstream-bla.tar.Z", tempDir);
    }

    private static List<String> entryNames(Path tar) throws IOException {
        var names = new ArrayList<String>();
        try (var in = new TarArchiveInputStream(Files.newInputStream(tar))) {
            for (var entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private byte[] decompress(byte[] bytes) throws IOException {
        var output = tempDir.resolve(UUID.randomUUID() + ".bin");
        try (var decompressor =
                ZDecompressor.builder(new ByteArrayInputStream(bytes)).build()) {
            decompressor.write(output);
        }
        return Files.readAllBytes(output);
    }

    @Test
    void shouldDecompressUpstreamSampleFromPath() throws IOException {
        var tar = tempDir.resolve("bla.tar");

        try (var decompressor = ZDecompressor.builder(sample()).build()) {
            decompressor.write(tar);
        }

        assertThat(entryNames(tar)).containsExactly("test1.xml", "test2.xml");
    }

    @Test
    void shouldDecompressUpstreamSampleFromFile() throws IOException {
        var tar = tempDir.resolve("bla-file.tar");

        try (var decompressor = new ZDecompressorBuilder(sample().toFile()).build()) {
            decompressor.write(tar);
        }

        assertThat(entryNames(tar)).containsExactly("test1.xml", "test2.xml");
    }

    @Test
    void shouldDecompressUpstreamSampleFromStream() throws IOException {
        var tar = tempDir.resolve("bla-stream.tar");

        try (var in = Files.newInputStream(sample());
                var decompressor = ZDecompressor.builder(in).build()) {
            decompressor.write(tar);
        }

        assertThat(entryNames(tar)).containsExactly("test1.xml", "test2.xml");
    }

    @Test
    void shouldExposeInputStreamBuilderParent() {
        var builder = ZDecompressor.builder(new ByteArrayInputStream(new byte[0]));

        assertThat(builder.compressorInputStreamBuilder().parentBuilder()).isSameAs(builder);
    }

    @Test
    void shouldRejectNonFormatInput() throws IOException {
        var gzip = new ByteArrayOutputStream();
        try (var out = new GZIPOutputStream(gzip)) {
            out.write("not compress".getBytes(StandardCharsets.UTF_8));
        }
        var bytes = gzip.toByteArray();

        assertThatThrownBy(() -> decompress(bytes)).isInstanceOf(IOException.class);
    }

    @Test
    void shouldReturnShorterOutputForTruncatedInputBecauseTheFormatHasNoEndMarker() throws IOException {
        var bytes = Files.readAllBytes(sample());
        var truncated = Arrays.copyOf(bytes, bytes.length / 2);

        assertThat(decompress(truncated)).hasSizeLessThan(decompress(bytes).length);
    }
}
