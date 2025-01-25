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

import io.github.compress4j.archive.decompression.builder.TarArchiveInputStreamBuilder;
import java.io.IOException;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/** Tar Decompressor */
public class TarDecompressor extends TarBaseDecompressor {

    /**
     * Creates a new {@code TarDecompressor}
     *
     * @param tarArchiveInputStream - the {@code TarArchiveInputStream} to the tar file
     */
    public TarDecompressor(TarArchiveInputStream tarArchiveInputStream) {
        super(tarArchiveInputStream);
    }

    /**
     * Creates a new {@code TarBaseDecompressor}.
     *
     * @param builder - the {@code ArchiveInputStreamBuilder} to build the {@code TarArchiveInputStream}.
     * @throws IOException - if the {@code TarArchiveInputStream} could not be created
     */
    public TarDecompressor(TarArchiveInputStreamBuilder builder) throws IOException {
        super(builder);
    }
}
