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
package org.compress4j.test.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;

public class MemoryAppender extends ListAppender<ILoggingEvent> {
    public void reset() {
        this.list.clear();
    }

    public boolean contains(String string, Level level) {
        return this.list.stream()
                .anyMatch(event ->
                        event.toString().contains(string) && event.getLevel().equals(level));
    }

    public List<ILoggingEvent> find(String string) {
        return this.list.stream()
                .filter(event -> event.toString().contains(string))
                .toList();
    }

    public List<ILoggingEvent> find(String string, Level level) {
        return this.list.stream()
                .filter(event ->
                        event.toString().contains(string) && event.getLevel().equals(level))
                .toList();
    }

    public int getSize() {
        return this.list.size();
    }
}
