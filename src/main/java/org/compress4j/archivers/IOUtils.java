/*
 * Copyright 2024 The Compress4J Project
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
package org.compress4j.archivers;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/** Utility class for I/O operations. */
public final class IOUtils {

    private IOUtils() {}

    /**
     * Makes sure that the given {@link File} is either a writable directory, or that it does not exist and a directory
     * can be created at its path. <br>
     * Will throw an exception if the given {@link File} is actually an existing file, or the directory is not writable
     *
     * @param destination the directory which to ensure its existence for
     * @throws IllegalArgumentException if the destination is an existing file, or the directory is not writable
     */
    public static void requireDirectory(File destination) throws IllegalArgumentException {
        if (destination.isFile()) {
            throw new IllegalArgumentException(destination + " exists and is a file, directory or path expected.");
        } else if (!destination.exists()) {
            //noinspection ResultOfMethodCallIgnored
            destination.mkdirs();
        }
        if (!destination.canWrite()) {
            throw new IllegalArgumentException("Can not write to destination " + destination);
        }
    }

    /**
     * Null-safe method that calls {@link java.io.Closeable#close()} and chokes the IOException.
     *
     * @param closeable the object to close
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    /**
     * Given a source File, return its direct descendants if the File is a directory. Otherwise return the File itself.
     *
     * @param source File or folder to be examined
     * @return a File[] array containing the files inside this folder, or a size-1 array containing the file itself.
     */
    public static File[] filesContainedIn(File source) {
        if (source.isDirectory()) {
            return source.listFiles();
        } else {
            return new File[] {source};
        }
    }
}
