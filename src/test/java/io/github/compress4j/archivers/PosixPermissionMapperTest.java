/*
 * Copyright 2024-2025 The Compress4J Project
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

import static java.nio.file.Files.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.github.compress4j.archivers.FileModeMapper.PosixPermissionMapper;
import io.github.compress4j.test.util.MemoryAppender;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@SuppressWarnings("OctalInteger")
@ExtendWith(MockitoExtension.class)
class PosixPermissionMapperTest {

    private static final String LOGGER_NAME = FileModeMapper.class.getPackageName();
    private static MemoryAppender memoryAppender;

    @Mock
    private TarArchiveEntry archiveEntry;

    @TempDir
    private Path tmpDir;

    @BeforeEach
    void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @AfterEach
    void cleanUp() {
        memoryAppender.reset();
        memoryAppender.stop();
    }

    @Test
    void shouldChangeFilePermissions() throws IOException {
        // given
        given(archiveEntry.getMode()).willReturn(0744);

        var tmpFile = Paths.get(tmpDir.toString(), "file");
        write(tmpFile, new byte[0]);

        Set<PosixFilePermission> perms =
                Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ);

        Files.setPosixFilePermissions(tmpFile, perms);
        assertThat(tmpFile).isReadable();

        // when
        new PosixPermissionMapper<>(archiveEntry).map(tmpFile.toFile());

        // then
        assertThat(tmpFile).isReadable().isWritable().isExecutable();
    }

    @Test
    void shouldLogWhenFailed() throws IOException {
        // given
        given(archiveEntry.getMode()).willReturn(0744);
        var tmpFile = Paths.get(tmpDir.toString(), "failingFile").toFile();

        // when
        new PosixPermissionMapper<>(archiveEntry).map(tmpFile);

        // then
        assertThat(memoryAppender.contains("Could not set file permissions of failingFile", Level.WARN))
                .isTrue();
    }
}
