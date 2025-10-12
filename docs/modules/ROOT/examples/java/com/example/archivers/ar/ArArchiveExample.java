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
package com.example.archivers.ar;

import io.github.compress4j.archivers.ar.ArArchiveCreator;
import io.github.compress4j.archivers.ar.ArArchiveExtractor;
import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings({"java:S1192", "unused"})
public class ArArchiveExample {

    private ArArchiveExample() {
        // Usage example
    }

    public static void arCreator() throws IOException {
        // tag::ar-creator[]
        try (ArArchiveCreator arCreator =
                ArArchiveCreator.builder(Path.of("example.ar")).build()) {
            arCreator.addFile(Path.of("path/to/file.txt"));
        }
        // end::ar-creator[]
    }

    public static void arExtractor() throws IOException {
        // tag::ar-extractor[]
        try (ArArchiveExtractor arExtractor = ArArchiveExtractor.builder(Path.of("example.ar"))
                .overwrite(true)
                .build()) {
            arExtractor.extract(Path.of("outputDir"));
        }
        // end::ar-extractor[]
    }
}
