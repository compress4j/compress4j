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
package io.github.compress4j.archivers.cpio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CpioArchiveOutputStreamBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void testBuilderWithDefaultConfiguration() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        var creatorBuilder = CpioArchiveCreator.builder(outputStream);
        var outputStreamBuilder = creatorBuilder.cpioOutputStream();

        assertThat(outputStreamBuilder).isNotNull();

        // when
        var cpioStream = outputStreamBuilder.build();

        // then
        assertThat(cpioStream).isNotNull();
        cpioStream.close();
        assertThat(outputStream.size()).isGreaterThan(0);
    }

    @Test
    void testBuilderWithNewFormat() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        var creatorBuilder = CpioArchiveCreator.builder(outputStream);
        var outputStreamBuilder = creatorBuilder.cpioOutputStream().format(CpioConstants.FORMAT_NEW);

        assertThat(outputStreamBuilder).isNotNull();

        // when
        var cpioStream = outputStreamBuilder.build();

        // then
        assertThat(cpioStream).isNotNull();
        cpioStream.close();

        assertThat(outputStream.size()).isGreaterThan(0);

        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        assertThatNoException().isThrownBy(() -> {
            try (var cpioInput = new CpioArchiveInputStream(inputStream)) {
                cpioInput.getNextEntry();
            }
        });
    }

    @Test
    void testBuilderWithOldAsciiFormat() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();

        var creatorBuilder = CpioArchiveCreator.builder(outputStream);
        var outputStreamBuilder = creatorBuilder.cpioOutputStream().format(CpioConstants.FORMAT_OLD_ASCII);

        assertThat(outputStreamBuilder).isNotNull();

        // when
        var cpioStream = outputStreamBuilder.build();

        // then
        assertThat(cpioStream).isNotNull();
        cpioStream.close();
        assertThat(outputStream.size()).isGreaterThan(0);
    }

    @Test
    void testBuilderWithOldBinaryFormat() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();

        var creatorBuilder = CpioArchiveCreator.builder(outputStream);
        var outputStreamBuilder = creatorBuilder.cpioOutputStream().format(CpioConstants.FORMAT_OLD_BINARY);

        assertThat(outputStreamBuilder).isNotNull();

        // when
        var cpioStream = outputStreamBuilder.build();

        // then
        assertThat(cpioStream).isNotNull();
        cpioStream.close();
        assertThat(outputStream.size()).isGreaterThan(0);
    }

    @Test
    void testBuilderWithCustomBlockSize() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        var creatorBuilder = CpioArchiveCreator.builder(outputStream);
        var outputStreamBuilder = creatorBuilder.cpioOutputStream().blockSize(1024);

        assertThat(outputStreamBuilder).isNotNull();

        // when
        var cpioStream = outputStreamBuilder.build();

        // then
        assertThat(cpioStream).isNotNull();
        cpioStream.close();
        assertThat(outputStream.size()).isGreaterThan(0);

        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        assertThatNoException().isThrownBy(() -> {
            try (var cpioInput = new CpioArchiveInputStream(inputStream, 1024)) {
                cpioInput.getNextEntry();
            }
        });
    }

    @Test
    void testBuilderWithCustomEncoding() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        var creatorBuilder = CpioArchiveCreator.builder(outputStream);
        var outputStreamBuilder = creatorBuilder.cpioOutputStream().encoding("ISO-8859-1");

        assertThat(outputStreamBuilder).isNotNull();

        // when
        var cpioStream = outputStreamBuilder.build();

        // then
        assertThat(cpioStream).isNotNull();
        cpioStream.close();
        assertThat(outputStream.size()).isGreaterThan(0);

        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        assertThatNoException().isThrownBy(() -> {
            try (var cpioInput = new CpioArchiveInputStream(inputStream, 512, "ISO-8859-1")) {
                cpioInput.getNextEntry();
            }
        });
    }

    @Test
    void testBuilderWithAllCustomOptions() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        var creatorBuilder = CpioArchiveCreator.builder(outputStream);
        var outputStreamBuilder = creatorBuilder
                .cpioOutputStream()
                .format(CpioConstants.FORMAT_NEW)
                .blockSize(2048)
                .encoding("UTF-8");

        assertThat(outputStreamBuilder).isNotNull();

        // when
        var cpioStream = outputStreamBuilder.build();
        assertThat(cpioStream).isNotNull();

        cpioStream.close();

        assertThat(outputStream.size()).isGreaterThan(0);

        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        assertThatNoException().isThrownBy(() -> {
            try (var cpioInput = new CpioArchiveInputStream(inputStream, 2048, "UTF-8")) {
                cpioInput.getNextEntry();
            }
        });
    }

    @Test
    void testBuilderMethodChaining() {
        // given
        var outputStream = new ByteArrayOutputStream();
        var creatorBuilder = CpioArchiveCreator.builder(outputStream);
        var outputStreamBuilder = creatorBuilder.cpioOutputStream();

        // when & then
        var chainedBuilder1 = outputStreamBuilder.format(CpioConstants.FORMAT_NEW);
        assertThat(chainedBuilder1).isSameAs(outputStreamBuilder);

        var chainedBuilder2 = outputStreamBuilder.blockSize(1024);
        assertThat(chainedBuilder2).isSameAs(outputStreamBuilder);

        var chainedBuilder3 = outputStreamBuilder.encoding("UTF-8");
        assertThat(chainedBuilder3).isSameAs(outputStreamBuilder);

        var parentBuilder = outputStreamBuilder.and();
        assertThat(parentBuilder).isSameAs(creatorBuilder);
    }

    @Test
    void testBuilderAndMethod() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();

        // when
        var creatorBuilder = CpioArchiveCreator.builder(outputStream);
        var outputStreamBuilder = creatorBuilder.cpioOutputStream();
        var returnedParent = outputStreamBuilder.and();

        // then
        assertThat(returnedParent).isSameAs(creatorBuilder);
        try (CpioArchiveCreator creator = returnedParent.build()) {
            assertThat(creator).isNotNull();
        }
    }

    @Test
    void testBuilderWithVariousBlockSizes() throws IOException {
        // given
        int[] blockSizes = {256, 512, 1024, 2048, 4096, 8192};

        for (int blockSize : blockSizes) {
            var outputStream = new ByteArrayOutputStream();

            // Create builder with specific block size
            var creatorBuilder = CpioArchiveCreator.builder(outputStream);
            var outputStreamBuilder = creatorBuilder.cpioOutputStream().blockSize(blockSize);

            // when
            var cpioStream = outputStreamBuilder.build();

            // then
            assertThat(cpioStream).isNotNull();
            cpioStream.close();
            assertThat(outputStream.size()).isGreaterThan(0);

            var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            assertThatNoException().isThrownBy(() -> {
                try (var cpioInput = new CpioArchiveInputStream(inputStream, blockSize)) {
                    cpioInput.getNextEntry();
                }
            });
        }
    }

    @Test
    void testBuilderWithVariousEncodings() throws IOException {
        // given
        String[] encodings = {"UTF-8", "ISO-8859-1", "US-ASCII", "UTF-16"};

        for (String encoding : encodings) {
            var outputStream = new ByteArrayOutputStream();
            var creatorBuilder = CpioArchiveCreator.builder(outputStream);
            var outputStreamBuilder = creatorBuilder.cpioOutputStream().encoding(encoding);

            // when
            var cpioStream = outputStreamBuilder.build();

            // then
            assertThat(cpioStream).isNotNull();
            cpioStream.close();
            assertThat(outputStream.size()).isGreaterThan(0);

            var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            assertThatNoException().isThrownBy(() -> {
                try (var cpioInput = new CpioArchiveInputStream(inputStream, 512, encoding)) {
                    cpioInput.getNextEntry();
                }
            });
        }
    }

    @Test
    void testBuilderWithAllFormats() throws IOException {
        // given
        short[] formats = {CpioConstants.FORMAT_NEW, CpioConstants.FORMAT_OLD_ASCII, CpioConstants.FORMAT_OLD_BINARY};

        for (short format : formats) {
            var outputStream = new ByteArrayOutputStream();
            var creatorBuilder = CpioArchiveCreator.builder(outputStream);
            var outputStreamBuilder = creatorBuilder.cpioOutputStream().format(format);

            // when
            var cpioStream = outputStreamBuilder.build();

            // then
            assertThat(cpioStream).isNotNull();
            cpioStream.close();
            assertThat(outputStream.size()).isGreaterThan(0);

            var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            assertThatNoException().isThrownBy(() -> {
                try (var cpioInput = new CpioArchiveInputStream(inputStream)) {
                    cpioInput.getNextEntry();
                }
            });
        }
    }

    @Test
    void testBuilderWithActualFileContent() throws IOException {
        // given
        var testFile = tempDir.resolve("test-file.txt");
        Files.write(testFile, "Test file content for CPIO".getBytes());

        var outputStream = new ByteArrayOutputStream();

        // when
        try (CpioArchiveCreator creator = CpioArchiveCreator.builder(outputStream)
                .cpioOutputStream()
                .format(CpioConstants.FORMAT_NEW)
                .blockSize(1024)
                .encoding("UTF-8")
                .and()
                .build()) {
            creator.addFile("test-file.txt", testFile);
        }

        // then
        assertThat(outputStream.size()).isGreaterThan(0);

        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try (var cpioInput = new CpioArchiveInputStream(inputStream, 1024, "UTF-8")) {
            var entry = cpioInput.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("test-file.txt");
            assertThat(entry.getSize()).isEqualTo("Test file content for CPIO".length());

            byte[] buffer = new byte[(int) entry.getSize()];
            int bytesRead = cpioInput.read(buffer);
            assertThat(bytesRead).isEqualTo(entry.getSize());
            assertThat(new String(buffer)).isEqualTo("Test file content for CPIO");
        }
    }

    @Test
    void testBuilderReusability() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        var creatorBuilder = CpioArchiveCreator.builder(outputStream);
        var outputStreamBuilder = creatorBuilder
                .cpioOutputStream()
                .format(CpioConstants.FORMAT_NEW)
                .blockSize(1024)
                .encoding("UTF-8");

        // when & then
        var stream1 = outputStreamBuilder.build();
        assertThat(stream1).isNotNull();
        stream1.close();

        var stream2 = outputStreamBuilder.build();
        assertThat(stream2).isNotNull();
        stream2.close();

        assertThat(outputStream.size()).isGreaterThan(0);
    }

    @Test
    void testBuilderConfigurationPersistence() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        var creatorBuilder = CpioArchiveCreator.builder(outputStream);
        var outputStreamBuilder = creatorBuilder.cpioOutputStream();
        outputStreamBuilder.format(CpioConstants.FORMAT_NEW);
        outputStreamBuilder.blockSize(2048);
        outputStreamBuilder.encoding("ISO-8859-1");

        // when
        var cpioStream = outputStreamBuilder.build();

        // then
        assertThat(cpioStream).isNotNull();
        cpioStream.close();
        assertThat(outputStream.size()).isGreaterThan(0);

        var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        assertThatNoException().isThrownBy(() -> {
            try (var cpioInput = new CpioArchiveInputStream(inputStream, 2048, "ISO-8859-1")) {
                cpioInput.getNextEntry();
            }
        });
    }
}
