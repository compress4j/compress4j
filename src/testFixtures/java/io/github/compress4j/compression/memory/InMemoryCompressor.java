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
package io.github.compress4j.compression.memory;

import io.github.compress4j.compression.Compressor;
import java.io.IOException;
import java.io.OutputStream;

public class InMemoryCompressor extends Compressor<InMemoryCompressorOutputStream> {
    public InMemoryCompressor(InMemoryCompressorOutputStream compressorOutputStream) {
        super(compressorOutputStream);
    }

    protected InMemoryCompressor(InMemoryCompressorBuilder builder) throws IOException {
        super(builder);
    }

    public static InMemoryCompressorBuilder builder(OutputStream outputStream) {
        return new InMemoryCompressorBuilder(outputStream);
    }

    public static class InMemoryCompressorBuilder
            extends CompressorBuilder<InMemoryCompressorOutputStream, InMemoryCompressorBuilder, InMemoryCompressor> {

        /**
         * Create a new {@link CompressorBuilder} with the given output stream.
         *
         * @param outputStream the output stream
         */
        protected InMemoryCompressorBuilder(OutputStream outputStream) {
            super(outputStream);
        }

        @Override
        protected InMemoryCompressorBuilder getThis() {
            return this;
        }

        @Override
        public InMemoryCompressorOutputStream buildCompressorOutputStream() {
            return new InMemoryCompressorOutputStream(outputStream);
        }

        @Override
        public InMemoryCompressor build() throws IOException {
            return new InMemoryCompressor(this);
        }
    }
}
