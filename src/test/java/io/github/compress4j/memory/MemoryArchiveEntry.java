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
package io.github.compress4j.memory;

import static io.github.compress4j.archive.decompression.Decompressor.Entry.Type.DIR;
import static io.github.compress4j.archive.decompression.Decompressor.Entry.Type.FILE;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.compress4j.archive.decompression.Decompressor.Entry.Type;
import jakarta.annotation.Nullable;
import java.util.Date;
import org.apache.commons.compress.archivers.ArchiveEntry;

public final class MemoryArchiveEntry implements ArchiveEntry {

    /** The entry's name. */
    private final String name;

    /** The entry's last modified date. */
    private final Date lastModifiedDate = new Date();

    /** The entry's content. */
    private final String content;

    /** The entry's link name. */
    private final String linkName;

    /** The entry's size. */
    private final long size;

    /** The entry's directory flag. */
    private final boolean directory;

    /** Type of the entry */
    public final Type type;

    /** Created for Jackson deserialization. */
    @JsonCreator
    private MemoryArchiveEntry() {
        this("", "");
    }

    /**
     * Creates a new {@code MemoryArchiveEntry}.
     *
     * @param name the entry's name
     * @param content the entry's content
     */
    public MemoryArchiveEntry(String name, String content) {
        this(name, content, false, 0);
    }

    /**
     * Creates a new {@code MemoryArchiveEntry}.
     *
     * @param name the entry's name
     * @param content the entry's content
     * @param isDirectory whether the entry is a directory
     * @param size the entry's size
     */
    public MemoryArchiveEntry(String name, String content, boolean isDirectory, long size) {
        this(name, content, isDirectory ? DIR : FILE, null, size);
    }

    /**
     * Creates a new {@code MemoryArchiveEntry}.
     *
     * @param name the entry's name
     * @param content the entry's content
     * @param type the entry's type
     * @param linkName the entry's link name
     * @param size the entry's size
     */
    public MemoryArchiveEntry(String name, String content, Type type, @Nullable String linkName, long size) {
        this.name = name;
        this.content = content;
        this.type = type;
        this.directory = type == DIR;
        this.linkName = linkName;
        this.size = size;
    }

    /**
     * Gets this entry's content.
     *
     * @return This entry's content.
     */
    public String getContent() {
        return content;
    }

    /** @inheritDoc */
    @Override
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    /** @inheritDoc */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets this entry's link name.
     *
     * @return This entry's link name.
     */
    public String getLinkName() {
        return linkName;
    }

    /** @inheritDoc */
    @Override
    public long getSize() {
        return size;
    }

    /** @inheritDoc */
    @Override
    public boolean isDirectory() {
        return directory;
    }
}
