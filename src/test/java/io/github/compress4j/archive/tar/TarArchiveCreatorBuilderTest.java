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
package io.github.compress4j.archive.tar;

import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_POSIX;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.github.compress4j.archive.tar.TarArchiveCreator.TarArchiveCreatorBuilder;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;

class TarArchiveCreatorBuilderTest {

    @Test
    void shouldBuildArchiveOutputStream() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        TarArchiveCreatorBuilder builder = new TarArchiveCreatorBuilder(outputStream)
                .withLongFileMode(LONGFILE_POSIX)
                .withBigNumberMode(BIGNUMBER_POSIX)
                .withBlockSize(1024)
                .withEncoding("UTF-8");
        var spiedBuilder = spy(builder);

        // when
        try (TarArchiveOutputStream out = spy(spiedBuilder.buildArchiveOutputStream())) {

            // then
            assertThat(out)
                    .isNotNull()
                    .extracting("longFileMode", "bigNumberMode", "recordsPerBlock", "charsetName")
                    .containsExactly(LONGFILE_POSIX, BIGNUMBER_POSIX, 2, "UTF-8");
            then(spiedBuilder).should().buildTarArchiveOutputStream(outputStream);
        }
    }
}
