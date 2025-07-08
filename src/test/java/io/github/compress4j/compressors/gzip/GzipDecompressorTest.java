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
package io.github.compress4j.compressors.gzip;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class GzipDecompressorTest {
    @Test
    void shouldWritePathEntry() throws Exception {
        // given
        var inputStream = mock(GzipCompressorInputStream.class);

        Path path = mock(Path.class);

        // when

        var aIn = GzipDecompressor.builder(inputStream);
        try (MockedStatic<Files> mockFiles = mockStatic(Files.class);
                GzipDecompressor compressor = new GzipDecompressor(aIn)) {
            compressor.write(path);

            // then
            mockFiles.verify(() -> Files.copy(any(GzipCompressorInputStream.class), any(Path.class)));
        }
    }
}
