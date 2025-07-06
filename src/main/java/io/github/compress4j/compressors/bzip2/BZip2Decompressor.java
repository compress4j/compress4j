/*
 * Copyright 2024-2025 The Compress4J Project
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

import io.github.compress4j.compressors.Decompressor;
import java.io.IOException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * This class provides a BZip2 decompressor that reads from a BZip2CompressorInputStream. It extends the Decompressor
 * class and provides a builder for creating instances.
 */
public class BZip2Decompressor extends Decompressor<BZip2CompressorInputStream> {

    /**
     * Constructor that takes a BZip2CompressorInputStream.
     *
     * @param inputStream the BZip2CompressorInputStream to read from.
     */
    public BZip2Decompressor(BZip2CompressorInputStream inputStream) {
        super(inputStream);
    }

    /**
     * Constructor that takes a BZip2DecompressorBuilder.
     *
     * @param builder the BZip2DecompressorBuilder to build from.
     */
    public BZip2Decompressor(BZip2DecompressorBuilder builder) {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link BZip2DecompressorBuilder}
     *
     * @param inputStream the BZip2CompressorInputStream to read from
     * @return An instance of the {@link BZip2DecompressorBuilder}
     */
    public static BZip2DecompressorBuilder builder(BZip2CompressorInputStream inputStream) {
        return new BZip2DecompressorBuilder(inputStream);
    }

    public static class BZip2DecompressorBuilder
            extends DecompressorBuilder<BZip2CompressorInputStream, BZip2Decompressor, BZip2DecompressorBuilder> {

        /**
         * Constructor that takes a BZip2CompressorInputStream.
         *
         * @param inputStream the BZip2CompressorInputStream to read from.
         */
        public BZip2DecompressorBuilder(BZip2CompressorInputStream inputStream) {
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
