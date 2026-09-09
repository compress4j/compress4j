/*
 * Copyright 2025-2026 The Compress4J Project
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
package io.github.compress4j.compressors.deflate;

import java.util.zip.Deflater;

/**
 * Defines constants for Deflate compression levels. These values correspond to the compression levels accepted by
 * {@link org.apache.commons.compress.compressors.deflate.DeflateParameters#setCompressionLevel(int)} and provide
 * different trade-offs between compression speed and compression ratio.
 *
 * @since 2.2
 */
public enum DeflateCompressionLevel {
    /**
     * Compression level for no compression.
     *
     * @see Deflater#NO_COMPRESSION
     */
    NO_COMPRESSION(0),

    /**
     * Compression level for fastest compression.
     *
     * @see Deflater#BEST_SPEED
     */
    BEST_SPEED(1),

    /**
     * Compression level for best compression.
     *
     * @see Deflater#BEST_COMPRESSION
     */
    BEST_COMPRESSION(9),

    /**
     * Default compression level.
     *
     * @see Deflater#DEFAULT_COMPRESSION
     */
    DEFAULT_COMPRESSION(-1),

    /**
     * This is a {@link Deflater} <em>strategy</em> constant, not a compression level:
     * {@link org.apache.commons.compress.compressors.deflate.DeflateParameters#setCompressionLevel(int)} does not
     * accept strategy values, so using this constant as a level silently applies {@link Deflater#BEST_SPEED} (value
     * {@code 1}) instead of the Filtered strategy.
     *
     * @deprecated does not do what its name implies; will be removed in a future major version.
     * @see Deflater#FILTERED
     */
    @Deprecated
    FILTERED(1),

    /**
     * This is a {@link Deflater} <em>strategy</em> constant, not a compression level:
     * {@link org.apache.commons.compress.compressors.deflate.DeflateParameters#setCompressionLevel(int)} does not
     * accept strategy values, so using this constant as a level applies compression level {@code 2}, not the
     * Huffman-only strategy.
     *
     * @deprecated does not do what its name implies; will be removed in a future major version.
     * @see Deflater#HUFFMAN_ONLY
     */
    @Deprecated
    HUFFMAN_ONLY(2),

    /**
     * This is a {@link Deflater} <em>strategy</em> constant, not a compression level:
     * {@link org.apache.commons.compress.compressors.deflate.DeflateParameters#setCompressionLevel(int)} does not
     * accept strategy values, so using this constant as a level silently applies {@link Deflater#NO_COMPRESSION} (value
     * {@code 0}) instead of the default strategy.
     *
     * @deprecated does not do what its name implies; will be removed in a future major version.
     * @see Deflater#DEFAULT_STRATEGY
     */
    @Deprecated
    DEFAULT_STRATEGY(0);

    private final int value;

    /**
     * Private constructor for the enum constants.
     *
     * @param value The integer value associated with the compression level or strategy.
     */
    DeflateCompressionLevel(int value) {
        this.value = value;
    }

    /**
     * Returns the integer value of the compression level or strategy.
     *
     * @return The integer value.
     */
    public int getValue() {
        return value;
    }
}
