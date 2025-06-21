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
package io.github.compress4j.compressors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.CompressorInputStream;

public abstract class Decompressor<I extends CompressorInputStream> implements AutoCloseable {

    protected final I compressorInputStream;

    protected Decompressor(I compressorInputStream) {
        this.compressorInputStream = compressorInputStream;

    }

    protected <B extends Decompressor.DecompressorBuilder<I,D,B>, D extends Decompressor<I>> Decompressor(B builder) throws IOException {
        this(builder.compressorInputStream);
    }

    public long write(final File file) throws IOException {
        return write(file.toPath());
    }

    public long write(final Path path) throws IOException {
        return Files.copy(compressorInputStream, path);
    }

    @Override
    public void close() throws IOException {
        compressorInputStream.close();
    }

    public abstract static class DecompressorBuilder<
            I extends CompressorInputStream,
            D extends Decompressor<I>,
            B extends Decompressor.DecompressorBuilder<I, D, B>> {

        protected final I compressorInputStream;

        protected DecompressorBuilder(I compressorInputStream) {
            this.compressorInputStream = compressorInputStream;
        }

        protected abstract B getThis();

        public abstract D build() throws IOException;
    }
}