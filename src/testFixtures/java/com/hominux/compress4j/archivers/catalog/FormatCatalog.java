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
package com.hominux.compress4j.archivers.catalog;

import static com.hominux.compress4j.archivers.catalog.Capability.DIRECTORIES;
import static com.hominux.compress4j.archivers.catalog.Capability.LAST_MODIFIED;
import static com.hominux.compress4j.archivers.catalog.Capability.MODES;
import static com.hominux.compress4j.archivers.catalog.Capability.REQUIRES_SIZE;
import static com.hominux.compress4j.archivers.catalog.Capability.STREAM_INPUT;
import static com.hominux.compress4j.archivers.catalog.Capability.STREAM_OUTPUT;
import static com.hominux.compress4j.archivers.catalog.Capability.SYMLINKS;

import com.hominux.compress4j.archivers.ArchiveCreator;
import com.hominux.compress4j.archivers.ArchiveExtractor;
import com.hominux.compress4j.archivers.ar.ArArchiveCreator;
import com.hominux.compress4j.archivers.ar.ArArchiveExtractor;
import com.hominux.compress4j.archivers.cpio.CpioArchiveCreator;
import com.hominux.compress4j.archivers.cpio.CpioArchiveExtractor;
import com.hominux.compress4j.archivers.sevenz.SevenZArchiveCreator;
import com.hominux.compress4j.archivers.sevenz.SevenZArchiveExtractor;
import com.hominux.compress4j.archivers.tar.TarArchiveCreator;
import com.hominux.compress4j.archivers.tar.TarArchiveExtractor;
import com.hominux.compress4j.archivers.tar.TarBZip2ArchiveCreator;
import com.hominux.compress4j.archivers.tar.TarBZip2ArchiveExtractor;
import com.hominux.compress4j.archivers.tar.TarGzArchiveCreator;
import com.hominux.compress4j.archivers.tar.TarGzArchiveExtractor;
import com.hominux.compress4j.archivers.tar.TarLz4ArchiveCreator;
import com.hominux.compress4j.archivers.tar.TarLz4ArchiveExtractor;
import com.hominux.compress4j.archivers.tar.TarLzmaArchiveCreator;
import com.hominux.compress4j.archivers.tar.TarLzmaArchiveExtractor;
import com.hominux.compress4j.archivers.tar.TarXzArchiveCreator;
import com.hominux.compress4j.archivers.tar.TarXzArchiveExtractor;
import com.hominux.compress4j.archivers.tar.TarZstdArchiveCreator;
import com.hominux.compress4j.archivers.tar.TarZstdArchiveExtractor;
import com.hominux.compress4j.archivers.zip.ZipArchiveCreator;
import com.hominux.compress4j.archivers.zip.ZipArchiveExtractor;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.io.function.IOFunction;

/** Every archive format Compress4J supports, with the capabilities the contract suite verifies. */
public final class FormatCatalog {

    private static final Set<Capability> TAR =
            EnumSet.of(DIRECTORIES, MODES, SYMLINKS, LAST_MODIFIED, REQUIRES_SIZE, STREAM_INPUT, STREAM_OUTPUT);

    private FormatCatalog() {}

    public static Stream<ArchiveFormat> all() {
        return Stream.of(
                streamNative(
                        "tar",
                        TAR,
                        p -> TarArchiveCreator.builder(p).build(),
                        o -> TarArchiveCreator.builder(o).build(),
                        p -> TarArchiveExtractor.builder(p).build(),
                        i -> TarArchiveExtractor.builder(i).build()),
                streamNative(
                        "tar.gz",
                        TAR,
                        p -> TarGzArchiveCreator.builder(p).build(),
                        o -> TarGzArchiveCreator.builder(o).build(),
                        p -> TarGzArchiveExtractor.builder(p).build(),
                        i -> TarGzArchiveExtractor.builder(i).build()),
                streamNative(
                        "tar.bz2",
                        TAR,
                        p -> TarBZip2ArchiveCreator.builder(p).build(),
                        o -> TarBZip2ArchiveCreator.builder(o).build(),
                        p -> TarBZip2ArchiveExtractor.builder(p).build(),
                        i -> TarBZip2ArchiveExtractor.builder(i).build()),
                streamNative(
                        "tar.xz",
                        TAR,
                        p -> TarXzArchiveCreator.builder(p).build(),
                        o -> TarXzArchiveCreator.builder(o).build(),
                        p -> TarXzArchiveExtractor.builder(p).build(),
                        i -> TarXzArchiveExtractor.builder(i).build()),
                streamNative(
                        "tar.lzma",
                        TAR,
                        p -> TarLzmaArchiveCreator.builder(p).build(),
                        o -> TarLzmaArchiveCreator.builder(o).build(),
                        p -> TarLzmaArchiveExtractor.builder(p).build(),
                        i -> TarLzmaArchiveExtractor.builder(i).build()),
                streamNative(
                        "tar.lz4",
                        TAR,
                        p -> TarLz4ArchiveCreator.builder(p).build(),
                        o -> TarLz4ArchiveCreator.builder(o).build(),
                        p -> TarLz4ArchiveExtractor.builder(p).build(),
                        i -> TarLz4ArchiveExtractor.builder(i).build()),
                streamNative(
                        "tar.zst",
                        TAR,
                        p -> TarZstdArchiveCreator.builder(p).build(),
                        o -> TarZstdArchiveCreator.builder(o).build(),
                        p -> TarZstdArchiveExtractor.builder(p).build(),
                        i -> TarZstdArchiveExtractor.builder(i).build()),
                streamNative(
                        "ar",
                        EnumSet.of(MODES, SYMLINKS, LAST_MODIFIED, REQUIRES_SIZE, STREAM_INPUT, STREAM_OUTPUT),
                        p -> ArArchiveCreator.builder(p).build(),
                        o -> ArArchiveCreator.builder(o).build(),
                        p -> ArArchiveExtractor.builder(p).build(),
                        i -> ArArchiveExtractor.builder(i).build()),
                streamNative(
                        "cpio",
                        EnumSet.of(
                                DIRECTORIES,
                                MODES,
                                SYMLINKS,
                                LAST_MODIFIED,
                                REQUIRES_SIZE,
                                STREAM_INPUT,
                                STREAM_OUTPUT),
                        p -> CpioArchiveCreator.builder(p).build(),
                        o -> CpioArchiveCreator.builder(o).build(),
                        p -> CpioArchiveExtractor.builder(p).build(),
                        i -> CpioArchiveExtractor.builder(i).build()),
                new ArchiveFormat(
                        "zip",
                        EnumSet.of(DIRECTORIES, MODES, SYMLINKS, LAST_MODIFIED, STREAM_OUTPUT),
                        Optional.of(c(p -> ZipArchiveCreator.builder(p).build())),
                        Optional.empty(),
                        Optional.of(co(o -> ZipArchiveCreator.builder(o).build())),
                        x(p -> ZipArchiveExtractor.builder(p).build()),
                        Optional.empty(),
                        Optional.empty()),
                new ArchiveFormat(
                        "7z",
                        EnumSet.of(DIRECTORIES, MODES, SYMLINKS, LAST_MODIFIED),
                        Optional.of(c(p -> SevenZArchiveCreator.builder(p).build())),
                        Optional.empty(),
                        Optional.empty(),
                        x(p -> SevenZArchiveExtractor.builder(p).build()),
                        Optional.empty(),
                        Optional.empty()));
    }

    public static Stream<ArchiveFormat> writable() {
        return all().filter(ArchiveFormat::writable);
    }

    private static ArchiveFormat streamNative(
            String name,
            Set<Capability> capabilities,
            IOFunction<Path, ArchiveCreator<?>> createAt,
            IOFunction<OutputStream, ArchiveCreator<?>> createOnStream,
            IOFunction<Path, ArchiveExtractor<?>> readAt,
            IOFunction<InputStream, ArchiveExtractor<?>> readFromStream) {
        return new ArchiveFormat(
                name,
                capabilities,
                Optional.of(createAt),
                Optional.empty(),
                Optional.of(createOnStream),
                readAt,
                Optional.empty(),
                Optional.of(readFromStream));
    }

    private static IOFunction<Path, ArchiveCreator<?>> c(IOFunction<Path, ArchiveCreator<?>> f) {
        return f;
    }

    private static IOFunction<OutputStream, ArchiveCreator<?>> co(IOFunction<OutputStream, ArchiveCreator<?>> f) {
        return f;
    }

    private static IOFunction<Path, ArchiveExtractor<?>> x(IOFunction<Path, ArchiveExtractor<?>> f) {
        return f;
    }
}
