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

import static java.util.zip.ZipEntry.DEFLATED;
import static java.util.zip.ZipEntry.STORED;
import static org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.DEFAULT_COMPRESSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.compress4j.archivers.zip.ZipArchiveCreator.ZipArchiveCreatorBuilder;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZipArchiveCreatorBuilderTest {

    @Mock
    private OutputStream mockOutputStream;

    private ZipArchiveCreatorBuilder builder;

    @BeforeEach
    void setUp() {
        builder = spy(ZipArchiveCreator.builder(mockOutputStream));
    }

    @Test
    @DisplayName("static builder(OutputStream) should return a builder")
    void testBuilderWithOutputStream() {
        // When
        var newBuilder = ZipArchiveCreator.builder(mockOutputStream);

        // Then
        assertThat(newBuilder).isNotNull();
    }

    @Test
    @DisplayName("getThis() should return the builder instance")
    void testGetThis() {
        // When
        var self = builder.getThis();

        // Then
        assertThat(self).isSameAs(builder);
    }

    @Test
    @DisplayName("compressionLevel() should set valid levels")
    void testCompressionLevel_Valid() {
        // When & Then
        assertThat(builder.compressionLevel(0)).isSameAs(builder);
        assertThat(builder.compressionLevel(9)).isSameAs(builder);
    }

    @Test
    @DisplayName("compressionLevel() should throw for invalid levels")
    void testCompressionLevel_Invalid() {
        // Given
        var creatorBuilder = ZipArchiveCreator.builder(mockOutputStream);

        // When & Then
        assertThatThrownBy(() -> creatorBuilder.compressionLevel(-2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Compression level must be between 0 and 9");

        assertThatThrownBy(() -> creatorBuilder.compressionLevel(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Compression level must be between 0 and 9");

        assertThatThrownBy(() -> creatorBuilder.compressionLevel(DEFAULT_COMPRESSION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Compression level must be between 0 and 9");
    }

    @Test
    @DisplayName("compressionMethod() should set valid methods")
    void testCompressionMethod_Valid() {
        // When & Then
        assertThat(builder.compressionMethod(STORED)).isSameAs(builder);
        assertThat(builder.compressionMethod(DEFLATED)).isSameAs(builder);
    }

    @Test
    @DisplayName("compressionMethod() should throw for invalid methods")
    void testCompressionMethod_Invalid() {
        // Given
        var creatorBuilder = ZipArchiveCreator.builder(mockOutputStream);

        // When & Then
        assertThatThrownBy(() -> creatorBuilder.compressionMethod(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Compression method must be STORED or DEFLATED");

        assertThatThrownBy(() -> creatorBuilder.compressionMethod(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Compression method must be STORED or DEFLATED");
    }

    @Test
    @DisplayName("buildArchiveOutputStream() should apply default settings")
    void testBuildArchiveOutputStream_Defaults() {
        try (MockedConstruction<ZipArchiveOutputStream> mockConstruction =
                mockConstruction(ZipArchiveOutputStream.class, (mock, context) -> {
                    assertThat(context.arguments()).hasSize(1);
                    assertThat(context.arguments().getFirst()).isSameAs(mockOutputStream);
                })) {

            // When
            var defaultBuilder = new ZipArchiveCreatorBuilder(mockOutputStream);
            //noinspection resource
            defaultBuilder.buildArchiveOutputStream();

            // Then
            assertThat(mockConstruction.constructed()).hasSize(1).element(0).satisfies(mockedZipOut -> {
                verify(mockedZipOut).setLevel(ZipArchiveOutputStream.DEFAULT_COMPRESSION);
                verify(mockedZipOut).setMethod(DEFLATED);
                verify(mockedZipOut).setComment("");
                verify(mockedZipOut).setUseZip64(Zip64Mode.AsNeeded);
                verify(mockedZipOut).setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.NEVER);
                verify(mockedZipOut).setFallbackToUTF8(false);
                verify(mockedZipOut).setUseLanguageEncodingFlag(true);
                verify(mockedZipOut).setEncoding("UTF-8");
            });
        }
    }

    @Test
    @DisplayName("buildArchiveOutputStream() should apply custom settings")
    void testBuildArchiveOutputStream_Custom() {
        try (MockedConstruction<ZipArchiveOutputStream> mockConstruction =
                mockConstruction(ZipArchiveOutputStream.class, (mock, context) -> {
                    assertThat(context.arguments()).hasSize(1);
                    assertThat(context.arguments().getFirst()).isSameAs(mockOutputStream);
                })) {

            // Given
            builder.compressionLevel(5)
                    .compressionMethod(STORED)
                    .setComment("test comment")
                    .setUseZip64(Zip64Mode.Always)
                    .setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS)
                    .setFallbackToUTF8(true)
                    .setUseLanguageEncodingFlag(false)
                    .setEncoding("ISO-8859-1");

            // When
            //noinspection resource
            builder.buildArchiveOutputStream();

            // Then
            assertThat(mockConstruction.constructed()).hasSize(1).element(0).satisfies(mockedZipOut -> {
                verify(mockedZipOut).setLevel(5);
                verify(mockedZipOut).setMethod(STORED);
                verify(mockedZipOut).setComment("test comment");
                verify(mockedZipOut).setUseZip64(Zip64Mode.Always);
                verify(mockedZipOut).setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);
                verify(mockedZipOut).setFallbackToUTF8(true);
                verify(mockedZipOut).setUseLanguageEncodingFlag(false);
                verify(mockedZipOut).setEncoding("ISO-8859-1");
            });
        }
    }

    @Test
    @DisplayName("build() should return ZipArchiveCreator")
    void testBuild() throws IOException {
        // Given
        var mockZipOut = mock(ZipArchiveOutputStream.class);
        when(builder.buildArchiveOutputStream()).thenReturn(mockZipOut);

        // When
        var creator = builder.build();

        // Then
        assertThat(creator).isNotNull();
        //noinspection resource
        verify(builder).buildArchiveOutputStream();
    }
}
