package io.github.compress4j.compressors;

import java.nio.file.Path;

public abstract class AbstractCompressorTest {


    protected abstract Compressor compressor();

    protected abstract long write();

    protected abstract Path appacheCompressor();

}
