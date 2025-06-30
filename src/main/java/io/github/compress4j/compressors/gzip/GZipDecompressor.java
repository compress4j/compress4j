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

import io.github.compress4j.compressors.Decompressor;
import java.io.IOException;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class GZipDecompressor extends Decompressor<GzipCompressorInputStream> {

    protected <
                    B extends DecompressorBuilder<GzipCompressorInputStream, D, B>,
                    D extends Decompressor<GzipCompressorInputStream>>
            GZipDecompressor(B builder) throws IOException {
        super(builder);
    }

    protected GZipDecompressor(GzipCompressorInputStream compressorInputStream) {
        super(compressorInputStream);
    }

    public static GZipDecompressorBuilder builder(GzipCompressorInputStream inputStream) {
        return new GZipDecompressorBuilder(inputStream);
    }

    public static class GZipDecompressorBuilder
            extends Decompressor.DecompressorBuilder<
                    GzipCompressorInputStream, GZipDecompressor, GZipDecompressorBuilder> {

        protected GZipDecompressorBuilder(GzipCompressorInputStream inputStream) {
            super(inputStream);
        }

        @Override
        protected GZipDecompressor.GZipDecompressorBuilder getThis() {
            return this;
        }

        @Override
        public GZipDecompressor build() throws IOException {
            return new GZipDecompressor(this);
        }
    }
}
