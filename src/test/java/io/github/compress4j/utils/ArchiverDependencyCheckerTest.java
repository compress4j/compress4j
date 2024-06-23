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

import static io.github.compress4j.utils.DependencyCheckerTestConstants.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.github.compress4j.exceptions.MissingArchiveDependencyException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ArchiverDependencyCheckerTest {

    private static Stream<Arguments> individualCheckers() {
        return Stream.of(
                Arguments.of("br", EXPECTED_MESSAGE_BROTLI),
                Arguments.of("lzma", EXPECTED_MESSAGE_LZMA),
                Arguments.of("xz", EXPECTED_MESSAGE_XZ),
                Arguments.of("zstd", EXPECTED_MESSAGE_ZSTD));
    }

    @ParameterizedTest
    @MethodSource("individualCheckers")
    void shouldCheckArchiverDependency(String entryName, String expectedMessage) {
        assertThatThrownBy(() -> ArchiverDependencyChecker.check(entryName))
                .isInstanceOf(MissingArchiveDependencyException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    void shouldNotThrowExceptionsForUnchecked() {
        assertDoesNotThrow(() -> ArchiverDependencyChecker.check("entry"));
    }

    @Test
    void shouldCheckBrotli() {
        assertThatThrownBy(ArchiverDependencyChecker::checkBrotli)
                .isInstanceOf(MissingArchiveDependencyException.class)
                .hasMessage(EXPECTED_MESSAGE_BROTLI);
    }

    @Test
    void shouldCheckLZMA() {
        assertThatThrownBy(ArchiverDependencyChecker::checkLZMA)
                .isInstanceOf(MissingArchiveDependencyException.class)
                .hasMessage(EXPECTED_MESSAGE_LZMA);
    }

    @Test
    void shouldCheckXZ() {
        assertThatThrownBy(ArchiverDependencyChecker::checkXZ)
                .isInstanceOf(MissingArchiveDependencyException.class)
                .hasMessage(EXPECTED_MESSAGE_XZ);
    }

    @Test
    void shouldCheckZstd() {
        assertThatThrownBy(ArchiverDependencyChecker::checkZstd)
                .isInstanceOf(MissingArchiveDependencyException.class)
                .hasMessage(EXPECTED_MESSAGE_ZSTD);
    }
}
