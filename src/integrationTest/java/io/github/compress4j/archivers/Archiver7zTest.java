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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import org.apache.commons.compress.archivers.StreamingNotSupportedException;
import org.junit.jupiter.api.Test;

public class Archiver7zTest extends AbstractArchiverTest {

    @Override
    protected Archiver getArchiver() {
        return ArchiverFactory.createArchiver(ArchiveFormat.SEVEN_Z);
    }

    @Override
    protected File getArchive() {
        return new File(AbstractResourceTest.RESOURCES_DIR, "archive.7z");
    }

    @Test
    public void extract_properlyExtractsArchiveStream() {
        // 7z does not allow streaming
        IOException exception = assertThrows(IOException.class, super::extract_properlyExtractsArchiveStream);
        assertThat(exception).hasCauseInstanceOf(StreamingNotSupportedException.class);
        assertThat(exception.getCause()).hasMessage("The 7z doesn't support streaming.");
    }
}
