/*
 * Copyright 2026 The Compress4J Project
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
package io.github.compress4j.archivers.arj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.compress4j.archivers.ArchiveExtractor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.apache.commons.compress.archivers.arj.ArjArchiveEntry;
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArjArchiveExtractorTest {

    private static ArjArchiveInputStream streamOf(ArjArchiveEntry entry) throws IOException {
        var in = mock(ArjArchiveInputStream.class);
        when(in.getNextEntry()).thenReturn(entry);
        return in;
    }

    @Test
    void shouldReturnEmptyWhenNoMoreEntries() throws IOException {
        try (var extractor = new ArjArchiveExtractor(mock(ArjArchiveInputStream.class))) {
            assertThat(extractor.nextEntry()).isEmpty();
        }
    }

    @Test
    void shouldMapDirectoryEntry() throws IOException {
        var entry = mock(ArjArchiveEntry.class);
        when(entry.getName()).thenReturn("dir");
        when(entry.isDirectory()).thenReturn(true);

        try (var extractor = new ArjArchiveExtractor(streamOf(entry))) {
            assertThat(extractor.nextEntry())
                    .hasValueSatisfying(e -> assertThat(e.type()).isEqualTo(ArchiveExtractor.Entry.Type.DIR));
        }
    }

    @Test
    void shouldMapFileEntryAndKeepUnixTypeAndPermissionBits() throws IOException {
        var entry = mock(ArjArchiveEntry.class);
        when(entry.getName()).thenReturn("file.txt");
        when(entry.isHostOsUnix()).thenReturn(true);
        when(entry.getUnixMode()).thenReturn(0100640);

        try (var extractor = new ArjArchiveExtractor(streamOf(entry))) {
            assertThat(extractor.nextEntry()).hasValueSatisfying(e -> {
                assertThat(e.type()).isEqualTo(ArchiveExtractor.Entry.Type.FILE);
                assertThat(e.mode()).isEqualTo(0100640);
            });
        }
    }

    @Test
    void shouldLetFiltersDetectASymlinkThroughTheModeTypeBits() throws IOException {
        var entry = mock(ArjArchiveEntry.class);
        when(entry.getName()).thenReturn("link");
        when(entry.isHostOsUnix()).thenReturn(true);
        when(entry.getUnixMode()).thenReturn(0120777);

        try (var extractor = new ArjArchiveExtractor(streamOf(entry))) {
            assertThat(extractor.nextEntry())
                    .hasValueSatisfying(e -> assertThat(e.mode() & 0170000).isEqualTo(0120000));
        }
    }

    @Test
    void shouldRejectUnixSymlinkEntryWithoutWritingAnything() throws IOException {
        var entry = mock(ArjArchiveEntry.class);
        when(entry.getName()).thenReturn("link");
        when(entry.isHostOsUnix()).thenReturn(true);
        when(entry.getUnixMode()).thenReturn(0120777);

        try (var extractor = new ArjArchiveExtractor(streamOf(entry))) {
            var mapped = extractor.nextEntry().orElseThrow();

            assertThatThrownBy(() -> extractor.openEntryStream(mapped))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Unsupported ARJ entry type: symlink: link");
        }
    }

    @Test
    void shouldRejectEntryItCannotRead() throws IOException {
        var entry = mock(ArjArchiveEntry.class);
        when(entry.getName()).thenReturn("secret.txt");
        var in = streamOf(entry);
        when(in.canReadEntryData(entry)).thenReturn(false);

        try (var extractor = new ArjArchiveExtractor(in)) {
            var mapped = extractor.nextEntry().orElseThrow();

            assertThatThrownBy(() -> extractor.openEntryStream(mapped))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Cannot read ARJ entry data (encrypted or unsupported method): secret.txt");
        }
    }

    @Test
    void shouldOpenReadableEntry() throws IOException {
        var entry = mock(ArjArchiveEntry.class);
        when(entry.getName()).thenReturn("ok.txt");
        var in = streamOf(entry);
        when(in.canReadEntryData(entry)).thenReturn(true);

        try (var extractor = new ArjArchiveExtractor(in)) {
            var mapped = extractor.nextEntry().orElseThrow();

            assertThat(extractor.openEntryStream(mapped)).isSameAs(in);
        }
    }

    @TempDir
    Path tempDir;

    @Test
    void shouldReportUnsupportedEntryToTheErrorHandlerAndCreateNothing() throws IOException {
        var entry = mock(ArjArchiveEntry.class);
        when(entry.getName()).thenReturn("link");
        when(entry.isHostOsUnix()).thenReturn(true);
        when(entry.getUnixMode()).thenReturn(0120777);
        var in = mock(ArjArchiveInputStream.class);
        when(in.getNextEntry()).thenReturn(entry).thenReturn(null);
        when(in.canReadEntryData(entry)).thenReturn(true);
        when(in.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        var seen = new ArrayList<String>();

        try (var extractor = new ArjArchiveExtractor(in)) {
            extractor.setErrorHandler((e, failure) -> {
                seen.add(e.name());
                return ArchiveExtractor.ErrorHandlerChoice.SKIP;
            });
            extractor.extract(tempDir);
        }

        assertThat(seen).containsExactly("link");
        assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void shouldRejectPathTraversalAndWriteNothingOutside() throws IOException {
        var entry = mock(ArjArchiveEntry.class);
        when(entry.getName()).thenReturn("../escape.txt");
        var in = mock(ArjArchiveInputStream.class);
        when(in.getNextEntry()).thenReturn(entry).thenReturn(null);
        when(in.canReadEntryData(entry)).thenReturn(true);
        var target = Files.createDirectory(tempDir.resolve("target"));

        try (var extractor = new ArjArchiveExtractor(in)) {
            assertThatThrownBy(() -> extractor.extract(target)).isInstanceOf(IOException.class);
        }

        assertThat(tempDir.resolve("escape.txt")).doesNotExist();
    }
}
