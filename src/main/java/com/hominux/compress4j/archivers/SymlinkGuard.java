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

import com.hominux.compress4j.exceptions.UnsafeEntryException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Re-checks, once an extraction has created all its entries, that every symlink it created still resolves inside the
 * output directory. A later entry can add a link that changes how an earlier, individually checked link resolves.
 */
final class SymlinkGuard {

    private final Path outputDir;
    private final List<Path> links = new ArrayList<>();

    SymlinkGuard(Path outputDir) {
        this.outputDir = outputDir;
    }

    void record(Path link) {
        links.add(link);
    }

    void verify() throws UnsafeEntryException {
        Optional<Path> firstEscape = Optional.empty();
        for (Path link : links) {
            if (escapes(link)) {
                firstEscape = firstEscape.or(() -> Optional.of(link));
                deleteQuietly(link);
            }
        }
        if (firstEscape.isPresent()) {
            throw new UnsafeEntryException("Invalid symlink (points outside of output directory): "
                    + outputDir.relativize(firstEscape.orElseThrow()));
        }
    }

    static Path realLocation(Path path) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path current = absolute.getRoot();
        for (Path part : absolute) {
            String name = part.toString();
            if (name.equals(".")) {
                continue;
            }
            Path next = name.equals("..") ? parentOf(current) : current.resolve(part);
            current = Files.exists(next) ? next.toRealPath() : next;
        }
        return current;
    }

    private boolean escapes(Path link) {
        try {
            Path target = link.getParent().resolve(Files.readSymbolicLink(link));
            return !realLocation(target).startsWith(outputDir.toRealPath());
        } catch (IOException e) {
            return true;
        }
    }

    private static Path parentOf(Path current) {
        Path parent = current.getParent();
        return parent != null ? parent : current;
    }

    private static void deleteQuietly(Path link) {
        try {
            Files.deleteIfExists(link);
        } catch (IOException ignored) {
            // The extraction fails with UnsafeEntryException regardless.
        }
    }
}
