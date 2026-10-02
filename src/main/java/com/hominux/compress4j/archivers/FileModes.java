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
package com.hominux.compress4j.archivers;

import static com.hominux.compress4j.utils.FileUtils.DOS_HIDDEN;
import static com.hominux.compress4j.utils.FileUtils.DOS_READ_ONLY;
import static com.hominux.compress4j.utils.FileUtils.NO_MODE;

import com.hominux.compress4j.utils.PosixFilePermissionsMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reads the archive mode of a file on disk. */
final class FileModes {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileModes.class);

    private FileModes() {}

    /**
     * Get mode of the {@code Path}.
     *
     * @param path {@code Path} to get the mode of
     * @param windows whether to read DOS attributes instead of POSIX permissions
     * @return the {@code Path} mode
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    static int of(Path path, boolean windows) throws IOException {
        if (windows) {
            DosFileAttributeView attrs =
                    Files.getFileAttributeView(path, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            if (attrs != null) {
                DosFileAttributes dosAttrs = attrs.readAttributes();
                int mode = NO_MODE;
                if (dosAttrs.isReadOnly()) mode |= DOS_READ_ONLY;
                if (dosAttrs.isHidden()) mode |= DOS_HIDDEN;
                return mode;
            } else {
                LOGGER.trace("Cannot get DOS file attributes for: {}", path);
            }
        } else {
            PosixFileAttributeView attrs =
                    Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            if (attrs != null) {
                return PosixFilePermissionsMapper.toUnixMode(
                        attrs.readAttributes().permissions());
            } else {
                LOGGER.trace("Cannot get POSIX file attributes for: {}", path);
            }
        }
        return NO_MODE;
    }
}
