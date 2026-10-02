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
package com.hominux.compress4j.compressors.lzma;

import com.hominux.compress4j.compressors.AbstractCompressorIntegrationTest;
import java.io.IOException;
import java.nio.file.Path;

class LZMACompressorIntegrationTest extends AbstractCompressorIntegrationTest {

    @Override
    protected LZMACompressor compressorBuilder(Path compressPath) throws IOException {
        return new LZMACompressor.LZMACompressorBuilder(compressPath).build();
    }

    @Override
    protected LZMADecompressor decompressorBuilder(Path compressPath) throws IOException {
        return new LZMADecompressor.LZMADecompressorBuilder(compressPath).build();
    }

    @Override
    protected String compressionExtension() {
        return ".lzma";
    }
}
