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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.assertj.core.matcher.AssertionMatcher;
import org.mockito.ArgumentMatchers;

/**
 * Allow using AssertJ assertions for mockito matchers.
 *
 * @see AssertionMatcher
 */
public final class AssertJMatcher {
    private AssertJMatcher() {}

    // TODO: remove when https://github.com/mockito/mockito/issues/3307 is fixed
    @SafeVarargs
    public static <T> T assertArgs(Consumer<T>... assertions) {
        AtomicInteger counter = new AtomicInteger();
        return ArgumentMatchers.argThat(actual -> {
            try {
                int index = counter.getAndIncrement();
                if (assertions.length > index) {
                    assertions[index].accept(actual);
                }
            } catch (ClassCastException ignored) {
                // ignore
            }
            return true;
        });
    }
}
