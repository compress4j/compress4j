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

import com.fasterxml.jackson.annotation.JsonCreator;
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
    private String linkName = "";

    /** The entry's size. */
    private long size;

    /** The entry's directory flag. */
    private boolean isDirectory;

    /** Created for Jackson deserialization. */
    @JsonCreator
    private MemoryArchiveEntry() {
        this("", "");
    }

    /**
     * Creates a new {@code MemoryArchiveEntry}.
     *
     * @param name the entry's name
     */
    public MemoryArchiveEntry(final String name, final String content) {
        this.name = name;
        this.content = content;
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

    /**
     * Sets this entry's link name.
     *
     * @param link the link name to use.
     */
    public void setLinkName(final String link) {
        this.linkName = link;
    }

    /** @inheritDoc */
    @Override
    public long getSize() {
        return size;
    }

    /**
     * Sets this entry's file size.
     *
     * @param size This entry's new file size.
     * @throws IllegalArgumentException if the size is &lt; 0.
     */
    public void setSize(final long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Size is out of range: " + size);
        }
        this.size = size;
    }

    /** @inheritDoc */
    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    /**
     * Sets this entry's directory flag.
     *
     * @param isDirectory This entry's new directory flag.
     */
    public void setDirectory(final boolean isDirectory) {
        this.isDirectory = isDirectory;
    }
}
