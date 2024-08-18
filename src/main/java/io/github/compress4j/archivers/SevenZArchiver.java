/*
 * Copyright 2024 The Compress4J Project
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

import io.github.compress4j.utils.ArchiverDependencyChecker;
import io.github.compress4j.utils.ExcludeFromJacocoGeneratedReport;
import jakarta.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

/**
 * Archiver to handle 7z archives. commons-compress does not handle 7z over ArchiveStreams, so we need this custom
 * implementation. <br>
 * Basically this could disperse by adapting the CommonsStreamFactory, but this seemed more convenient as we also have
 * both Input and Output stream wrappers capsuled here.
 */
class SevenZArchiver extends CommonsArchiver<SevenZArchiveEntry> {

    public SevenZArchiver() {
        super(ArchiveFormat.SEVEN_Z);
    }

    @Override
    protected ArchiveOutputStream<SevenZArchiveEntry> createArchiveOutputStream(File archive) throws IOException {
        ArchiverDependencyChecker.checkLZMA();
        return new SevenZOutputStream(new SevenZOutputFile(archive));
    }

    @Override
    protected ArchiveInputStream<SevenZArchiveEntry> createArchiveInputStream(File archive) throws IOException {
        ArchiverDependencyChecker.checkLZMA();
        return new SevenZInputStream(SevenZFile.builder().setFile(archive).get());
    }

    /** Wraps a SevenZFile to make it usable as an ArchiveInputStream. */
    static class SevenZInputStream extends ArchiveInputStream<SevenZArchiveEntry> {

        private final SevenZFile file;

        public SevenZInputStream(SevenZFile file) {
            this.file = file;
        }

        @Override
        public int read(@Nonnull byte[] b, int off, int len) throws IOException {
            return file.read(b, off, len);
        }

        @Override
        public SevenZArchiveEntry getNextEntry() throws IOException {
            return file.getNextEntry();
        }

        @Override
        public void close() throws IOException {
            file.close();
        }
    }

    /** Wraps a SevenZOutputFile to make it usable as an ArchiveOutputStream. */
    static class SevenZOutputStream extends ArchiveOutputStream<SevenZArchiveEntry> {

        private final SevenZOutputFile file;

        public SevenZOutputStream(SevenZOutputFile file) {
            this.file = file;
        }

        @Override
        public void flush() throws IOException {
            file.close();
        }

        @Override
        public void putArchiveEntry(SevenZArchiveEntry entry) {
            file.putArchiveEntry(entry);
        }

        @Override
        public void closeArchiveEntry() throws IOException {
            file.closeArchiveEntry();
        }

        @ExcludeFromJacocoGeneratedReport
        @Override
        public void finish() throws IOException {
            file.finish();
        }

        @Override
        public SevenZArchiveEntry createArchiveEntry(File inputFile, String entryName) {
            return file.createArchiveEntry(inputFile, entryName);
        }

        @ExcludeFromJacocoGeneratedReport
        @Override
        public void write(int b) throws IOException {
            file.write(b);
        }

        @ExcludeFromJacocoGeneratedReport
        @Override
        public void write(@SuppressWarnings("NullableProblems") byte[] b) throws IOException {
            file.write(b);
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            file.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            file.close();
        }
    }
}
