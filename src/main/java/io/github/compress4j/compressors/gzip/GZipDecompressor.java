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

/**
 * This class provides a GZip decompressor that reads from a GzipCompressorInputStream. It extends the Decompressor
 * class and provides a builder for creating instances.
 */
public class GZipDecompressor extends Decompressor<GzipCompressorInputStream> {

    /**
     * Constructor that takes a GZipDecompressorBuilder.
     *
     * @param builder the GZipDecompressorBuilder to build from.
     */
    public GZipDecompressor(GZipDecompressorBuilder builder) {
        super(builder);
    }

    /**
     * Constructor that takes a GzipCompressorInputStream.
     *
     * @param compressorInputStream the GzipCompressorInputStream to read from.
     */
    public GZipDecompressor(GzipCompressorInputStream compressorInputStream) {
        super(compressorInputStream);
    }

    public static GZipDecompressorBuilder builder(GzipCompressorInputStream inputStream) {
        return new GZipDecompressorBuilder(inputStream);
    }

    public static class GZipDecompressorBuilder
            extends Decompressor.DecompressorBuilder<
                    GzipCompressorInputStream, GZipDecompressor, GZipDecompressorBuilder> {

        /**
         * Constructor that takes a GzipCompressorInputStream.
         *
         * @param inputStream the GzipCompressorInputStream to read from.
         */
        public GZipDecompressorBuilder(GzipCompressorInputStream inputStream) {
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
