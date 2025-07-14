package io.github.compress4j.compressors;

import io.github.compress4j.compressors.bzip2.BZip2Compressor;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

class BZip2CompressorTest extends  AbstractCompressorTest {

    @Override
    protected Compressor compressorBuilder(Path targetPath) throws IOException {
        return new BZip2Compressor.BZip2CompressorBuilder(targetPath).build();
    }

    @Override
    protected String getCompressorExtension() {
        return ".bz2";
    }
    @Override
    protected void appacheCompressor(Path sourceFile, Path expectedPath) throws IOException{
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
