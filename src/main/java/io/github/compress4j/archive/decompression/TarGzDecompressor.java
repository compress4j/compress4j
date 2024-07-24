/*
 * Copyright 2024 The Compress4J Project
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
package io.github.compress4j.archive.decompression;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class TarGzDecompressor extends TarBaseDecompressor<TarGzDecompressor, TarGzDecompressor.Builder> {

    protected TarGzDecompressor(Builder builder) {
        super(builder);
    }

    /** {@inheritDoc} */
    @Override
    protected TarArchiveInputStream buildArchiveInputStream(InputStream input) throws IOException {
        return new TarArchiveInputStream(new GzipCompressorInputStream(input));
    }

    /**
     * Creates a new {@code TarDecompressor.Builder} with the given {@code Path}
     *
     * @param path the {@code Path} to the tar file
     */
    public static Builder builder(Path path) throws IOException {
        return new Builder(path);
    }

    /**
     * Creates a new {@code TarDecompressor.Builder} with the given {@code InputStream}
     *
     * @param stream the {@code InputStream} to the tar file
     */
    public static Builder builder(InputStream stream) {
        return new Builder(stream);
    }

    public static final class Builder
            extends TarBaseDecompressor.Builder<TarGzDecompressor, TarGzDecompressor.Builder> {

        public Builder(Path path) throws IOException {
            super(path);
        }

        public Builder(InputStream stream) {
            super(stream);
        }

        /**
         * Builds the TarDecompressor.
         *
         * @return A TarDecompressor, populated with all fields from this builder.
         */
        @Override
        public TarGzDecompressor build() {
            return new TarGzDecompressor(this);
        }
    }
}
