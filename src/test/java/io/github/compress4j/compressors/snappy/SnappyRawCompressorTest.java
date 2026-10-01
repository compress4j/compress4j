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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SnappyRawCompressorTest {

    private static final String CONTENT = "Raw snappy test data. ".repeat(100);

    @TempDir
    Path tempDir;

    @Test
    void shouldRoundTripThroughRawDecompressor() throws IOException {
        var source = Files.writeString(tempDir.resolve("source.txt"), CONTENT, UTF_8);
        var compressed = tempDir.resolve("data.snappy");
        var output = tempDir.resolve("out.txt");

        try (var compressor =
                SnappyRawCompressor.builder(compressed, Files.size(source)).build()) {
            compressor.write(source);
        }
        try (var decompressor = SnappyRawDecompressor.builder(compressed).build()) {
            decompressor.write(output);
        }

        assertThat(Files.size(compressed)).isLessThan(Files.size(source));
        assertThat(output).hasContent(CONTENT);
    }

    @Test
    void shouldBuildWithOutputStream() throws IOException {
        var source = Files.writeString(tempDir.resolve("source.txt"), "x", UTF_8);
        var bytes = new ByteArrayOutputStream();

        try (var compressor = SnappyRawCompressor.builder(bytes, 1).build()) {
            compressor.write(source);
        }

        assertThat(bytes.size()).isPositive();
    }

    @Test
    void shouldRejectDirectoryPath() {
        assertThatThrownBy(() -> SnappyRawCompressor.builder(tempDir, 1)).isInstanceOf(IOException.class);
    }

    @Test
    void shouldRejectNegativeUncompressedSize() {
        var out = new ByteArrayOutputStream();

        assertThatThrownBy(() -> SnappyRawCompressor.builder(out, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("but was: -1");
    }

    @Test
    void shouldRejectNegativeUncompressedSizeFromPathWithoutCreatingTheFile() {
        var target = tempDir.resolve("x.snappy");

        assertThatThrownBy(() -> SnappyRawCompressor.builder(target, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(target).doesNotExist();
    }
}
