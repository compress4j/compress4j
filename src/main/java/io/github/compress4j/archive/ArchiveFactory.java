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
package io.github.compress4j.archive;

import io.github.compress4j.archive.compression.Compressor.CompressorBuilder;
import io.github.compress4j.archive.compression.TarCompressor;
import io.github.compress4j.archive.compression.TarGzCompressor;
import io.github.compress4j.archive.decompression.Decompressor;
import io.github.compress4j.archive.decompression.TarDecompressor;
import io.github.compress4j.archive.decompression.TarGzDecompressor;
import java.io.OutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Factory class to build compressors and decompressors
 *
 * @since 2.2
 */
public class ArchiveFactory {
    private ArchiveFactory() {}

    @SuppressWarnings({"rawtypes"})
    public static CompressorBuilder compressor(ArchiveType archiveType, OutputStream outputStream) {
        switch (archiveType) {
            case TAR:
                return TarCompressor.builder(outputStream);
            case TAR_GZ:
                return TarGzCompressor.builder(outputStream);
            default:
                throw new UnsupportedOperationException("Unsupported archive type: " + archiveType);
        }
    }

    @SuppressWarnings("java:S1452")
    public static Decompressor<?> decompressor(ArchiveType archiveType) {
        switch (archiveType) {
            case TAR:
                return new TarDecompressor((TarArchiveInputStream) null);
            case TAR_GZ:
                return new TarGzDecompressor((TarArchiveInputStream) null);
            default:
                throw new UnsupportedOperationException("Unsupported archive type: " + archiveType);
        }
    }
}
