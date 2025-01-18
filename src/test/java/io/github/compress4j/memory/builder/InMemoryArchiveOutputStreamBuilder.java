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
package io.github.compress4j.memory.builder;

import io.github.compress4j.archive.compression.builder.ArchiveOutputStreamBuilder;
import io.github.compress4j.memory.InMemoryArchiveOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class InMemoryArchiveOutputStreamBuilder extends ArchiveOutputStreamBuilder<InMemoryArchiveOutputStream> {

    public InMemoryArchiveOutputStreamBuilder(OutputStream outputStream) {
        super(outputStream);
    }

    public InMemoryArchiveOutputStreamBuilder(OutputStream outputStream, Map<String, Object> options)
            throws IOException {
        super(outputStream, options);
    }

    @Override
    public InMemoryArchiveOutputStream build() throws IOException {
        return applyFormatOptions(new InMemoryArchiveOutputStream(outputStream), options);
    }
}
