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
package io.github.compress4j.compressors.bzip2;

import io.github.compress4j.compressors.AbstractCompressorTest;
import io.github.compress4j.compressors.Compressor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

class BZip2CompressorTest extends AbstractCompressorTest {

    @Override
    protected Compressor<BZip2CompressorOutputStream> compressorBuilder(Path targetPath) throws IOException {
        return new BZip2Compressor.BZip2CompressorBuilder(targetPath).build();
    }

    @Override
    protected void apacheCompressor(Path sourceFile, Path expectedPath) throws IOException {
        try (InputStream in = new FileInputStream(sourceFile.toFile());
                OutputStream out = new FileOutputStream(expectedPath.toFile());
                BZip2CompressorOutputStream bzipOut = new BZip2CompressorOutputStream(out)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                bzipOut.write(buffer, 0, bytesRead);
            }
        }
    }
}
