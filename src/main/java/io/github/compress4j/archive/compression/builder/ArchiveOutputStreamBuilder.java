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
package io.github.compress4j.archive.compression.builder;

import java.beans.Statement;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract builder for ArchiveOutputStreams.
 *
 * @param <A> the type of ArchiveOutputStream to build
 */
public abstract class ArchiveOutputStreamBuilder<A extends ArchiveOutputStream<? extends ArchiveEntry>> {
    protected final OutputStream outputStream;
    protected final Map<String, Object> options;

    /**
     * Create a new ArchiveOutputStreamBuilder.
     *
     * @param outputStream the output stream
     */
    protected ArchiveOutputStreamBuilder(OutputStream outputStream) {
        this(outputStream, Collections.emptyMap());
    }

    /**
     * Create a new ArchiveOutputStreamBuilder with the given output stream and options.
     *
     * @param outputStream the output outputStream
     * @param options the options for the compressor
     */
    protected ArchiveOutputStreamBuilder(OutputStream outputStream, Map<String, Object> options) {
        this.outputStream = outputStream;
        this.options = options;
    }

    /**
     * Apply options to archive output stream
     *
     * @param stream stream to apply options to
     * @param options options map
     * @return stream with option applied
     * @throws IOException if an IO error occurred
     */
    protected A applyFormatOptions(A stream, Map<String, Object> options) throws IOException {
        for (Map.Entry<String, Object> option : options.entrySet()) {
            try {
                new Statement(stream, "set" + StringUtils.capitalize(option.getKey()), new Object[] {option.getValue()})
                        .execute();
            } catch (Exception e) {
                throw new IOException("Cannot set option: " + option.getKey(), e);
            }
        }
        return stream;
    }

    /**
     * Start a new archive. Entries can be included in the archive using the putEntry method, and then the archive
     * should be closed using its close method. In addition, options can be applied to the underlying stream. E.g.
     * compression level.
     *
     * <p>
     *
     * <ol>
     *   <li>Use {@link #outputStream} as underlying output stream to which to write the archive.
     *   <li>Use {@link #options} to apply to the underlying output stream. Keys are option names and values are option
     *       values.
     * </ol>
     *
     * @return new archive object for use in putEntry
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    public abstract A build() throws IOException;
}
