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
package io.github.compress4j.archive.decompression;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/** Tar Decompressor */
@SuppressWarnings("unused")
public class TarDecompressor extends TarBaseDecompressor<TarDecompressor, TarDecompressor.Builder> {

    /**
     * Creates a new {@code TarDecompressor} with the given {@code Builder}
     *
     * @param builder the {@code Builder} to build the {@code TarDecompressor}
     */
    protected TarDecompressor(Builder builder) {
        super(builder);
    }

    /**
     * Creates a new {@code TarDecompressor.Builder} with the given {@code Path}
     *
     * @param path the {@code Path} to the tar file
     * @throws IOException if an I/O error occurs
     * @return a new {@code TarDecompressor.Builder}
     */
    public static Builder builder(Path path) throws IOException {
        return new Builder(path);
    }

    /**
     * Creates a new {@code TarDecompressor.Builder} with the given {@code InputStream}
     *
     * @param stream the {@code InputStream} to the tar file
     * @return a new {@code TarDecompressor.Builder}
     */
    public static Builder builder(InputStream stream) {
        return new Builder(stream);
    }

    /**
     * {@inheritDoc}
     *
     * @param input {@inheritDoc}
     * @return {@inheritDoc}
     */
    protected TarArchiveInputStream buildArchiveInputStream(InputStream input) {
        return new TarArchiveInputStream(input);
    }

    /** A builder for {@code TarDecompressor} */
    public static final class Builder extends TarBaseDecompressor.Builder<TarDecompressor, Builder> {

        /**
         * Creates a new {@code TarDecompressor.Builder} with the given {@code Path}
         *
         * @param path the {@code Path} to the tar file
         * @throws IOException if an I/O error occurs
         */
        public Builder(Path path) throws IOException {
            super(path);
        }

        /**
         * Creates a new {@code TarDecompressor.Builder} with the given {@code InputStream}
         *
         * @param stream the {@code InputStream} to the tar file
         */
        public Builder(InputStream stream) {
            super(stream);
        }

        /**
         * Builds the TarDecompressor.
         *
         * @return A TarDecompressor, populated with all fields from this builder.
         */
        @Override
        public TarDecompressor build() {
            return new TarDecompressor(this);
        }
    }
}
