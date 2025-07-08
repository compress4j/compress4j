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
package io.github.compress4j.assertion;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.AbstractPathAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;

public class DirectoryAssert extends AbstractPathAssert<DirectoryAssert> {

    protected DirectoryAssert(Path actual) {
        super(actual, DirectoryAssert.class);
    }

    public static DirectoryAssert assertThat(Path actual) {
        return new DirectoryAssert(actual);
    }

    private static Map<String, Path> directoryContents(final Path root) {
        final int rootPathLength = root.toAbsolutePath().toString().length();
        try (final Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> !root.equals(path))
                    .collect(Collectors.toMap(
                            path -> path.toAbsolutePath().toString().substring(rootPathLength + 1),
                            Function.identity(),
                            (oldPath, newPath) -> {
                                final String message =
                                        String.format("paths `%s` and `%s` have conflicting keys", oldPath, newPath);
                                throw new IllegalStateException(message);
                            },
                            TreeMap::new));
        } catch (final IOException error) {
            final String message = String.format("failed walking directory: `%s`", root);
            throw new UncheckedIOException(message, error);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public DirectoryAssert containsAllEntriesOf(Map<String, String> expected) throws IOException {
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

    @SuppressWarnings("UnusedReturnValue")
    public DirectoryAssert containsSameContentAs(final Path expected) {
        // Compare file paths
        final Map<String, Path> actualContents = directoryContents(actual);
        final Map<String, Path> expectedContents = directoryContents(expected);
        final Set<String> relativeFilePaths = expectedContents.keySet();
        Assertions.assertThat(actualContents).containsOnlyKeys(relativeFilePaths);

        // Compare file contents
        final SoftAssertions assertions = new SoftAssertions();
        relativeFilePaths.forEach(relativeFilePath -> {
            final Path actualFilePath = actualContents.get(relativeFilePath);
            final Path expectedFilePath = expectedContents.get(relativeFilePath);
            if (!Files.isDirectory(actualFilePath) || !Files.isDirectory(expectedFilePath)) {
                assertions.assertThat(actualFilePath).hasSameTextualContentAs(expectedFilePath);
            }
        });
        assertions.assertAll();
        return this;
    }
}
