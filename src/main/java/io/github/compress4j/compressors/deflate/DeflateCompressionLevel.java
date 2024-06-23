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
package io.github.compress4j.compressors.deflate;

import java.util.zip.Deflater;

/**
 * Defines constants for Deflate compression levels and strategies. These values are standard for the deflate algorithm.
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
     * Compression strategy best used for data consisting mostly of small values with a somewhat random distribution.
     * Forces more Huffman coding and less string matching.
     *
     * @see Deflater#FILTERED
     */
    FILTERED(1),

    /**
     * Compression strategy for Huffman coding only.
     *
     * @see Deflater#HUFFMAN_ONLY
     */
    HUFFMAN_ONLY(2),

    /**
     * Default compression strategy.
     *
     * @see Deflater#DEFAULT_STRATEGY
     */
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
