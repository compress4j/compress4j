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
public class TarDecompressor extends TarBaseDecompressor {

    /**
     * Creates a new {@code TarDecompressor} with the given {@code Builder}
     *
     * @param path the {@code Path} to the tar file
     * @throws IOException if an I/O error occurs
     */
    public TarDecompressor(Path path) throws IOException {
        super(path);
    }

    /**
     * Creates a new {@code TarDecompressor}
     *
     * @param inputStream - the {@code InputStream} to the tar file
     * @throws IOException - if the {@code A} could not be created
     */
    public TarDecompressor(InputStream inputStream) throws IOException {
        super(inputStream);
    }

    /**
     * {@inheritDoc}
     *
     * @param inputStream {@inheritDoc}
     * @return {@inheritDoc}
     */
    protected TarArchiveInputStream buildArchiveInputStream(InputStream inputStream) {
        return new TarArchiveInputStream(inputStream);
    }
}