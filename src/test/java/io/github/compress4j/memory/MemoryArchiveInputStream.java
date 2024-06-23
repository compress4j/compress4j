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
package io.github.compress4j.memory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.IOUtils;

/** A test input stream. */
public final class MemoryArchiveInputStream extends ArchiveInputStream<MemoryArchiveEntry> {

    public static final String INNER_DELIMITER = ";";
    public static final String OUTER_DELIMITER = ":";
    private final String[] fileNames;
    private final String[] content;
    private int pointer;

    public MemoryArchiveInputStream(final InputStream inputStream) throws IOException {
        this(toArray(IOUtils.toString(inputStream, StandardCharsets.UTF_8)));
    }

    public MemoryArchiveInputStream(final String[][] pFiles) {
        final int pFilesLength = pFiles.length;
        fileNames = new String[pFilesLength];
        content = new String[pFilesLength];

        for (int i = 0; i < pFilesLength; i++) {
            final String[] nameAndContent = pFiles[i];
            fileNames[i] = nameAndContent[0];
            content[i] = nameAndContent[1];
        }
        pointer = 0;
    }

    public static InputStream toInputStream(final String[][] pFiles) {
        return new ByteArrayInputStream(join(pFiles).getBytes(StandardCharsets.UTF_8));
    }

    private static String[][] toArray(final String str) {
        String[] strings = str.split(OUTER_DELIMITER);
        String[][] result = new String[strings.length][];
        for (int i = 0; i < strings.length; i++) {
            result[i] = strings[i].split(INNER_DELIMITER);
        }
        return result;
    }

    private static String join(final String[][] pFiles) {
        return Arrays.stream(pFiles)
                .map(col -> String.join(INNER_DELIMITER, col))
                .collect(Collectors.joining(OUTER_DELIMITER));
    }

    @Override
    public MemoryArchiveEntry getNextEntry() {
        if (pointer >= fileNames.length) {
            return null;
        }
        String fileName = fileNames[pointer];
        pointer++;
        return new MemoryArchiveEntry(fileName);
    }

    @Override
    public int read() {
        return 0;
    }

    public String readString() {
        int currentEntry = pointer - 1; // pointer has already been incremented to the next entry
        return content[currentEntry];
    }
}
