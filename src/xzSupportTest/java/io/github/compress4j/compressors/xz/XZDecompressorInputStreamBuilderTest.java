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
package io.github.compress4j.compressors.xz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.compress4j.compressors.xz.XZDecompressor.XZDecompressorInputStreamBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class XZDecompressorInputStreamBuilderTest {

    private static final String TEST_DATA = "Hello XZ world!";
    private byte[] compressedData;
    private Object parentMock;

    @BeforeEach
    void setUp() throws IOException {
        var outputStream = new ByteArrayOutputStream();
        try (var xzOut = new XZCompressorOutputStream(outputStream)) {
            xzOut.write(TEST_DATA.getBytes(StandardCharsets.UTF_8));
        }
        compressedData = outputStream.toByteArray();

        parentMock = mock(Object.class);
    }

    /** Helper to read all bytes from an InputStream. */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        var buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    @Test
    void testBuildInputStreamDefault() throws IOException {
        // Given
        var in = new ByteArrayInputStream(compressedData);
        var builder = new XZDecompressorInputStreamBuilder<>(parentMock, in);

        // When
        try (var xzIn = builder.buildInputStream()) {

            // Then
            assertThat(xzIn).isNotNull();
            byte[] decompressed = readAllBytes(xzIn);
            assertThat(new String(decompressed, StandardCharsets.UTF_8)).isEqualTo(TEST_DATA);
        }
    }

    @Test
    void testSetDecompressConcatenated() throws IOException {
        // Given
        var in = new ByteArrayInputStream(compressedData);
        var builder = new XZDecompressorInputStreamBuilder<>(parentMock, in);

        // When
        builder.setDecompressConcatenated(true);

        // Then
        try (var xzIn = builder.buildInputStream()) {
            assertThat(xzIn).isNotNull();
            byte[] decompressed = readAllBytes(xzIn);
            assertThat(new String(decompressed, StandardCharsets.UTF_8)).isEqualTo(TEST_DATA);
        }
    }

    @Test
    void testSetMemoryLimitInKb() throws IOException {
        // Given
        var in = new ByteArrayInputStream(compressedData);
        var builder = new XZDecompressorInputStreamBuilder<>(parentMock, in);

        // When
        builder.setMemoryLimitInKb(8296);

        // Then
        try (var xzIn = builder.buildInputStream()) {
            assertThat(xzIn).isNotNull();
            byte[] decompressed = readAllBytes(xzIn);
            assertThat(new String(decompressed, StandardCharsets.UTF_8)).isEqualTo(TEST_DATA);
        }
    }

    @Test
    void testSetMemoryLimitInKbInvalid() {
        // Given
        var in = new ByteArrayInputStream(compressedData);

        // When
        var builder = new XZDecompressorInputStreamBuilder<>(parentMock, in);

        // Then
        assertThatThrownBy(() -> builder.setMemoryLimitInKb(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Memory limit must be positive or -1");

        assertThatThrownBy(() -> builder.setMemoryLimitInKb(-2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Memory limit must be positive or -1");
    }

    @Test
    void testSetMemoryLimitInKbValid() {
        // Given
        var in = new ByteArrayInputStream(compressedData);

        // When
        var builder = new XZDecompressorInputStreamBuilder<>(parentMock, in);

        // Then
        assertThatCode(() -> builder.setMemoryLimitInKb(-1)).doesNotThrowAnyException();
        assertThatCode(() -> builder.setMemoryLimitInKb(1)).doesNotThrowAnyException();
        assertThatCode(() -> builder.setMemoryLimitInKb(Integer.MAX_VALUE)).doesNotThrowAnyException();
    }

    @Test
    void testParentBuilder() {
        // Given
        var in = new ByteArrayInputStream(compressedData);

        // When
        var builder = new XZDecompressorInputStreamBuilder<>(parentMock, in);

        // Then
        assertThat(builder.parentBuilder()).isSameAs(parentMock);
    }
}
