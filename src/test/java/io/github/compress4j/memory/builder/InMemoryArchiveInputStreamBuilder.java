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
package io.github.compress4j.memory.builder;

import io.github.compress4j.archive.decompression.builder.ArchiveInputStreamBuilder;
import io.github.compress4j.memory.InMemoryArchiveEntry;
import io.github.compress4j.memory.InMemoryArchiveInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class InMemoryArchiveInputStreamBuilder extends ArchiveInputStreamBuilder<InMemoryArchiveInputStream> {
    /**
     * Creates a new {@code InMemoryArchiveInputStreamBuilder}.
     *
     * @param entries the {@code List} of {@code InMemoryArchiveEntry}
     * @throws IOException if an I/O error occurs
     */
    public InMemoryArchiveInputStreamBuilder(final List<InMemoryArchiveEntry> entries) throws IOException {
        this(InMemoryArchiveInputStream.toInputStream(entries));
    }

    /**
     * Create a new ArchiveInputStreamBuilder.
     *
     * @param inputStream the input stream
     */
    public InMemoryArchiveInputStreamBuilder(InputStream inputStream) {
        super(inputStream);
    }

    @Override
    public InMemoryArchiveInputStream build() throws IOException {
        return new InMemoryArchiveInputStream(inputStream);
    }
}
