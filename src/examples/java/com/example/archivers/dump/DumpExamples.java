/*
 * Copyright 2026 The Compress4J Project
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
package com.example.archivers.dump;

import io.github.compress4j.archivers.dump.DumpArchiveExtractor;
import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings({"unused"})
public class DumpExamples {
    private DumpExamples() {
        /* no-op */
    }

    /** Example for UNIX dump extraction. */
    public static void dumpExtractor() throws IOException {
        // tag::dump-extractor[]
        try (DumpArchiveExtractor dumpExtractor = DumpArchiveExtractor.builder(Path.of("example.dump"))
                .filter(entry -> !entry.name().startsWith("bad"))
                .overwrite(true)
                .build()) {
            dumpExtractor.extract(Path.of("outputDir"));
        }
        // end::dump-extractor[]
    }
}
