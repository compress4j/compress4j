/*
 * Copyright 2024 The Compress4J Project
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
package io.github.compress4j.archive.compression;

import static io.github.compress4j.utils.FileUtils.write;

import io.github.compress4j.archive.decompression.TarGzDecompressor;
import io.github.compress4j.assertion.CompressorAssertion;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TarGzCompressorTest extends TarCompressorTest {

    @Override
    @BeforeEach
    void setup() throws IOException {
        compressFile = tempDir.resolve("test.tar.gz");
        compressor = new TarGzCompressor(compressFile);
    }

    @Test
    void shouldAddFiles(@TempDir Path tempDir) throws IOException {
        var data = tempDir.resolve("file.txt");
        write(data, "789");
        compressor.addFile("empty.txt", new byte[0]);
        compressor.addFile("file1.txt", "123".getBytes());
        compressor.addFile("file2.txt", "456".getBytes());
        compressor.addFile("file3.txt", data);

        CompressorAssertion.assertThat(compressFile)
                .containsAllEntriesOf(
                        Map.of("empty.txt", "", "file1.txt", "123", "file2.txt", "456", "file3.txt", "789"));

        var out = tempDir.resolve("out");
        extract(compressFile, out);
        Assertions.assertThat(out.toFile()).isDirectory().isDirectoryContaining(f -> f.getName()
                .equals("empty.txt"));
    }

    @Override
    protected void extract(Path in, Path out) throws IOException {
        TarGzDecompressor.builder(in).build().extract(out);
    }
}
