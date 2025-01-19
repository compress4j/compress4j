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
package io.github.compress4j.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.compress4j.archive.compression.TarCompressor.TarCompressorBuilder;
import io.github.compress4j.archive.decompression.TarDecompressor;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

class ArchiveFactoryTest {

    @Test
    void shouldCompressorFromPath() {
        // given
        var type = ArchiveType.TAR;
        var outputStream = mock(OutputStream.class);

        // when
        var compressor = ArchiveFactory.compressor(type, outputStream);

        // then
        assertThat(compressor).isInstanceOf(TarCompressorBuilder.class);
    }

    @Test
    void decompressor() {
        // given
        var type = ArchiveType.TAR;

        // when
        var decompressor = ArchiveFactory.decompressor(type);

        // then
        assertThat(decompressor).isInstanceOf(TarDecompressor.class);
    }
}
