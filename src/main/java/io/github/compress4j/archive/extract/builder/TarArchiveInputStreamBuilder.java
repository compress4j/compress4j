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
package io.github.compress4j.archive.extract.builder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public class TarArchiveInputStreamBuilder extends ArchiveInputStreamBuilder<TarArchiveInputStream> {
    /**
     * Creates a new {@code TarArchiveInputStreamBuilder} with the given {@code Path}.
     *
     * @param path the {@code Path} to the tar file
     * @throws IOException if an I/O error occurs
     */
    public TarArchiveInputStreamBuilder(Path path) throws IOException {
        this(Files.newInputStream(path));
    }

    /**
     * Create a new TarArchiveInputStreamBuilder.
     *
     * @param inputStream the input stream
     */
    public TarArchiveInputStreamBuilder(InputStream inputStream) {
        super(inputStream);
    }

    /** {@inheritDoc} */
    @Override
    public TarArchiveInputStream build() throws IOException {
        return buildArchiveInputStream(inputStream);
    }

    /**
     * Build a {@code A} from the given {@code InputStream}. If you want to combine an archive format with a compression
     * format - like when reading a `tar.gz` file - you wrap the {@code ArchiveInputStream} around
     * {@code CompressorInputStream} for example:
     *
     * <pre>{@code
     * return new TarArchiveInputStream(new GzipCompressorInputStream(inputStream));
     * }</pre>
     *
     * @param inputStream - the {@code InputStream} to the compressed file
     * @return a {@code A} from the given {@code InputStream}
     */
    protected TarArchiveInputStream buildArchiveInputStream(InputStream inputStream) {
        return new TarArchiveInputStream(inputStream);
    }
}
