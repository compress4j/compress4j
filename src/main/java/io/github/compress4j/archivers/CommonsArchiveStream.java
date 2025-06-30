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
package io.github.compress4j.archivers;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import org.apache.commons.compress.archivers.ArchiveInputStream;

/** {@link ArchiveStream} implementation that wraps a commons compress {@link ArchiveInputStream}. */
class CommonsArchiveStream<E extends org.apache.commons.compress.archivers.ArchiveEntry> extends ArchiveStream {

    private final ArchiveInputStream<E> stream;

    CommonsArchiveStream(ArchiveInputStream<E> stream) {
        this.stream = stream;
    }

    @Override
    protected ArchiveEntry createNextEntry() throws IOException {
        org.apache.commons.compress.archivers.ArchiveEntry next = stream.getNextEntry();

        return (next == null) ? null : new CommonsArchiveEntry(this, next);
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public int read(@Nonnull byte[] b) throws IOException {
        return stream.read(b);
    }

    @Override
    public int read(@Nonnull byte[] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        super.close();
        stream.close();
    }
}
