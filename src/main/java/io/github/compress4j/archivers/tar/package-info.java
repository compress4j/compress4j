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
/**
 * Provides stream classes for reading and writing archives using the TAR format.
 *
 * <p>There are many different format dialects that call themselves TAR. The classes of this package can read and write
 * archives in the traditional pre-POSIX <strong>ustar</strong> format and support GNU specific extensions for long file
 * names that GNU tar itself by now refers to as <strong>oldgnu</strong>.
 *
 * @since 2.2
 */
@Nonnull
package io.github.compress4j.archivers.tar;

import jakarta.annotation.Nonnull;
