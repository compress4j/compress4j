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
package org.compress4j.archivers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ExtractPermissionsTest {

    @Nested
    class TarExtractPermissionsTest extends BaseArchivePermissionsTest {
        @Override
        protected String getArchiveFileName() {
            return "archive.tar";
        }

        @Override
        protected ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.TAR;
        }
    }

    @Nested
    class ZipExtractPermissionsTest extends BaseArchivePermissionsTest {
        @Override
        protected String getArchiveFileName() {
            return "archive.zip";
        }

        @Override
        protected ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.ZIP;
        }
    }

    @SuppressWarnings("unused")
    public abstract static class BaseArchivePermissionsTest extends AbstractResourceTest {
        private Archiver archiver;
        private File archive;

        @BeforeEach
        public void setUp() {
            this.archiver = ArchiverFactory.createArchiver(getArchiveFormat());
            this.archive = new File(RESOURCES_DIR, getArchiveFileName());
        }

        protected abstract String getArchiveFileName();

        protected abstract ArchiveFormat getArchiveFormat();

        @Test
        public void extract_restoresJavaFilePermissions() throws Exception {
            archiver.extract(archive, archiveExtractTmpDir);
            assertJavaPermissions();
        }

        @Test
        public void extract_restoresUnixPermissions() throws Exception {
            archiver.extract(archive, archiveExtractTmpDir);
            assertPosixPermissions();
        }

        @Test
        public void extract_stream_restoresUnixPermissions() throws Exception {
            extractWithStream();
            assertPosixPermissions();
        }

        @Test
        public void extract_stream_restoresJavaPermissions() throws Exception {
            extractWithStream();
            assertJavaPermissions();
        }

        private void extractWithStream() throws IOException {
            try (ArchiveStream stream = archiver.stream(archive)) {
                ArchiveEntry entry;
                while ((entry = stream.getNextEntry()) != null) {
                    entry.extract(archiveExtractTmpDir);
                }
            }
        }

        private void assertJavaPermissions() {
            assertPermissions(true, true, true, getExtractedFile("permissions/executable_file.txt"));
            assertPermissions(true, true, true, getExtractedFile("permissions/private_executable_file.txt"));
            assertPermissions(true, false, false, getExtractedFile("permissions/readonly_file.txt"));
            assertPermissions(true, true, true, getExtractedFile("permissions/private_folder"));
            assertPermissions(true, true, false, getExtractedFile("permissions/private_folder/private_file.txt"));
        }

        private void assertPosixPermissions() throws IOException {
            assertThat(getPosixPermissionsString(getExtractedFile("permissions/executable_file.txt")))
                    .isEqualTo("rwxr-xr-x");
            assertThat(getPosixPermissionsString(getExtractedFile("permissions/private_executable_file.txt")))
                    .isEqualTo("rwx------");
            assertThat(getPosixPermissionsString(getExtractedFile("permissions/readonly_file.txt")))
                    .isEqualTo("r--r--r--");
            assertThat(getPosixPermissionsString(getExtractedFile("permissions/private_folder")))
                    .isEqualTo("rwx------");
            assertThat(getPosixPermissionsString(getExtractedFile("permissions/private_folder/private_file.txt")))
                    .isEqualTo("rw-------");
        }

        private void assertPermissions(
                @SuppressWarnings("SameParameterValue") boolean readable,
                boolean writable,
                boolean executable,
                File file) {
            assertThat(file.canRead()).isEqualTo(readable);
            assertThat(file.canWrite()).isEqualTo(writable);
            assertThat(file.canExecute()).isEqualTo(executable);
        }

        private String getPosixPermissionsString(File file) throws IOException {
            return PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath()));
        }

        private File getExtractedFile(String name) {
            return new File(archiveExtractTmpDir, name);
        }
    }
}
