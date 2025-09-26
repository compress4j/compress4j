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

/**
 * Utility class providing specialized string manipulation methods for the compression library. This class contains
 * methods for trimming specific characters from the beginning or end of strings, which is particularly useful for path
 * normalization in archive processing.
 */
public class StringUtil {

    private StringUtil() {}

    /**
     * Trims the trailing characters from the given string.
     *
     * @param s the string to trim
     * @param c the character to trim
     * @return the trimmed string
     */
    public static String trimTrailing(String s, char c) {
        return s.replaceAll("(?!^)" + c + "+$", "");
    }

    /**
     * Trims the leading characters from the given string.
     *
     * @param s the string to trim
     * @param c the character to trim
     * @return the trimmed string
     */
    public static String trimLeading(String s, char c) {
        return s.replaceAll("^" + c + "+(?!$)", "");
    }
}
