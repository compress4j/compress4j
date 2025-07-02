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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class BZip2CompressorTest {

    @Test
    void shouldWritePathEntry() throws Exception {
        // given
        var outputStream = mock(OutputStream.class);

        final Path tempSourceFile1 = mock(Path.class);

        // when
        var aOut = spy(BZip2Compressor.builder(outputStream).buildCompressorOutputStream());
        try (MockedStatic<Files> mockFiles = mockStatic(Files.class);
                BZip2Compressor compressor = new BZip2Compressor(aOut)) {

            compressor.write(tempSourceFile1);

            // then
            mockFiles.verify(() -> Files.copy(any(Path.class), any(OutputStream.class)));
        }
    }
}
