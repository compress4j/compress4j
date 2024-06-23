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
package io.github.compress4j.compressors.bzip2;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BZip2DecompressorBuilderTest {

    private InputStream mockRawInputStream;

    @BeforeEach
    void setUp() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] emptyValidBZip2Data = bos.toByteArray();
        mockRawInputStream = new ByteArrayInputStream(emptyValidBZip2Data);
    }

    @Test
    void shouldBuildInputStream() throws IOException {
        var builder = BZip2Decompressor.builder(mockRawInputStream);

        // when
        try (BZip2CompressorInputStream in = builder.buildCompressorInputStream()) {
            // then
            assertThat(in).isNotNull();
        }
    }

    @Test
    void shouldBuildInputStreamWithDecomporessConcatTrue() throws IOException {
        var builder = BZip2Decompressor.builder(mockRawInputStream);

        // when
        try (BZip2CompressorInputStream in =
                builder.inputStreamBuilder().setDecompressConcatenated(true).buildInputStream()) {
            // then
            assertThat(in).isNotNull();
        }
    }
}
