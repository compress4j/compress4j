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
package io.github.compress4j.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.compress4j.exceptions.MissingArchiveDependencyException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ArchiverDependencyCheckerWithoutXzTest {

    private static final String XZ_PACKAGE = "org.tukaani.";
    private static final String YOU_NEED_XZ_JAVA =
            " In addition to Apache Commons Compress you need the XZ for Java library"
                    + " - see https://tukaani.org/xz/java.html";

    private static final class XzHidingClassLoader extends URLClassLoader {
        XzHidingClassLoader(URL[] urls) {
            super(urls, ClassLoader.getPlatformClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith(XZ_PACKAGE)) {
                throw new ClassNotFoundException(name);
            }
            return super.loadClass(name, resolve);
        }
    }

    private static URL[] classpathUrls() {
        return Stream.of(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(entry -> toUrl(Path.of(entry)))
                .toArray(URL[]::new);
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Throwable checkWithoutXz(String entryName) throws Exception {
        try (XzHidingClassLoader loader = new XzHidingClassLoader(classpathUrls())) {
            Class<?> checker = loader.loadClass(ArchiverDependencyChecker.class.getName());
            checker.getMethod("check", String.class).invoke(null, entryName);
            throw new AssertionError("Expected check to fail without xz");
        } catch (InvocationTargetException e) {
            return e.getCause();
        }
    }

    @ParameterizedTest
    @CsvSource({"xz,XZ", "lzma,LZMA"})
    void shouldRejectWhenXzIsMissing(String entryName, String format) throws Exception {
        Throwable failure = checkWithoutXz(entryName);

        assertThat(failure.getClass().getName()).isEqualTo(MissingArchiveDependencyException.class.getName());
        assertThat(failure).hasMessage(format + " compression is not available." + YOU_NEED_XZ_JAVA);
    }
}
