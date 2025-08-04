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
package io.github.compress4j.test.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

public class BZip2Helper {

    /**
     * A helper method to generate a valid, in-memory BZip2 InputStream from a String.
     *
     * @param content The string content to compress.
     * @return An InputStream containing the BZip2-compressed content.
     * @throws IOException if there is an I/O error.
     */
    public static InputStream createBZip2InputStream(String content) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (BZip2CompressorOutputStream outputStream = new BZip2CompressorOutputStream(byteArrayOutputStream)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }
}
