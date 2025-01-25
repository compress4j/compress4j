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
package io.github.compress4j.archive.decompression;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.compress4j.archive.decompression.builder.TarArchiveInputStreamBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class TarDecompressorTest {

    @Test
    void shouldNotThrowExceptionWhenNextEntryIsNull() throws IOException {
        // given
        var mockInputStream = new ByteArrayInputStream("test".getBytes());
        var builder = new TarArchiveInputStreamBuilder(mockInputStream);

        try (TarDecompressor tarDecompressor = new TarDecompressor(builder)) {
            // when
            var result = tarDecompressor.nextEntry();

            // then
            assertThat(result).isNull();
        }
    }

    @Test
    void shouldNotThrowExceptionWhenNextEntryIsHardlink() throws IOException {
        // given
        var mockInputStream = new ByteArrayInputStream("test".getBytes());
        var tarArchiveInputStream = new TarArchiveInputStreamBuilder(mockInputStream).build();

        try (TarDecompressor tarDecompressor = new TarDecompressor(tarArchiveInputStream)) {
            // when
            var result = tarDecompressor.nextEntry();

            // then
            assertThat(result).isNull();
        }
    }
}
