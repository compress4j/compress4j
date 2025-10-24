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
package io.github.compress4j.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Utility class for file operations. */
public class FileUtils {
    /** Private constructor to prevent instantiation. */
    private FileUtils() {
        // No-op
    }

    /** DOS read-only attribute. */
    public static final int DOS_READ_ONLY = 0b01;

    /** DOS hidden attribute. */
    public static final int DOS_HIDDEN = 0b010;

    /** Signifies no mode set on file. */
    public static final int NO_MODE = 0;

    /**
     * Reads the full content of a file into a single string, normalizing all line endings to a single newline character
     * ('\n').
     *
     * <p>This method ensures that file content comparisons in tests will work consistently across different operating
     * systems (like Windows and Linux) that use different line ending characters.
     *
     * @param path the path to the file to read.
     * @return a string containing the file's content with normalized line endings.
     * @throws IOException if an I/O error occurs reading from the file.
     */
    public static String readStringNormalized(Path path) throws IOException {
        return String.join("\n", Files.readAllLines(path, StandardCharsets.UTF_8));
    }
}
