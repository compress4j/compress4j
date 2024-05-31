/*
 * Copyright 2024 The Compress4J Project
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
package io.github.compress4j.archivers;

import static io.github.compress4j.archivers.AbstractCompressorTest.COMPRESS_TXT;
import static io.github.compress4j.utils.DependencyCheckerTestConstants.EXPECTED_MESSAGE_XZ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.github.compress4j.exceptions.MissingArchiveDependencyException;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("java:S5778")
class CompressorXzDependencyCheckerTest {

    @TempDir
    protected File archiveTmpDir;

    @Test
    void shouldCheckCompressorXZDependency() throws IOException {
        try {
            new CommonsCompressor(CompressionType.XZ)
                    .compress(COMPRESS_TXT, new File(archiveTmpDir, "compress.txt.xz"));
            fail("Expected MissingArchiveDependencyException");
        } catch (MissingArchiveDependencyException e) {
            assertThat(e).hasMessage(EXPECTED_MESSAGE_XZ);
        }
    }
}
