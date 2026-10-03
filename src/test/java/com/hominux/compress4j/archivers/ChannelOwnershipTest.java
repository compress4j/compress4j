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
package com.hominux.compress4j.archivers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hominux.compress4j.archivers.tar.TarArchiveCreator;
import com.hominux.compress4j.archivers.tar.TarArchiveExtractor;
import com.hominux.compress4j.archivers.tar.TarGzArchiveExtractor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.junit.jupiter.api.Test;

class ChannelOwnershipTest {

    @Test
    void closingTheExtractorClosesTheChannel() throws IOException {
        // Given
        var bytes = new SeekableInMemoryByteChannel();
        try (var creator = TarArchiveCreator.builder(bytes).build()) {
            creator.addFile("a", "1".getBytes(StandardCharsets.UTF_8));
        }
        var channel = new SeekableInMemoryByteChannel(bytes.array());

        // When
        try (var extractor = TarArchiveExtractor.builder(channel).build()) {
            extractor.stream().toList();
        }

        // Then
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    void failedBuildLeavesTheCallersChannelOpen() {
        // Given
        var garbage = new SeekableInMemoryByteChannel("not gzip".getBytes(StandardCharsets.UTF_8));

        // Then
        assertThatThrownBy(() -> TarGzArchiveExtractor.builder(garbage).build()).isInstanceOf(IOException.class);
        assertThat(garbage.isOpen()).isTrue();
    }
}
