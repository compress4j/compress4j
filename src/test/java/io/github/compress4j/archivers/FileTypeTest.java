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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileTypeTest {

    @Test
    void get_archive_returnsCorrectFileType() {
        FileType extension;

        extension = FileType.get("/path/to/file/file.tar");
        assertThat(extension.isArchive()).isTrue();
        assertThat(extension.isCompressed()).isFalse();

        assertThat(extension.getCompressionType()).isNull();
        assertThat(extension.getArchiveFormat()).isEqualTo(ArchiveFormat.TAR);
        assertThat(extension.getSuffix()).isEqualTo(".tar");
    }

    @Test
    void get_compressed_returnsCorrectFileType() {
        FileType extension;

        extension = FileType.get("/path/to/file/file.gz");
        assertThat(extension.isArchive()).isFalse();
        assertThat(extension.isCompressed()).isTrue();

        assertThat(extension.getCompressionType()).isEqualTo(CompressionType.GZIP);
        assertThat(extension.getArchiveFormat()).isNull();
        assertThat(extension.getSuffix()).isEqualTo(".gz");
    }

    @Test
    void get_compressedArchive_returnsCorrectFileType() {
        FileType extension;

        extension = FileType.get("/path/to/file/file.tar.gz");
        assertThat(extension.isArchive()).isTrue();
        assertThat(extension.isCompressed()).isTrue();

        assertThat(extension.getCompressionType()).isEqualTo(CompressionType.GZIP);
        assertThat(extension.getArchiveFormat()).isEqualTo(ArchiveFormat.TAR);
        assertThat(extension.getSuffix()).isEqualTo(".tar.gz");
    }

    @Test
    void get_unknownExtension_returnsUnknown() {
        assertThat(FileType.get("/path/to/file/file.foobar")).isEqualTo(FileType.UNKNOWN);
    }
}
