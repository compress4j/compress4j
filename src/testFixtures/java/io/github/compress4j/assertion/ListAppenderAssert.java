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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class ListAppenderAssert extends AbstractAssert<ListAppenderAssert, ListAppender<ILoggingEvent>> {

    protected ListAppenderAssert(ListAppender<ILoggingEvent> listAppender) {
        super(listAppender, ListAppenderAssert.class);
    }

    public static ListAppenderAssert assertThat(ListAppender<ILoggingEvent> listAppender) {
        return new ListAppenderAssert(listAppender);
    }

    public ListAppenderAssert contains(String message, Level level) {
        Assertions.assertThat(actual.list)
                .anyMatch(event ->
                        event.toString().contains(message) && event.getLevel().equals(level));
        return this;
    }
}
