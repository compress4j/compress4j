/*
 * Copyright 2025-2026 The Compress4J Project
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

import io.github.compress4j.archivers.tar.TarXzArchiveExtractor.TarXzArchiveExtractorBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.junit.jupiter.api.Test;

class TarXzArchiveInputStreamBuilderTest {

    private static final String ENTRY_NAME = "file.txt";
    private static final String ENTRY_CONTENT = "Hello tar.xz world!";

    private static byte[] buildTarXz() throws IOException {
        var outputStream = new ByteArrayOutputStream();
        try (var xzOut = new XZCompressorOutputStream(outputStream);
                var tarOut = new TarArchiveOutputStream(xzOut)) {
            byte[] content = ENTRY_CONTENT.getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry entry = new TarArchiveEntry(ENTRY_NAME);
            entry.setSize(content.length);
            tarOut.putArchiveEntry(entry);
            tarOut.write(content);
            tarOut.closeArchiveEntry();
        }
        return outputStream.toByteArray();
    }

    @Test
    void shouldBuildArchiveInputStream() throws IOException {
        // Given
        var builder = new TarXzArchiveExtractorBuilder(new ByteArrayInputStream(buildTarXz()));

        // When
        try (var out = builder.buildArchiveInputStream()) {

            // Then
            assertThat(out).isNotNull();
            TarArchiveEntry entry = out.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo(ENTRY_NAME);
            assertThat(out.readAllBytes()).asString(StandardCharsets.UTF_8).isEqualTo(ENTRY_CONTENT);
        }
    }

    @Test
    void shouldBuildArchiveInputStreamWithXzOptions() throws IOException {
        // Given
        var builder = new TarXzArchiveExtractorBuilder(new ByteArrayInputStream(buildTarXz()));
        builder.xzInputStream().setDecompressConcatenated(true).setMemoryLimitInKb(-1);

        // When
        try (var out = builder.buildArchiveInputStream()) {

            // Then
            assertThat(out).isNotNull();
            TarArchiveEntry entry = out.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo(ENTRY_NAME);
        }
    }
}
