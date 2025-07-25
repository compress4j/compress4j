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
package io.github.compress4j.compressors;

import io.github.compress4j.compressors.bzip2.BZip2Decompressor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

public class BZip2DecompressionTest extends AbstractDecompressorTest {
    @Override
    protected Decompressor<BZip2CompressorInputStream> decompressorBuilder(Path sourceFile) throws IOException {
        return new BZip2Decompressor.BZip2DecompressorBuilder(sourceFile).build();
    }

    @Override
    protected void apacheCompressor(Path sourceFile, Path compressedFile) throws IOException {
        try (InputStream in = new FileInputStream(sourceFile.toFile());
                OutputStream out = new FileOutputStream(compressedFile.toFile());
                BZip2CompressorOutputStream bzipOut = new BZip2CompressorOutputStream(out)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                bzipOut.write(buffer, 0, bytesRead);
            }
        }
    }
}
