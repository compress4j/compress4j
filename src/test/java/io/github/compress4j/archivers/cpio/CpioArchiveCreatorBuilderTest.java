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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CpioArchiveCreatorBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void testBuilderWithPath() throws IOException {
        Path archivePath = tempDir.resolve("test-builder-path.cpio");
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Test content".getBytes());

        // Build creator using path
        CpioArchiveCreator.CpioArchiveCreatorBuilder builder = CpioArchiveCreator.builder(archivePath);
        assertThat(builder).isNotNull();

        try (CpioArchiveCreator creator = builder.build()) {
            creator.addFile("test.txt", testFile);
        }

        // Verify archive was created
        assertThat(archivePath).exists();
        assertThat(Files.size(archivePath)).isGreaterThan(0);
    }

    @Test
    void testBuilderWithOutputStream() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Test content".getBytes());

        // Build creator using output stream
        CpioArchiveCreator.CpioArchiveCreatorBuilder builder = CpioArchiveCreator.builder(outputStream);
        assertThat(builder).isNotNull();

        try (CpioArchiveCreator creator = builder.build()) {
            creator.addFile("test.txt", testFile);
        }

        // Verify archive was created in the stream
        assertThat(outputStream.size()).isGreaterThan(0);
    }

    @Test
    void testBuilderWithDefaultConfiguration() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Test content".getBytes());

        // Build creator with default configuration
        try (CpioArchiveCreator creator =
                CpioArchiveCreator.builder(outputStream).build()) {
            creator.addFile("test.txt", testFile);
        }

        // Verify archive was created with default settings
        assertThat(outputStream.size()).isGreaterThan(0);

        // Verify the archive can be read (indicating correct default format)
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try (CpioArchiveInputStream cpioInput = new CpioArchiveInputStream(inputStream)) {
            var entry = cpioInput.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("test.txt");
        }
    }

    @Test
    void testBuilderWithNewFormat() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Test content".getBytes());

        // Build creator with NEW format
        try (CpioArchiveCreator creator = CpioArchiveCreator.builder(outputStream)
                .cpioOutputStream()
                .format(CpioConstants.FORMAT_NEW)
                .and()
                .build()) {
            creator.addFile("test.txt", testFile);
        }

        // Verify archive was created
        assertThat(outputStream.size()).isGreaterThan(0);

        // Verify the archive can be read with NEW format
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try (CpioArchiveInputStream cpioInput = new CpioArchiveInputStream(inputStream)) {
            var entry = cpioInput.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("test.txt");
        }
    }

    @Test
    void testBuilderWithOldAsciiFormat() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Test content".getBytes());

        // Build creator with OLD ASCII format
        try (CpioArchiveCreator creator = CpioArchiveCreator.builder(outputStream)
                .cpioOutputStream()
                .format(CpioConstants.FORMAT_OLD_ASCII)
                .and()
                .build()) {
            creator.addFile("test.txt", testFile);
        }

        // Verify archive was created
        assertThat(outputStream.size()).isGreaterThan(0);

        // Verify the archive can be read with OLD ASCII format
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try (CpioArchiveInputStream cpioInput = new CpioArchiveInputStream(inputStream)) {
            var entry = cpioInput.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("test.txt");
        }
    }

    @Test
    void testBuilderWithOldBinaryFormat() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Test content".getBytes());

        // Build creator with OLD BINARY format
        try (CpioArchiveCreator creator = CpioArchiveCreator.builder(outputStream)
                .cpioOutputStream()
                .format(CpioConstants.FORMAT_OLD_BINARY)
                .and()
                .build()) {
            creator.addFile("test.txt", testFile);
        }

        // Verify archive was created
        assertThat(outputStream.size()).isGreaterThan(0);

        // Verify the archive can be read with OLD BINARY format
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try (CpioArchiveInputStream cpioInput = new CpioArchiveInputStream(inputStream)) {
            var entry = cpioInput.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("test.txt");
        }
    }

    @Test
    void testBuilderWithCustomBlockSize() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Test content".getBytes());

        // Build creator with custom block size
        try (CpioArchiveCreator creator = CpioArchiveCreator.builder(outputStream)
                .cpioOutputStream()
                .blockSize(1024)
                .and()
                .build()) {
            creator.addFile("test.txt", testFile);
        }

        // Verify archive was created
        assertThat(outputStream.size()).isGreaterThan(0);

        // Verify the archive can be read with custom block size
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try (CpioArchiveInputStream cpioInput = new CpioArchiveInputStream(inputStream, 1024)) {
            var entry = cpioInput.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("test.txt");
        }
    }

    @Test
    void testBuilderWithCustomEncoding() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Path testFile = tempDir.resolve("test-äöü.txt");
        Files.write(testFile, "Test content with special chars".getBytes());

        // Build creator with UTF-8 encoding
        try (CpioArchiveCreator creator = CpioArchiveCreator.builder(outputStream)
                .cpioOutputStream()
                .encoding("UTF-8")
                .and()
                .build()) {
            creator.addFile("test-äöü.txt", testFile);
        }

        // Verify archive was created
        assertThat(outputStream.size()).isGreaterThan(0);

        // Verify the archive can be read with UTF-8 encoding
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try (CpioArchiveInputStream cpioInput = new CpioArchiveInputStream(inputStream, 512, "UTF-8")) {
            var entry = cpioInput.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("test-äöü.txt");
        }
    }

    @Test
    void testBuilderWithAllCustomOptions() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Path testFile = tempDir.resolve("test-complete.txt");
        Files.write(testFile, "Complete configuration test".getBytes());

        // Build creator with all custom options
        try (CpioArchiveCreator creator = CpioArchiveCreator.builder(outputStream)
                .cpioOutputStream()
                .format(CpioConstants.FORMAT_NEW)
                .blockSize(2048)
                .encoding("UTF-8")
                .and()
                .build()) {
            creator.addFile("test-complete.txt", testFile);
        }

        // Verify archive was created
        assertThat(outputStream.size()).isGreaterThan(0);

        // Verify the archive can be read with all custom options
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try (CpioArchiveInputStream cpioInput = new CpioArchiveInputStream(inputStream, 2048, "UTF-8")) {
            var entry = cpioInput.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("test-complete.txt");
            assertThat(entry.getSize()).isEqualTo("Complete configuration test".length());
        }
    }

    @Test
    void testBuilderChaining() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Test method chaining works correctly
        CpioArchiveCreator.CpioArchiveCreatorBuilder builder = CpioArchiveCreator.builder(outputStream)
                .cpioOutputStream()
                .format(CpioConstants.FORMAT_NEW)
                .blockSize(1024)
                .encoding("UTF-8")
                .and();

        assertThat(builder).isNotNull();

        try (CpioArchiveCreator creator = builder.build()) {
            assertThat(creator).isNotNull();
        }
    }

    @Test
    void testBuilderWithInvalidPath() {
        Path invalidPath = tempDir.resolve("nonexistent/invalid.cpio");

        // Should throw IOException when trying to create output stream for invalid path
        assertThatThrownBy(() -> CpioArchiveCreator.builder(invalidPath)).isInstanceOf(IOException.class);
    }

    @Test
    void testBuilderWithNullOutputStream() {
        // Should throw exception when null output stream is provided
        assertThatThrownBy(() -> CpioArchiveCreator.builder((ByteArrayOutputStream) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testBuilderWithNullPath() {
        // Should throw exception when null path is provided
        assertThatThrownBy(() -> CpioArchiveCreator.builder((Path) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testMultipleBuildsFromSameBuilder() throws IOException {
        ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream();
        CpioArchiveCreator.CpioArchiveCreatorBuilder builder = CpioArchiveCreator.builder(outputStream1);

        // First build
        try (CpioArchiveCreator creator1 = builder.build()) {
            assertThat(creator1).isNotNull();
        }

        // Second build should work (builder should be reusable)
        try (CpioArchiveCreator creator2 = builder.build()) {
            assertThat(creator2).isNotNull();
        }
    }

    @Test
    void testBuilderOutputStreamConfiguration() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Test that the output stream builder can be accessed and configured
        var builderInstance = CpioArchiveCreator.builder(outputStream);
        var cpioOutputStreamBuilder = builderInstance.cpioOutputStream();

        assertThat(cpioOutputStreamBuilder).isNotNull();

        // Configure the output stream builder
        var configuredBuilder = cpioOutputStreamBuilder
                .format(CpioConstants.FORMAT_NEW)
                .blockSize(1024)
                .encoding("UTF-8")
                .and();

        assertThat(configuredBuilder).isSameAs(builderInstance);

        // Build and verify it works
        try (CpioArchiveCreator creator = configuredBuilder.build()) {
            assertThat(creator).isNotNull();
        }
    }

    @Test
    void testBuilderGetThis() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CpioArchiveCreator.CpioArchiveCreatorBuilder builder = CpioArchiveCreator.builder(outputStream);

        // The getThis() method should return the builder itself for chaining
        // This is tested indirectly through the fluent API usage above
        try (CpioArchiveCreator creator = builder.build()) {
            assertThat(creator).isNotNull();
        }
    }

    @Test
    void testBuilderWithDifferentBlockSizes() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Test content".getBytes());

        // Test various block sizes
        int[] blockSizes = {256, 512, 1024, 2048, 4096};

        for (int blockSize : blockSizes) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try (CpioArchiveCreator creator = CpioArchiveCreator.builder(outputStream)
                    .cpioOutputStream()
                    .blockSize(blockSize)
                    .and()
                    .build()) {
                creator.addFile("test.txt", testFile);
            }

            // Verify archive was created successfully with each block size
            assertThat(outputStream.size()).isGreaterThan(0);

            // Verify archive can be read
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            try (CpioArchiveInputStream cpioInput = new CpioArchiveInputStream(inputStream, blockSize)) {
                var entry = cpioInput.getNextEntry();
                assertThat(entry).isNotNull();
                assertThat(entry.getName()).isEqualTo("test.txt");
            }
        }
    }

    @Test
    void testBuilderWithDifferentEncodings() throws IOException {
        Path testFile = tempDir.resolve("test-encoding.txt");
        Files.write(testFile, "Test content".getBytes());

        // Test various encodings
        String[] encodings = {"UTF-8", "ISO-8859-1", "US-ASCII"};

        for (String encoding : encodings) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try (CpioArchiveCreator creator = CpioArchiveCreator.builder(outputStream)
                    .cpioOutputStream()
                    .encoding(encoding)
                    .and()
                    .build()) {
                creator.addFile("test-encoding.txt", testFile);
            }

            // Verify archive was created successfully with each encoding
            assertThat(outputStream.size()).isGreaterThan(0);

            // Verify archive can be read
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            try (CpioArchiveInputStream cpioInput = new CpioArchiveInputStream(inputStream, 512, encoding)) {
                var entry = cpioInput.getNextEntry();
                assertThat(entry).isNotNull();
                assertThat(entry.getName()).isEqualTo("test-encoding.txt");
            }
        }
    }
}
