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

import static io.github.compress4j.archive.decompression.Decompressor.Entry.Type.FILE;
import static io.github.compress4j.archive.decompression.Decompressor.Entry.Type.SYMLINK;

import io.github.compress4j.archive.compression.Compressor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;

public class InMemoryCompressor extends Compressor<InMemoryArchiveOutputStream> {

    @SuppressWarnings("unused")
    public InMemoryCompressor(InMemoryArchiveOutputStream outputStream) {
        super(outputStream);
    }

    public InMemoryCompressor(InMemoryCompressorBuilder outputStreamBuilder) throws IOException {
        super(outputStreamBuilder);
    }

    @Override
    public void writeDirectoryEntry(String name, FileTime modTime) throws IOException {
        archiveOutputStream.putArchiveEntry(InMemoryArchiveEntry.builder()
                .name(name)
                .lastModifiedDate(modTime)
                .build());
    }

    @Override
    public void writeFileEntry(
            String name, InputStream source, long length, FileTime modTime, int mode, Optional<Path> symlinkTarget)
            throws IOException {
        InMemoryArchiveEntry.Builder builder =
                InMemoryArchiveEntry.builder().name(name).lastModifiedDate(modTime);
        if (symlinkTarget.isPresent()) {
            builder.type(SYMLINK).linkName(symlinkTarget.get().toString());
        } else {
            builder.type(FILE).content(new String(source.readAllBytes()));
        }
        archiveOutputStream.putArchiveEntry(builder.build());
    }

    public static class InMemoryCompressorBuilder
            extends CompressorBuilder<InMemoryArchiveOutputStream, InMemoryCompressorBuilder, InMemoryCompressor> {
        private int someOption = 0;

        public InMemoryCompressorBuilder(OutputStream outputStream) {
            super(outputStream);
        }

        @Override
        protected InMemoryCompressorBuilder getThis() {
            return this;
        }

        public InMemoryCompressorBuilder withSomeOption(int option) {
            someOption = option;
            return this;
        }

        @Override
        public InMemoryArchiveOutputStream buildArchiveOutputStream() {
            InMemoryArchiveOutputStream out = new InMemoryArchiveOutputStream(outputStream);
            out.setSomeOption(someOption);
            return out;
        }

        @Override
        public InMemoryCompressor build() throws IOException {
            return new InMemoryCompressor(this);
        }
    }
}
