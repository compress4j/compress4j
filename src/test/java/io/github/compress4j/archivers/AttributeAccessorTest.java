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
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.arj.ArjArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttributeAccessorTest {

    private static Stream<Arguments> entryNames() {
        return Stream.of(
                Arguments.of(mock(ArArchiveEntry.class), AttributeAccessor.ArAttributeAccessor.class),
                Arguments.of(mock(ArjArchiveEntry.class), AttributeAccessor.ArjAttributeAccessor.class),
                Arguments.of(mock(CpioArchiveEntry.class), AttributeAccessor.CpioAttributeAccessor.class),
                Arguments.of(mock(TarArchiveEntry.class), AttributeAccessor.TarAttributeAccessor.class),
                Arguments.of(mock(TestArchiveEntry.class), AttributeAccessor.FallbackAttributeAccessor.class));
    }

    @ParameterizedTest
    @MethodSource("entryNames")
    <A extends org.apache.commons.compress.archivers.ArchiveEntry> void shouldCreate(A entry, Class<?> clazz) {
        AttributeAccessor<A> accessor = AttributeAccessor.create(entry);

        assertThat(accessor).isNotNull().isOfAnyClassIn(clazz);
    }
}
