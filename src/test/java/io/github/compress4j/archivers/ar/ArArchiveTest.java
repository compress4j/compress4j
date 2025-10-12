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
package io.github.compress4j.archivers.ar;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Test for AR archive functionality */
class ArArchiveTest {

    @Test
    void testCreateAndExtractArArchive(@TempDir Path tempDir) throws IOException {
        // Create an AR archive in memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ArArchiveCreator creator = ArArchiveCreator.builder(baos).build()) {
            // Add a simple text file
            String content = "Hello, AR archive!";
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

            creator.addFile("test.txt", new ByteArrayInputStream(contentBytes), FileTime.from(Instant.now()));
        }

        // Extract the AR archive to temporary directory
        byte[] archiveBytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(archiveBytes);

        try (ArArchiveExtractor extractor = ArArchiveExtractor.builder(bais).build()) {
            extractor.extract(tempDir);
        }

        // Verify the extracted file
        Path extractedFile = tempDir.resolve("test.txt");
        assertThat(extractedFile).exists();

        String extractedContent = Files.readString(extractedFile, StandardCharsets.UTF_8);
        assertThat(extractedContent).isEqualTo("Hello, AR archive!");
    }
}
