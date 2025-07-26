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
package io.github.compress4j.compressors;

import io.github.compress4j.compressors.bzip2.BZip2Compressor;
import io.github.compress4j.compressors.bzip2.BZip2Decompressor;
import java.io.IOException;
import java.nio.file.Path;

class BZip2Test extends AbstractTest {

    @Override
    protected Compressor<?> compressorBuilder(Path compressPath) throws IOException {
        return new BZip2Compressor.BZip2CompressorBuilder(compressPath).build();
    }

    @Override
    protected Decompressor<?> decompressorBuilder(Path compressPath) throws IOException {
        return new BZip2Decompressor.BZip2DecompressorBuilder(compressPath).build();
    }

    @Override
    protected String compressionExtension() {
        return ".bz";
    }
}
