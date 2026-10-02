/*
 * Copyright 2025-2026 The Compress4J Project
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
package com.hominux.compress4j.archivers.memory;

import static com.hominux.compress4j.archivers.ArchiveExtractor.Entry.Type.FILE;
import static com.hominux.compress4j.archivers.ArchiveExtractor.Entry.Type.SYMLINK;

import com.hominux.compress4j.archivers.ArchiveCreator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class InMemoryArchiveCreator extends ArchiveCreator<InMemoryArchiveOutputStream> {

    @SuppressWarnings("unused")
    public InMemoryArchiveCreator(InMemoryArchiveOutputStream outputStream) {
        super(outputStream);
    }

    public InMemoryArchiveCreator(InMemoryArchiveCreatorBuilder outputStreamBuilder) throws IOException {
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
    public void writeFileEntry(String name, InputStream source, long length, FileTime modTime, int mode)
            throws IOException {
        archiveOutputStream.putArchiveEntry(InMemoryArchiveEntry.builder()
                .name(name)
                .lastModifiedDate(modTime)
                .type(FILE)
                .content(new String(source.readAllBytes()))
                .build());
    }

    @Override
    public void writeFileEntry(
            String name, InputStream source, long length, FileTime modTime, int mode, Path symlinkTarget)
            throws IOException {
        archiveOutputStream.putArchiveEntry(InMemoryArchiveEntry.builder()
                .name(name)
                .lastModifiedDate(modTime)
                .type(SYMLINK)
                .linkName(symlinkTarget.toString())
                .build());
    }

    public static class InMemoryArchiveCreatorBuilder
            extends ArchiveCreatorBuilder<
                    InMemoryArchiveOutputStream, InMemoryArchiveCreatorBuilder, InMemoryArchiveCreator> {
        private int someOption = 0;

        public InMemoryArchiveCreatorBuilder(OutputStream outputStream) {
            super(outputStream);
        }

        public InMemoryArchiveCreatorBuilder(OutputStream outputStream, boolean owned) {
            super(outputStream, owned);
        }

        @Override
        protected InMemoryArchiveCreatorBuilder getThis() {
            return this;
        }

        public InMemoryArchiveCreatorBuilder withSomeOption(int option) {
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
        public InMemoryArchiveCreator build() throws IOException {
            return new InMemoryArchiveCreator(this);
        }
    }
}
