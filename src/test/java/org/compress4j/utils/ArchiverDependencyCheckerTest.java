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
package org.compress4j.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.compress4j.utils.DependencyCheckerTestConstants.EXPECTED_MESSAGE_BROTLI;
import static org.compress4j.utils.DependencyCheckerTestConstants.EXPECTED_MESSAGE_LZMA;
import static org.compress4j.utils.DependencyCheckerTestConstants.EXPECTED_MESSAGE_XZ;
import static org.compress4j.utils.DependencyCheckerTestConstants.EXPECTED_MESSAGE_ZSTD;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.compress4j.exceptions.MissingArchiveDependencyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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
    void shouldCheckArchiverDependency(String entryName, String message) {
        var exception =
                assertThrows(MissingArchiveDependencyException.class, () -> ArchiverDependencyChecker.check(entryName));

        assertThat(exception).hasMessage(message);
    }

    @Test
    void shouldNotThrowExceptionsForUnchecked() {
        assertDoesNotThrow(() -> ArchiverDependencyChecker.check("entry"));
    }

    @Test
    void shouldCheckBrotli() {
        Executable checkBrotli = ArchiverDependencyChecker::checkBrotli;
        var exception = assertThrows(MissingArchiveDependencyException.class, checkBrotli);

        assertThat(exception).hasMessage(EXPECTED_MESSAGE_BROTLI);
    }

    @Test
    void shouldCheckLZMA() {
        var exception = assertThrows(MissingArchiveDependencyException.class, ArchiverDependencyChecker::checkLZMA);

        assertThat(exception).hasMessage(EXPECTED_MESSAGE_LZMA);
    }

    @Test
    void shouldCheckXZ() {
        var exception = assertThrows(MissingArchiveDependencyException.class, ArchiverDependencyChecker::checkXZ);

        assertThat(exception).hasMessage(EXPECTED_MESSAGE_XZ);
    }

    @Test
    void shouldCheckZstd() {
        var exception = assertThrows(MissingArchiveDependencyException.class, ArchiverDependencyChecker::checkZstd);

        assertThat(exception).hasMessage(EXPECTED_MESSAGE_ZSTD);
    }
}
