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
package io.github.compress4j.archivers.tar;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.compress4j.archivers.tar.TarLz4ArchiveExtractor.TarLz4ArchiveExtractorBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.junit.jupiter.api.Test;

class TarLz4ArchiveInputStreamBuilderTest {

    static byte[] tarLz4(String name, String content) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var tar = new TarArchiveOutputStream(new FramedLZ4CompressorOutputStream(bytes))) {
            var data = content.getBytes(StandardCharsets.UTF_8);
            var entry = new TarArchiveEntry(name);
            entry.setSize(data.length);
            tar.putArchiveEntry(entry);
            tar.write(data);
            tar.closeArchiveEntry();
        }
        return bytes.toByteArray();
    }

    @Test
    void shouldBuildArchiveInputStream() throws IOException {
        var builder = new TarLz4ArchiveExtractorBuilder(new ByteArrayInputStream(tarLz4("file.txt", "hello")));

        try (var in = builder.buildArchiveInputStream()) {
            assertThat(in.getNextEntry().getName()).isEqualTo("file.txt");
            assertThat(in.readAllBytes()).asString(StandardCharsets.UTF_8).isEqualTo("hello");
        }
    }

    @Test
    void shouldExposeLz4InputStreamBuilder() {
        var builder = new TarLz4ArchiveExtractorBuilder(new ByteArrayInputStream(new byte[0]));

        assertThat(builder.lz4InputStream().parentBuilder()).isSameAs(builder);
    }
}
