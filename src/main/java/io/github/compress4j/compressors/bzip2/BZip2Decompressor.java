/*
 * Copyright 2024-2025 The Compress4J Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.compress4j.compressors.bzip2;

import io.github.compress4j.compressors.Decompressor;
import java.io.IOException;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class BZip2Decompressor extends Decompressor<BZip2CompressorInputStream> {


    protected BZip2Decompressor(BZip2CompressorInputStream inputStream) {
        super(inputStream);
    }

    protected BZip2Decompressor(BZip2DecompressorBuilder builder) throws IOException {
        super(builder);
    }

    public static BZip2DecompressorBuilder builder(BZip2CompressorInputStream inputStream) {
        return new BZip2DecompressorBuilder(inputStream);
    }

    public static class BZip2DecompressorBuilder
            extends Decompressor.DecompressorBuilder<
            BZip2CompressorInputStream,
            BZip2Decompressor,
            BZip2DecompressorBuilder
            > {

        protected BZip2DecompressorBuilder(BZip2CompressorInputStream inputStream) {
            super(inputStream);
        }

        @Override
        protected BZip2DecompressorBuilder getThis() {
            return this;
        }

        @Override
        public BZip2Decompressor build() throws IOException {
            return new BZip2Decompressor(this);
        }
    }
}
