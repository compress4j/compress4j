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

import io.github.compress4j.archive.ArchiveCreator.ArchiveCreatorBuilder;
import io.github.compress4j.archive.ArchiveExtractor.ArchiveExtractorBuilder;
import io.github.compress4j.archive.tar.TarArchiveCreator;
import io.github.compress4j.archive.tar.TarArchiveExtractor;
import io.github.compress4j.archive.tar.TarGzArchiveCreator;
import io.github.compress4j.archive.tar.TarGzArchiveExtractor;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Factory class to build creators and decompressors
 *
 * @since 2.2
 */
public class ArchiveFactory {
    private ArchiveFactory() {}

    /**
     * Creates a new {@link ArchiveCreatorBuilder} for the given {@link ArchiveType}.
     *
     * @param archiveType the archive type to create the archive for
     * @param outputStream the output stream to write the archive to
     * @return a new ArchiveCreatorBuilder instance
     * @throws UnsupportedOperationException if the given archive type is not supported
     */
    @SuppressWarnings("rawtypes")
    public static ArchiveCreatorBuilder creator(ArchiveType archiveType, OutputStream outputStream) {
        switch (archiveType) {
            case TAR:
                return TarArchiveCreator.builder(outputStream);
            case TAR_GZ:
                return TarGzArchiveCreator.builder(outputStream);
            default:
                throw new UnsupportedOperationException("Unsupported archive type: " + archiveType);
        }
    }

    @SuppressWarnings("rawtypes")
    public static ArchiveExtractorBuilder extractor(ArchiveType archiveType, InputStream inputStream) {
        switch (archiveType) {
            case TAR:
                return TarArchiveExtractor.builder(inputStream);
            case TAR_GZ:
                return TarGzArchiveExtractor.builder(inputStream);
            default:
                throw new UnsupportedOperationException("Unsupported archive type: " + archiveType);
        }
    }
}
