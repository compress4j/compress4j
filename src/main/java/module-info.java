/*
 * Copyright 2024-2026 The Compress4J Project
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

/**
 * Archiving and compression API on top of Apache Commons Compress.
 *
 * <p>Commons Compress is an automatic module, so consumers are bound to the name {@code org.apache.commons.compress}.
 * The XZ formats need {@code org.tukaani.xz} on the module path.
 */
module io.github.compress4j {
    requires transitive org.apache.commons.compress;
    requires transitive org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.slf4j;
    requires static transitive jakarta.annotation;
    requires static transitive org.tukaani.xz;

    exports io.github.compress4j.archivers;
    exports io.github.compress4j.archivers.ar;
    exports io.github.compress4j.archivers.cpio;
    exports io.github.compress4j.archivers.tar;
    exports io.github.compress4j.archivers.zip;
    exports io.github.compress4j.compressors;
    exports io.github.compress4j.compressors.bzip2;
    exports io.github.compress4j.compressors.deflate;
    exports io.github.compress4j.compressors.gzip;
    exports io.github.compress4j.compressors.pack200;
    exports io.github.compress4j.compressors.xz;
    exports io.github.compress4j.exceptions;
}
