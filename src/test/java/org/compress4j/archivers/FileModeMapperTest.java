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
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileModeMapperTest {

    @Mock
    ArchiveEntry archiveEntry;

    private static void setIsPosix(@SuppressWarnings("SameParameterValue") boolean isPosix) {
        try {
            Field field = ReflectionUtils.findFields(
                            FileModeMapper.class,
                            f -> f.getName().equals("IS_POSIX"),
                            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                    .get(0);
            field.setAccessible(true);
            field.setBoolean(null, isPosix);
        } catch (Exception e) {
            fail("Failed to set IS_POSIX field: " + e.getMessage());
        }
    }

    @Test
    @Disabled
    void shouldCreateForNonPosix() {
        setIsPosix(false);
        var fileModeMapper = FileModeMapper.create(archiveEntry);

        assertThat(fileModeMapper).isNotNull().isOfAnyClassIn(FileModeMapper.FallbackFileModeMapper.class);
    }

    @Test
    void shouldCreateForPosix() {
        var fileModeMapper = FileModeMapper.create(archiveEntry);

        assertThat(fileModeMapper).isNotNull().isOfAnyClassIn(FileModeMapper.PosixPermissionMapper.class);
    }
}
