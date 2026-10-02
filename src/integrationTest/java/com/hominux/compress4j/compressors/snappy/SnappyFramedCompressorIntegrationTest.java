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
package com.hominux.compress4j.compressors.snappy;

import com.hominux.compress4j.compressors.AbstractCompressorIntegrationTest;
import java.io.IOException;
import java.nio.file.Path;

class SnappyFramedCompressorIntegrationTest extends AbstractCompressorIntegrationTest {

    @Override
    protected SnappyFramedCompressor compressorBuilder(Path compressPath) throws IOException {
        return new SnappyFramedCompressor.SnappyFramedCompressorBuilder(compressPath).build();
    }

    @Override
    protected SnappyFramedDecompressor decompressorBuilder(Path compressPath) throws IOException {
        return new SnappyFramedDecompressor.SnappyFramedDecompressorBuilder(compressPath).build();
    }

    @Override
    protected String compressionExtension() {
        return ".sz";
    }
}
