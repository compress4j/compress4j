package io.github.compress4j.compressors.gzip;

import io.github.compress4j.compressors.Decompressor;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.IOException;

public class GZipDecompressor extends Decompressor<GzipCompressorInputStream> {

    protected <B extends DecompressorBuilder<GzipCompressorInputStream, D, B>, D extends Decompressor<GzipCompressorInputStream>> GZipDecompressor(B builder) throws IOException {
        super(builder);
    }

    protected GZipDecompressor(GzipCompressorInputStream compressorInputStream) {
        super(compressorInputStream);
    }

    public static class GZipDecompressorBuilder
            extends Decompressor.DecompressorBuilder<
            GzipCompressorInputStream,
            GZipDecompressor,
            GZipDecompressorBuilder
            > {

        protected GZipDecompressorBuilder(GzipCompressorInputStream inputStream) {
            super(inputStream);
        }

        @Override
        protected GZipDecompressor.GZipDecompressorBuilder getThis() {
            return this;
        }

        @Override
        public GZipDecompressor build() throws IOException {
            return new GZipDecompressor(this);
        }
    }
}
