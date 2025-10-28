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
package io.github.compress4j.compressors.xz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

import io.github.compress4j.compressors.xz.XZCompressor.XZCompressorBuilder;
import io.github.compress4j.compressors.xz.XZCompressor.XZCompressorOutputStreamBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tukaani.xz.LZMA2Options;

/** Tests for the {@link XZCompressorOutputStreamBuilder} class. */
class XZCompressorOutputStreamBuilderTest {

    private XZCompressorBuilder mockParent;
    private XZCompressorOutputStreamBuilder<XZCompressorBuilder> builder;

    @BeforeEach
    void setUp() {
        // Given
        mockParent = mock(XZCompressorBuilder.class);
        builder = new XZCompressorOutputStreamBuilder<>(mockParent, new ByteArrayOutputStream());
    }

    @Test
    void testParentBuilder() {
        // When
        var parent = builder.parentBuilder();

        // Then
        assertThat(parent).isSameAs(mockParent);
    }

    @Test
    void testBuildReturnsNonNullStream() throws IOException {
        // When
        try (var stream = builder.build()) {
            // Then
            assertThat(stream).isNotNull();
        }
    }

    @Test
    void testLzma2Options() throws IOException, NoSuchFieldException, IllegalAccessException {
        // Given
        var customOptions = new LZMA2Options(3);

        // When
        builder.lzma2Options(customOptions);

        // Then
        var optionsField = builder.getClass().getDeclaredField("lzma2Options");
        optionsField.setAccessible(true);
        var internalOptions = (LZMA2Options) optionsField.get(builder);

        assertThat(internalOptions).isSameAs(customOptions);

        try (var stream = builder.build()) {
            assertThat(stream).isNotNull();
        }
    }

    @Test
    void testLzma2OptionsNullResetsToDefault() throws IOException, NoSuchFieldException, IllegalAccessException {
        // Given
        var oldOptions = new LZMA2Options(1);
        builder.lzma2Options(oldOptions);

        // When
        builder.lzma2Options(null);

        // Then
        var optionsField = builder.getClass().getDeclaredField("lzma2Options");
        optionsField.setAccessible(true);
        var internalOptions = (LZMA2Options) optionsField.get(builder);

        assertThat(internalOptions).isNotNull().isNotSameAs(oldOptions);

        try (var stream = builder.build()) {
            assertThat(stream).isNotNull();
        }
    }

    @Test
    void testPresetValidLevels() throws IOException, NoSuchFieldException, IllegalAccessException {
        // When & Then for min preset
        builder.preset(LZMA2Options.PRESET_MIN);
        var optionsField = builder.getClass().getDeclaredField("lzma2Options");
        optionsField.setAccessible(true);
        var internalOptions = (LZMA2Options) optionsField.get(builder);
        assertThat(internalOptions).isNotNull();
        try (var stream = builder.build()) {
            assertThat(stream).isNotNull();
        }

        // When & Then for max preset
        builder.preset(LZMA2Options.PRESET_MAX);
        internalOptions = (LZMA2Options) optionsField.get(builder);
        assertThat(internalOptions).isNotNull();
        try (var stream = builder.build()) {
            assertThat(stream).isNotNull();
        }
    }

    @Test
    void testPresetTooLowThrowsException() {
        // Given
        var invalidPreset = LZMA2Options.PRESET_MIN - 1;

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> builder.preset(invalidPreset))
                .withMessageContaining("XZ preset must be in the range [0, 9], but was: " + invalidPreset);
    }

    @Test
    void testPresetTooHighThrowsException() {
        // Given
        var invalidPreset = LZMA2Options.PRESET_MAX + 1;

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> builder.preset(invalidPreset))
                .withMessageContaining("XZ preset must be in the range [0, 9], but was: " + invalidPreset);
    }
}
