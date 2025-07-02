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
package io.github.compress4j.archivers.tar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.github.compress4j.archivers.tar.TarArchiveExtractor.TarArchiveExtractorBuilder;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

class TarArchiveExtractorBuilderTest {

    @Test
    void shouldBuildArchiveInputStream() throws IOException {
        // given
        var inputStream = mock(InputStream.class);
        TarArchiveExtractorBuilder builder = new TarArchiveExtractorBuilder(inputStream);

        // when
        try (TarArchiveInputStream out = spy(builder.buildArchiveInputStream())) {

            // then
            assertThat(out).isNotNull();
        }
    }
}
