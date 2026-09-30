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
package io.github.compress4j.archivers;

import static io.github.compress4j.utils.FileUtils.checkValidPath;

import io.github.compress4j.archivers.ArchiveExtractor.Entry;
import io.github.compress4j.archivers.ArchiveExtractor.EscapingSymlinkPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the symbolic link an archive entry describes, applying the {@link EscapingSymlinkPolicy}.
 *
 * @param policy how to treat links pointing outside the output directory
 * @param overwrite whether an existing file at the link's location is replaced
 */
record SymlinkExtractor(EscapingSymlinkPolicy policy, boolean overwrite) {

    private static final Logger LOGGER = LoggerFactory.getLogger(SymlinkExtractor.class);

    /**
     * Extracts the symlink to the output file.
     *
     * @param outputDir the directory to extract the archive to
     * @param entry the entry to extract
     * @param outputFile the file to extract the entry to
     * @throws IOException if an I/O error occurs
     */
    void extract(Path outputDir, Entry entry, Path outputFile) throws IOException {
        if (entry.linkTarget() == null || StringUtils.isBlank(entry.linkTarget())) {
            throw new IOException("Invalid symlink entry: " + entry.name() + " (empty target)");
        }

        String target = entry.linkTarget();

        switch (policy) {
            case DISALLOW -> verifySymlinkTarget(entry.name(), entry.linkTarget(), outputDir, outputFile);
            case RELATIVIZE_ABSOLUTE -> target = relativizeIfAbsolute(target, outputDir);
            case ALLOW -> LOGGER.debug("Extracting symlink entry as is: {} -> {}", entry.name(), target);
        }

        if (overwrite || !Files.exists(outputFile, LinkOption.NOFOLLOW_LINKS)) {
            Path outputTarget = Paths.get(target);
            EntryPaths.makeDirectory(outputFile.getParent());
            Files.deleteIfExists(outputFile);
            Files.createSymbolicLink(outputFile, outputTarget);
        } else {
            LOGGER.debug("Skipping symlink entry: {} -> {} (already exists)", entry.name(), target);
        }
    }

    private static String relativizeIfAbsolute(String target, Path outputDir) {
        return Paths.get(target).isAbsolute()
                ? Paths.get(outputDir.toString(), target.substring(1)).toString()
                : target;
    }

    /**
     * Verifies that the symlink target is valid.
     *
     * @param entryName the name of the entry
     * @param linkTarget the target of the symlink
     * @param outputDir the directory to extract the archive to
     * @param outputFile the file to extract the entry to
     * @throws IOException if the symlink target is invalid
     */
    private static void verifySymlinkTarget(String entryName, String linkTarget, Path outputDir, Path outputFile)
            throws IOException {
        Path outputTarget = Paths.get(linkTarget);
        if (outputTarget.isAbsolute()) {
            throw new IOException("Invalid symlink (absolute path): " + entryName + " -> " + linkTarget);
        }

        Path linkTargetPath = outputFile.getParent().resolve(outputTarget);

        try {
            checkValidPath(linkTargetPath, outputDir);
        } catch (IOException e) {
            throw new IOException(
                    "Invalid symlink (points outside of output directory): " + entryName + " -> " + linkTarget, e);
        }
    }
}
