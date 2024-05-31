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

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IOUtilsTest {

    private static Stream<Arguments> entryNames() {
        return Stream.of(
                Arguments.of("../../../file.txt", "file.txt"),
                Arguments.of("/../../../file.txt", "file.txt"),
                Arguments.of("path/../../../file.txt", "file.txt"),
                Arguments.of("/path/../../../file.txt", "file.txt"),
                Arguments.of("path/sub/../file.txt", "path/file.txt"));
    }

    @ParameterizedTest
    @MethodSource("entryNames")
    void shouldCleanEntryName(String entryName, String expected) {
        assertThat(IOUtils.cleanEntryName(entryName)).isEqualTo(expected);
    }
}