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

import java.util.Date;
import org.apache.commons.compress.archivers.ArchiveEntry;

public final class MemoryArchiveEntry implements ArchiveEntry {

    private final String name;

    public MemoryArchiveEntry(final String pName) {
        name = pName;
    }

    @Override
    public Date getLastModifiedDate() {
        return new Date();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isDirectory() {
        // TODO Auto-generated method stub
        return false;
    }
}
