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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.compress4j.archivers.zip.ZipArchiveCreator.ZipArchiveCreatorBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZipArchiveCreatorTest {

    @Mock
    private ZipArchiveOutputStream mockZipStream;

    @Mock
    private ZipArchiveCreatorBuilder mockBuilder;

    private ZipArchiveCreator creator;

    @Captor
    private ArgumentCaptor<ZipArchiveEntry> entryCaptor;

    @Captor
    private ArgumentCaptor<byte[]> bytesCaptor;

    @Captor
    private ArgumentCaptor<Integer> intCaptor;

    private static final int NO_MODE = -1;

    @BeforeEach
    void setUp() {
        creator = new ZipArchiveCreator(mockZipStream);
    }

    @Test
    @DisplayName("Constructor with builder should build stream")
    void testConstructorWithBuilder() throws IOException {
        // Given
        when(mockBuilder.buildArchiveOutputStream()).thenReturn(mockZipStream);

        // When
        var creatorFromBuilder = new ZipArchiveCreator(mockBuilder);

        // Then
        assertThat(creatorFromBuilder).isNotNull();
        //noinspection resource
        verify(mockBuilder).buildArchiveOutputStream();
    }

    @Test
    @DisplayName("close() should close the underlying stream")
    void testClose() throws IOException {
        // When
        creator.close();

        // Then
        verify(mockZipStream).close();
    }

    @SuppressWarnings("OctalInteger")
    @Nested
    @DisplayName("Entry Writing Tests")
    class EntryWritingTests {

        private final FileTime testTime = FileTime.fromMillis(123456789000L);

        @Test
        @DisplayName("writeDirectoryEntry should write a correct directory entry")
        void testWriteDirectoryEntry() throws IOException {
            // When
            creator.writeDirectoryEntry("testDir", testTime);

            // Then
            var inOrder = inOrder(mockZipStream);
            inOrder.verify(mockZipStream).putArchiveEntry(entryCaptor.capture());
            inOrder.verify(mockZipStream).closeArchiveEntry();

            var entry = entryCaptor.getValue();
            assertThat(entry.getName()).isEqualTo("testDir/");
            assertThat(entry.isDirectory()).isTrue();
            assertThat(entry.getTime()).isEqualTo(testTime.toMillis());
        }

        @Test
        @DisplayName("writeFileEntry should write a correct file entry")
        void testWriteFileEntry() throws IOException {
            // Given
            byte[] data = "test data".getBytes();
            var dataStream = new ByteArrayInputStream(data);
            var size = data.length;
            var mode = 0644;

            // When
            creator.writeFileEntry("testFile.txt", dataStream, size, testTime, mode, Optional.empty());

            // Then
            var inOrder = inOrder(mockZipStream);
            inOrder.verify(mockZipStream).putArchiveEntry(entryCaptor.capture());

            inOrder.verify(mockZipStream).write(bytesCaptor.capture(), intCaptor.capture(), intCaptor.capture());

            inOrder.verify(mockZipStream).closeArchiveEntry();

            var entry = entryCaptor.getValue();
            assertThat(entry.getName()).isEqualTo("testFile.txt");
            assertThat(entry.isDirectory()).isFalse();
            assertThat(entry.getSize()).isEqualTo(size);
            assertThat(entry.getTime()).isEqualTo(testTime.toMillis());
            assertThat(entry.getUnixMode()).isEqualTo(mode);

            byte[] writtenBytes = bytesCaptor.getValue();
            var offset = intCaptor.getAllValues().get(0);
            var length = intCaptor.getAllValues().get(1);

            byte[] actualData = new byte[length];
            System.arraycopy(writtenBytes, offset, actualData, 0, length);

            assertThat(actualData).isEqualTo(data);
        }

        @Test
        @DisplayName("writeFileEntry should write file entry with no mode")
        void testWriteFileEntry_NoMode() throws IOException {
            // Given
            byte[] data = "test data".getBytes();
            var dataStream = new ByteArrayInputStream(data);
            var size = data.length;

            // When
            creator.writeFileEntry("testFile.txt", dataStream, size, testTime, NO_MODE, Optional.empty());

            // Then
            verify(mockZipStream).putArchiveEntry(entryCaptor.capture());

            var entry = entryCaptor.getValue();
            assertThat(entry.getName()).isEqualTo("testFile.txt");
            assertThat(entry.getSize()).isEqualTo(size);
            assertThat(entry.getUnixMode()).isEqualTo(65535);
        }

        @Test
        @DisplayName("writeFileEntry should write a symlink as a file containing the target path")
        void testWriteFileEntry_Symlink() throws IOException {
            // Given
            var targetPath = Paths.get("../target.txt");
            byte[] targetBytes = targetPath.toString().getBytes();
            var size = targetBytes.length;
            var mode = 0777;
            var dataStream = new ByteArrayInputStream(new byte[0]);

            // When
            creator.writeFileEntry("testLink.lnk", dataStream, size, testTime, mode, Optional.of(targetPath));

            // Then
            var inOrder = inOrder(mockZipStream);
            inOrder.verify(mockZipStream).putArchiveEntry(entryCaptor.capture());
            inOrder.verify(mockZipStream).write(bytesCaptor.capture());
            inOrder.verify(mockZipStream).closeArchiveEntry();

            var entry = entryCaptor.getValue();
            assertThat(entry.getName()).isEqualTo("testLink.lnk");
            assertThat(entry.isDirectory()).isFalse();
            assertThat(entry.getSize()).isEqualTo(size); // Size is the length of the path string
            assertThat(entry.getTime()).isEqualTo(testTime.toMillis());
            assertThat(entry.getUnixMode()).isEqualTo(mode);
            assertThat(bytesCaptor.getValue()).isEqualTo(targetBytes);
        }
    }
}
