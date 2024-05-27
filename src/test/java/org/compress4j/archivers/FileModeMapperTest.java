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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

import java.nio.file.FileSystem;
import java.util.Set;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileModeMapperTest {

    @Mock
    private ArchiveEntry archiveEntry;

    @Mock
    private FileSystem fileSystem;

    @Test
    void shouldCreateForNonPosix() {
        given(fileSystem.supportedFileAttributeViews()).willReturn(Set.of());
        var fileModeMapper = FileModeMapper.create(fileSystem, archiveEntry);

        assertThat(fileModeMapper).isNotNull().isOfAnyClassIn(FileModeMapper.FallbackFileModeMapper.class);
        assertDoesNotThrow(() -> fileModeMapper.map(null));
    }

    @Test
    void shouldCreateForPosix() {
        given(fileSystem.supportedFileAttributeViews()).willReturn(Set.of("posix"));

        var fileModeMapper = FileModeMapper.create(fileSystem, archiveEntry);

        assertThat(fileModeMapper).isNotNull().isOfAnyClassIn(FileModeMapper.PosixPermissionMapper.class);
    }
}
