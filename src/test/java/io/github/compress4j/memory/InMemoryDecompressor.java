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
package io.github.compress4j.memory;

import io.github.compress4j.archive.decompression.Decompressor;
import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class InMemoryDecompressor extends Decompressor<InMemoryArchiveInputStream> {
    public InMemoryDecompressor(final List<InMemoryArchiveEntry> entries) throws IOException {
        super(InMemoryArchiveInputStream.toInputStream(entries));
    }

    @Override
    protected InMemoryArchiveInputStream buildArchiveInputStream(InputStream inputStream) throws IOException {
        return new InMemoryArchiveInputStream(inputStream);
    }

    @Override
    protected void closeEntryStream(InputStream stream) {
        // do nothing
    }

    @Nullable
    @Override
    protected Entry nextEntry() {
        InMemoryArchiveEntry nextEntry = archiveInputStream.getNextEntry();
        if (nextEntry == null) {
            return null;
        }
        return new Entry(
                nextEntry.getName(),
                nextEntry.getType(),
                nextEntry.getMode(),
                nextEntry.getLinkName(),
                nextEntry.getSize());
    }

    @Override
    protected InputStream openEntryStream(Entry entry) {
        return new ByteArrayInputStream(archiveInputStream.readString().getBytes(StandardCharsets.UTF_8));
    }
}