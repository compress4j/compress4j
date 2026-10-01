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
package io.github.compress4j.compressors.lz4;

import io.github.compress4j.compressors.AbstractCompressorIntegrationTest;
import java.io.IOException;
import java.nio.file.Path;

class Lz4BlockCompressorIntegrationTest extends AbstractCompressorIntegrationTest {

    @Override
    protected Lz4BlockCompressor compressorBuilder(Path compressPath) throws IOException {
        return new Lz4BlockCompressor.Lz4BlockCompressorBuilder(compressPath).build();
    }

    @Override
    protected Lz4BlockDecompressor decompressorBuilder(Path compressPath) throws IOException {
        return new Lz4BlockDecompressor.Lz4BlockDecompressorBuilder(compressPath).build();
    }

    @Override
    protected String compressionExtension() {
        return ".block_lz4";
    }
}
