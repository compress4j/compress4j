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
package io.github.compress4j.archivers.zip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.compress4j.archivers.zip.ZipArchiveExtractor.ZipArchiveExtractorBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.function.IOFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZipArchiveExtractorBuilderTest {

    @TempDir
    Path tempPath;

    private Path archivePath;

    @Mock
    private ZipFile.Builder mockZipFileBuilder;

    @Mock
    private ZipFile mockZipFile;

    @Mock
    private SeekableByteChannel mockChannel;

    @Mock
    private IOFunction<InputStream, InputStream> mockFactory;

    private MockedStatic<ZipFile> zipFileMockedStatic;

    @BeforeEach
    void setUp() {
        archivePath = tempPath.resolve("test.zip");

        zipFileMockedStatic = mockStatic(ZipFile.class);
        zipFileMockedStatic.when(ZipFile::builder).thenReturn(mockZipFileBuilder);
    }

    /**
     * Helper method to stub all fluent setters on the mock builder. Call this only from tests that build an extractor
     * or stream.
     */
    private void stubFluentSetters() {
        when(mockZipFileBuilder.setIgnoreLocalFileHeader(any(boolean.class))).thenReturn(mockZipFileBuilder);
        when(mockZipFileBuilder.setMaxNumberOfDisks(any(long.class))).thenReturn(mockZipFileBuilder);
        when(mockZipFileBuilder.setSeekableByteChannel(nullable(SeekableByteChannel.class)))
                .thenReturn(mockZipFileBuilder);
        when(mockZipFileBuilder.setUseUnicodeExtraFields(any(boolean.class))).thenReturn(mockZipFileBuilder);
        when(mockZipFileBuilder.setPath(any(Path.class))).thenReturn(mockZipFileBuilder);
    }

    @AfterEach
    void tearDown() {
        zipFileMockedStatic.close();
    }

    @Test
    @DisplayName("Static builder(Path) method creates a builder")
    void testStaticBuilder() throws IOException {
        // When
        var builder = ZipArchiveExtractor.builder(archivePath);

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("build() should configure ZipFile.Builder with all setters and build an extractor")
    void testBuild_WithAllSetters() throws IOException {
        stubFluentSetters();
        when(mockZipFileBuilder.setZstdInputStreamFactory(any())).thenReturn(mockZipFileBuilder);
        when(mockZipFileBuilder.get()).thenReturn(mockZipFile);

        // Given
        var builder = new ZipArchiveExtractorBuilder(archivePath)
                .setIgnoreLocalFileHeader(true)
                .setMaxNumberOfDisks(10)
                .setSeekableByteChannel(mockChannel)
                .setUseUnicodeExtraFields(false)
                .setZstdInputStreamFactory(mockFactory);

        // When
        var extractor = builder.build();

        // Then
        assertThat(extractor).isNotNull();

        zipFileMockedStatic.verify(ZipFile::builder);

        verify(mockZipFileBuilder).setIgnoreLocalFileHeader(true);
        verify(mockZipFileBuilder).setMaxNumberOfDisks(10);
        verify(mockZipFileBuilder).setSeekableByteChannel(mockChannel);
        verify(mockZipFileBuilder).setUseUnicodeExtraFields(false);
        verify(mockZipFileBuilder).setZstdInputStreamFactory(mockFactory);
        verify(mockZipFileBuilder).setPath(archivePath);

        verify(mockZipFileBuilder).get();
    }

    @Test
    @DisplayName("buildArchiveInputStream() should configure with default values")
    void testBuildArchiveInputStream_Defaults() throws IOException {
        // Given
        stubFluentSetters();
        when(mockZipFileBuilder.setZstdInputStreamFactory(isNull())).thenReturn(mockZipFileBuilder);
        when(mockZipFileBuilder.get()).thenReturn(mockZipFile);

        var builder = new ZipArchiveExtractorBuilder(archivePath);

        // When
        var inputStream = builder.buildArchiveInputStream();

        // Then
        assertThat(inputStream).isNotNull();

        zipFileMockedStatic.verify(ZipFile::builder);
        verify(mockZipFileBuilder).setIgnoreLocalFileHeader(false);
        verify(mockZipFileBuilder).setMaxNumberOfDisks(1);
        verify(mockZipFileBuilder).setUseUnicodeExtraFields(true);
        verify(mockZipFileBuilder).setPath(archivePath);
        verify(mockZipFileBuilder).setSeekableByteChannel(null);
        verify(mockZipFileBuilder).setZstdInputStreamFactory(null);
        verify(mockZipFileBuilder).get();
    }

    @Test
    @DisplayName("getThis() should return the builder instance")
    void testGetThis() throws IOException {
        // Given
        var builder = new ZipArchiveExtractorBuilder(archivePath);

        // When
        var self = builder.getThis();

        // Then
        assertThat(self).isSameAs(builder);
    }

    @Test
    @DisplayName("build() should propagate IOException from ZipFile.builder().get()")
    void testBuild_PropagatesIOException() throws IOException {
        stubFluentSetters();
        when(mockZipFileBuilder.setZstdInputStreamFactory(isNull())).thenReturn(mockZipFileBuilder);
        when(mockZipFileBuilder.get()).thenThrow(new IOException("Test build error"));

        // Given
        var builder = new ZipArchiveExtractorBuilder(archivePath);

        // When & Then
        assertThatThrownBy(builder::build).isInstanceOf(IOException.class).hasMessage("Test build error");
    }

    @Test
    @DisplayName("buildArchiveInputStream() should propagate IOException from ZipFile.builder().get()")
    void testBuildArchiveInputStream_PropagatesIOException() throws IOException {
        stubFluentSetters();
        when(mockZipFileBuilder.setZstdInputStreamFactory(isNull())).thenReturn(mockZipFileBuilder);
        when(mockZipFileBuilder.get()).thenThrow(new IOException("Test build error"));

        // Given
        var builder = new ZipArchiveExtractorBuilder(archivePath);

        // When & Then
        assertThatThrownBy(builder::buildArchiveInputStream)
                .isInstanceOf(IOException.class)
                .hasMessage("Test build error");
    }
}
