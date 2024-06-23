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
package io.github.compress4j.archive;

import static io.github.compress4j.archive.ArchiveType.TAR;
import static io.github.compress4j.archive.ArchiveType.TAR_GZ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.compress4j.archive.tar.TarArchiveCreator.TarArchiveCreatorBuilder;
import io.github.compress4j.archive.tar.TarArchiveExtractor.TarArchiveExtractorBuilder;
import io.github.compress4j.archive.tar.TarGzArchiveCreator.TarGzArchiveCreatorBuilder;
import io.github.compress4j.archive.tar.TarGzArchiveExtractor.TarGzArchiveExtractorBuilder;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ArchiveFactoryTest {

    private static Stream<Arguments> archiveCreatorProvider() {
        return Stream.of(
                Arguments.of(TAR, TarArchiveCreatorBuilder.class),
                Arguments.of(TAR_GZ, TarGzArchiveCreatorBuilder.class));
    }

    private static Stream<Arguments> archiveExtractorProvider() {
        return Stream.of(
                Arguments.of(TAR, TarArchiveExtractorBuilder.class),
                Arguments.of(TAR_GZ, TarGzArchiveExtractorBuilder.class));
    }

    @ParameterizedTest
    @MethodSource("archiveCreatorProvider")
    void shouldCreateArchiveCreatorFromPath(ArchiveType archiveType, Class<?> expectedClass) {
        // given
        var outputStream = mock(OutputStream.class);

        // when
        var archiveCreatorBuilder = ArchiveFactory.creator(archiveType, outputStream);

        // then
        assertThat(archiveCreatorBuilder).isInstanceOf(expectedClass);
    }

    @ParameterizedTest
    @MethodSource("archiveExtractorProvider")
    void shouldCreateArchiveExtractorFromPath(ArchiveType archiveType, Class<?> expectedClass) {
        // given
        var inputStream = mock(InputStream.class);

        // when
        var decompressor = ArchiveFactory.extractor(archiveType, inputStream);

        // then
        assertThat(decompressor).isInstanceOf(expectedClass);
    }
}
