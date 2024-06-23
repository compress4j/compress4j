/*
 * Copyright 2024-2025 The Compress4J Project
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
package io.github.compress4j.assertion;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class CompressorAssertion extends AbstractAssert<CompressorAssertion, Path> {

    protected CompressorAssertion(Path path) {
        super(path, CompressorAssertion.class);
    }

    public static CompressorAssertion assertThat(Path path) {
        return new CompressorAssertion(path);
    }

    @SuppressWarnings("UnusedReturnValue")
    public CompressorAssertion containsAllEntriesOf(Map<String, String> expected) throws IOException {
        try (InputStream fi = Files.newInputStream(actual);
                InputStream bi = new BufferedInputStream(fi);
                TarArchiveInputStream o = new TarArchiveInputStream(bi)) {
            ArchiveEntry e;
            while ((e = o.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String content = new String(IOUtils.toByteArray(o), StandardCharsets.UTF_8).trim();
                Assertions.assertThat(content).isEqualTo(expected.get(e.getName()));
            }
        }
        return this;
    }
}
