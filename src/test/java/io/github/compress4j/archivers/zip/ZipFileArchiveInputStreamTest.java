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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZipFileArchiveInputStreamTest {

    @Mock
    private ZipFile mockZipFile;

    private ZipFileArchiveInputStream inputStream;

    @BeforeEach
    void setUp() {
        inputStream = new ZipFileArchiveInputStream(mockZipFile);
    }

    @Nested
    @DisplayName("getNextEntry() Tests")
    class GetNextEntryTests {

        @Test
        @DisplayName("Should return null for an empty archive")
        void testGetNextEntry_EmptyArchive() throws IOException {
            // Given
            when(mockZipFile.getEntriesInPhysicalOrder()).thenReturn(Collections.emptyEnumeration());

            // When
            var entry = inputStream.getNextEntry();

            // Then
            assertThat(entry).isNull();
            assertThat(inputStream.getCurrentEntry()).isNull();
            assertThat(inputStream.getCurrentEntryStream()).isNull();
        }

        @Test
        @DisplayName("Should return null on subsequent calls after empty archive")
        void testGetNextEntry_EmptyArchiveSubsequentCalls() throws IOException {
            // Given
            when(mockZipFile.getEntriesInPhysicalOrder()).thenReturn(Collections.emptyEnumeration());

            // When
            inputStream.getNextEntry();
            var entry = inputStream.getNextEntry();

            // Then
            assertThat(entry).isNull();
        }

        @Test
        @DisplayName("Should return single entry and then null")
        void testGetNextEntry_SingleEntryArchive() throws IOException {
            // Given
            var mockEntry = mock(ZipArchiveEntry.class);
            var mockStream = mock(InputStream.class);
            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Collections.singletonList(mockEntry)));
            when(mockZipFile.getInputStream(mockEntry)).thenReturn(mockStream);

            // When: First call
            var entry1 = inputStream.getNextEntry();

            // Then: First call
            assertThat(entry1).isSameAs(mockEntry);
            assertThat(inputStream.getCurrentEntry()).isSameAs(mockEntry);
            assertThat(inputStream.getCurrentEntryStream()).isSameAs(mockStream);

            // When: Second call
            var entry2 = inputStream.getNextEntry();

            // Then: Second call
            assertThat(entry2).isNull();
            assertThat(inputStream.getCurrentEntry()).isNull();
            assertThat(inputStream.getCurrentEntryStream()).isNull();
            verify(mockStream, times(1)).close();
        }

        @Test
        @DisplayName("Should iterate through multiple entries correctly")
        void testGetNextEntry_MultiEntryArchive() throws IOException {
            // Given
            var mockEntry1 = mock(ZipArchiveEntry.class, "entry1");
            var mockEntry2 = mock(ZipArchiveEntry.class, "entry2");
            var mockStream1 = mock(InputStream.class, "stream1");
            var mockStream2 = mock(InputStream.class, "stream2");

            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Arrays.asList(mockEntry1, mockEntry2)));
            when(mockZipFile.getInputStream(mockEntry1)).thenReturn(mockStream1);
            when(mockZipFile.getInputStream(mockEntry2)).thenReturn(mockStream2);

            // When: First call
            var entry1 = inputStream.getNextEntry();
            // Then: First call
            assertThat(entry1).isSameAs(mockEntry1);
            assertThat(inputStream.getCurrentEntry()).isSameAs(mockEntry1);
            assertThat(inputStream.getCurrentEntryStream()).isSameAs(mockStream1);
            verify(mockStream1, never()).close();

            // When: Second call
            var entry2 = inputStream.getNextEntry();
            // Then: Second call
            assertThat(entry2).isSameAs(mockEntry2);
            assertThat(inputStream.getCurrentEntry()).isSameAs(mockEntry2);
            assertThat(inputStream.getCurrentEntryStream()).isSameAs(mockStream2);
            verify(mockStream1, times(1)).close();
            verify(mockStream2, never()).close();

            // When: Third call (end of archive)
            var entry3 = inputStream.getNextEntry();
            // Then: Third call
            assertThat(entry3).isNull();
            assertThat(inputStream.getCurrentEntry()).isNull();
            assertThat(inputStream.getCurrentEntryStream()).isNull();
            verify(mockStream2, times(1)).close();
        }

        @Test
        @DisplayName("Should propagate IOException from getInputStream")
        void testGetNextEntry_GetInputStreamThrowsIOException() throws IOException {
            // Given
            var mockEntry = mock(ZipArchiveEntry.class);
            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Collections.singletonList(mockEntry)));
            when(mockZipFile.getInputStream(mockEntry)).thenThrow(new IOException("Test exception"));

            // When & Then
            assertThatThrownBy(() -> inputStream.getNextEntry()).hasMessage("Test exception");

            assertThat(inputStream.getCurrentEntry()).isSameAs(mockEntry);
            assertThat(inputStream.getCurrentEntryStream()).isNull();
        }

        @Test
        @DisplayName("Should lazily initialize entries enumeration")
        void testGetNextEntry_LazyInitialization() throws IOException {
            // Given
            when(mockZipFile.getEntriesInPhysicalOrder()).thenReturn(Collections.emptyEnumeration());

            // When
            inputStream.getNextEntry();
            inputStream.getNextEntry();

            // Then
            verify(mockZipFile, times(1)).getEntriesInPhysicalOrder();
        }
    }

    @Nested
    @DisplayName("read() Tests")
    class ReadTests {

        @Test
        @DisplayName("Should throw NullPointerException if read is called before getNextEntry")
        void testRead_BeforeNextEntry() {
            // Given
            byte[] buffer = new byte[1024];

            // When & Then
            assertThatThrownBy(() -> inputStream.read(buffer, 0, buffer.length))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should read data correctly from current entry stream")
        void testRead_SuccessfulRead() throws IOException {
            // Given
            byte[] data = "test data".getBytes();
            var dataStream = new ByteArrayInputStream(data);
            var mockEntry = mock(ZipArchiveEntry.class);

            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Collections.singletonList(mockEntry)));
            when(mockZipFile.getInputStream(mockEntry)).thenReturn(dataStream);

            inputStream.getNextEntry();

            // When
            byte[] buffer = new byte[data.length];
            var bytesRead = inputStream.read(buffer, 0, buffer.length);

            // Then
            assertThat(bytesRead).isEqualTo(data.length);
            assertThat(inputStream.getBytesRead()).isEqualTo(data.length);
            assertThat(buffer).isEqualTo(data);
        }

        @Test
        @DisplayName("Should handle partial reads and offsets")
        void testRead_PartialReadWithOffset() throws IOException {
            // Given
            byte[] data = "abcdefghij".getBytes();
            var dataStream = new ByteArrayInputStream(data);
            var mockEntry = mock(ZipArchiveEntry.class);

            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Collections.singletonList(mockEntry)));
            when(mockZipFile.getInputStream(mockEntry)).thenReturn(dataStream);

            inputStream.getNextEntry();

            byte[] buffer = new byte[20];
            Arrays.fill(buffer, (byte) 'x');

            // When: Read 3 bytes ("abc") into offset 2
            var bytesRead1 = inputStream.read(buffer, 2, 3);

            // Then:
            assertThat(bytesRead1).isEqualTo(3);
            assertThat(inputStream.getBytesRead()).isEqualTo(3);
            assertThat(buffer).startsWith("xxabc".getBytes());

            // When: Read next 5 bytes ("defgh") into offset 0
            var bytesRead2 = inputStream.read(buffer, 0, 5);

            // Then:
            assertThat(bytesRead2).isEqualTo(5);
            assertThat(inputStream.getBytesRead()).isEqualTo(8);

            assertThat(buffer).startsWith("defghxx".getBytes());
        }

        @Test
        @DisplayName("Should return -1 and close stream on EOF")
        void testRead_ReachesEOF() throws IOException {
            // Given
            var mockEntry = mock(ZipArchiveEntry.class);
            var mockStream = mock(InputStream.class);
            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Collections.singletonList(mockEntry)));
            when(mockZipFile.getInputStream(mockEntry)).thenReturn(mockStream);
            when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);

            inputStream.getNextEntry();

            // When
            var bytesRead = inputStream.read(new byte[10], 0, 10);

            // Then
            assertThat(bytesRead).isEqualTo(-1);
            assertThat(inputStream.getBytesRead()).isZero();
            verify(mockStream, times(1)).close();
        }

        @Test
        @DisplayName("Should propagate IOException from stream read")
        void testRead_ThrowsIOException() throws IOException {
            // Given
            var mockEntry = mock(ZipArchiveEntry.class);
            var mockStream = mock(InputStream.class);
            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Collections.singletonList(mockEntry)));
            when(mockZipFile.getInputStream(mockEntry)).thenReturn(mockStream);
            when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Read error"));

            inputStream.getNextEntry();

            // When & Then
            assertThatThrownBy(() -> inputStream.read(new byte[10], 0, 10))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Read error");

            verify(mockStream, never()).close();
        }
    }

    @Nested
    @DisplayName("close() Tests")
    class CloseTests {

        @Test
        @DisplayName("Should close the underlying ZipFile when no entry is active")
        void testClose_ClosesFile() throws IOException {
            // When
            inputStream.close();

            // Then
            verify(mockZipFile, times(1)).close();
        }

        @Test
        @DisplayName("Should close active entry stream and ZipFile")
        void testClose_ClosesCurrentStreamAndFile() throws IOException {
            // Given
            var mockEntry = mock(ZipArchiveEntry.class);
            var mockStream = mock(InputStream.class);
            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Collections.singletonList(mockEntry)));
            when(mockZipFile.getInputStream(mockEntry)).thenReturn(mockStream);
            inputStream.getNextEntry(); // Activates mockStream

            // When
            inputStream.close();

            // Then
            verify(mockStream, times(1)).close();
            verify(mockZipFile, times(1)).close();
        }

        @Test
        @DisplayName("Should close ZipFile even if entry stream close throws IOException")
        void testClose_QuietlyClosesStream() throws IOException {
            // Given
            var mockEntry = mock(ZipArchiveEntry.class);
            var mockStream = mock(InputStream.class);
            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Collections.singletonList(mockEntry)));
            when(mockZipFile.getInputStream(mockEntry)).thenReturn(mockStream);
            doThrow(new IOException("Stream close error")).when(mockStream).close();
            inputStream.getNextEntry();

            // When & Then
            assertThatCode(() -> inputStream.close()).doesNotThrowAnyException();

            verify(mockZipFile, times(1)).close();
        }

        @Test
        @DisplayName("Should not throw even if ZipFile close throws IOException")
        void testClose_QuietlyClosesFile() throws IOException {
            // Given
            doThrow(new IOException("File close error")).when(mockZipFile).close();

            // When & Then
            assertThatCode(() -> inputStream.close()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should not throw even if both stream and ZipFile close throw IOException")
        void testClose_QuietlyClosesBoth() throws IOException {
            // Given
            var mockEntry = mock(ZipArchiveEntry.class);
            var mockStream = mock(InputStream.class);
            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Collections.singletonList(mockEntry)));
            when(mockZipFile.getInputStream(mockEntry)).thenReturn(mockStream);
            doThrow(new IOException("Stream close error")).when(mockStream).close();
            doThrow(new IOException("File close error")).when(mockZipFile).close();
            inputStream.getNextEntry();

            // When & Then
            assertThatCode(() -> inputStream.close()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Other Public Methods")
    class OtherMethodsTests {

        @Test
        @DisplayName("getUnixSymlink should delegate to ZipFile")
        void testGetUnixSymlink_Delegates() throws IOException {
            // Given
            var linkTarget = "/path/to/target";
            var mockEntry = mock(ZipArchiveEntry.class);
            when(mockZipFile.getUnixSymlink(mockEntry)).thenReturn(linkTarget);

            // When
            var result = inputStream.getUnixSymlink(mockEntry);

            // Then
            assertThat(result).isEqualTo(linkTarget);
            verify(mockZipFile, times(1)).getUnixSymlink(mockEntry);
        }

        @Test
        @DisplayName("getUnixSymlink should propagate IOException")
        void testGetUnixSymlink_ThrowsIOException() throws IOException {
            // Given
            var mockEntry = mock(ZipArchiveEntry.class);
            when(mockZipFile.getUnixSymlink(mockEntry)).thenThrow(new IOException("Symlink error"));

            // When & Then
            assertThatThrownBy(() -> inputStream.getUnixSymlink(mockEntry)).hasMessage("Symlink error");
        }

        @Test
        @DisplayName("canReadEntryData should be correct based on current entry")
        void testCanReadEntryData() throws IOException {
            // Given
            var mockEntry1 = mock(ZipArchiveEntry.class, "entry1");
            var mockEntry2 = mock(ZipArchiveEntry.class, "entry2");
            var mockStream = mock(InputStream.class);
            var differentEntryType = mock(ArchiveEntry.class, "otherType");

            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Collections.singletonList(mockEntry1)));
            when(mockZipFile.getInputStream(mockEntry1)).thenReturn(mockStream);

            // When: Initial state
            // Then:
            assertThat(inputStream.canReadEntryData(mockEntry1)).isFalse();
            assertThat(inputStream.canReadEntryData(null)).isTrue();

            // When: First entry is active
            inputStream.getNextEntry();
            // Then:
            assertThat(inputStream.canReadEntryData(mockEntry1)).isTrue();
            assertThat(inputStream.canReadEntryData(mockEntry2)).isFalse();
            assertThat(inputStream.canReadEntryData(differentEntryType)).isFalse();
            assertThat(inputStream.canReadEntryData(null)).isFalse();

            // When: Archive is exhausted
            inputStream.getNextEntry();
            // Then:
            assertThat(inputStream.canReadEntryData(mockEntry1)).isFalse();
            assertThat(inputStream.canReadEntryData(null)).isTrue();
        }

        @Test
        @DisplayName("Getters should return correct initial state")
        void testGetters_InitialState() {
            assertThat(inputStream.getCurrentEntry()).isNull();
            assertThat(inputStream.getCurrentEntryStream()).isNull();
        }

        @Test
        @DisplayName("Getters should return correct state after getNextEntry")
        void testGetters_AfterGetNextEntry() throws IOException {
            // Given
            var mockEntry = mock(ZipArchiveEntry.class);
            var mockStream = mock(InputStream.class);
            when(mockZipFile.getEntriesInPhysicalOrder())
                    .thenReturn(Collections.enumeration(Collections.singletonList(mockEntry)));
            when(mockZipFile.getInputStream(mockEntry)).thenReturn(mockStream);

            // When
            inputStream.getNextEntry();

            // Then
            assertThat(inputStream.getCurrentEntry()).isSameAs(mockEntry);
            assertThat(inputStream.getCurrentEntryStream()).isSameAs(mockStream);
        }
    }
}
