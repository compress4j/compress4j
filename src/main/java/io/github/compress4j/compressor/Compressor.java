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
package io.github.compress4j.compressor;

import java.io.OutputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;

/**
 * This abstract class is the superclass of all classes providing compression.
 *
 * @param <C> The type of {@link CompressorOutputStream} to write to.
 * @since 2.2
 */
public abstract class Compressor<C extends CompressorOutputStream<? extends OutputStream>> implements AutoCloseable {
    /** Compressor output stream to be used for compression. */
    protected final C compressorOutputStream;

    /**
     * Create an instance of {@link Compressor}
     *
     * @param compressorOutputStream the {@link CompressorOutputStream} to write to.
     */
    protected Compressor(C compressorOutputStream) {
        this.compressorOutputStream = compressorOutputStream;
    }
}
