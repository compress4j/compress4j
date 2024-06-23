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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import org.junit.jupiter.api.Test;

class FactoryTest extends AbstractResourceTest {

    @Test
    void createArchiver_withUnknownArchiveType_fails() {
        assertThatThrownBy(() -> ArchiverFactory.createArchiver("foo")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createArchiver_withUnknownArchiveAndCompressionType_fails() {
        assertThatThrownBy(() -> ArchiverFactory.createArchiver("foo", "bar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createArchiver_withUnknownCompressionType_fails() {
        assertThatThrownBy(() -> ArchiverFactory.createArchiver("tar", "bar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createArchiver_fromStringArchiveFormat_returnsCorrectArchiver() {
        Archiver archiver = ArchiverFactory.createArchiver("tar");

        assertThat(archiver).isNotNull().isOfAnyClassIn(CommonsArchiver.class);
    }

    @Test
    void createArchiver_fromStringArchiveAndCompressionFormat_returnsCorrectArchiver() {
        Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");

        assertThat(archiver).isNotNull().isOfAnyClassIn(ArchiverCompressorDecorator.class);
    }

    @Test
    void createArchiver_fromCompressedArchiveFile_returnsCorrectArchiver() {
        Archiver archiver = ArchiverFactory.createArchiver(new File(RESOURCES_DIR, "archive.tar.gz"));

        assertThat(archiver).isNotNull().isOfAnyClassIn(ArchiverCompressorDecorator.class);
    }

    @Test
    void createArchiver_fromArchiveFile_returnsCorrectArchiver() {
        Archiver archiver = ArchiverFactory.createArchiver(new File(RESOURCES_DIR, "archive.tar"));

        assertThat(archiver).isNotNull().isOfAnyClassIn(CommonsArchiver.class);
    }

    @Test
    void createArchiver_fromUnknownFileExtension_fails() {
        assertThatThrownBy(() -> ArchiverFactory.createArchiver(nonReadableFile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createArchiver_fromUnknownArchiveType_fails() {
        File archive = new File(RESOURCES_DIR, "compress.txt.gz");
        assertThatThrownBy(() -> ArchiverFactory.createArchiver(archive)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createCompressor_withUnknownCompressionType_fails() {
        assertThatThrownBy(() -> CompressorFactory.createCompressor("foo"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createCompressor_fromStringCompressionFormat_returnsCorrectCompressor() {
        Compressor compressor = CompressorFactory.createCompressor("gz");

        assertThat(compressor).isNotNull().isOfAnyClassIn(CommonsCompressor.class);
    }

    @Test
    void createCompressor_fromFile_returnsCorrectCompressor() {
        Compressor compressor = CompressorFactory.createCompressor(new File(RESOURCES_DIR, "compress.txt.gz"));

        assertThat(compressor).isNotNull().isOfAnyClassIn(CommonsCompressor.class);
    }

    @Test
    void createCompressor_fromUnknownFileExtension_fails() {
        assertThatThrownBy(() -> CompressorFactory.createCompressor(nonReadableFile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createCompressor_fromUnknownCompressionType_fails() {
        File archive = new File(RESOURCES_DIR, "archive.tar");
        assertThatThrownBy(() -> CompressorFactory.createCompressor(archive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createCompressor_fromUnknownFileType_throwsException() {
        assertThatThrownBy(() -> CompressorFactory.createCompressor(FileType.UNKNOWN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
