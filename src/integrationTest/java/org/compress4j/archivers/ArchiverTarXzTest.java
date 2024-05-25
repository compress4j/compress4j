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
package org.compress4j.archivers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

class ArchiverTarXzTest extends AbstractArchiverTest {

    @Override
    protected Archiver getArchiver() {
        return ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.XZ);
    }

    @Override
    protected File getArchive() {
        return new File(AbstractResourceTest.RESOURCES_DIR, "archive.tar.xz");
    }

    @Test
    void getFilenameExtension_tar_xz_returnsCorrectFilenameExtension() {
        assertThat(getArchiver().getFilenameExtension()).isEqualTo(".tar.xz");
    }
}
