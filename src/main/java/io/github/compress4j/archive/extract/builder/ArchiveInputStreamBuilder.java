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
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

public abstract class ArchiveInputStreamBuilder<A extends ArchiveInputStream<? extends ArchiveEntry>> {
    protected final InputStream inputStream;

    /**
     * Create a new ArchiveInputStreamBuilder.
     *
     * @param inputStream the input stream
     */
    protected ArchiveInputStreamBuilder(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Build a {@code A} from the given {@code InputStream}. If you want to combine an archive format with a compression
     * format - like when reading a `tar.gz` file - you wrap the {@code ArchiveInputStream} around
     *
     * <p>Use {@link #inputStream} as input output stream from to read the archive from. {@code CompressorInputStream}
     * for example:
     *
     * <pre>{@code
     * return new TarArchiveInputStream(new GzipCompressorInputStream(inputStream));
     * }</pre>
     *
     * @return a {@code A} from the given {@code InputStream}
     * @throws IOException - if the {@code A} could not be created
     */
    public abstract A build() throws IOException;
}
