# Fixture provenance

Fixtures marked "upstream" are copied unmodified from https://github.com/apache/commons-compress (`src/test/resources`), licensed under the Apache License 2.0, the same licence as this project.

| Fixture | Source | Notes |
|---|---|---|
| `archives/upstream-bla.tar.lzma` | upstream `bla.tar.lzma` | unmodified |
| `archives/archive.tar.lzma` | generated | `lzma -c` of `archives/archive.tar` |
| `compression/compress.txt.lzma` | generated | `lzma -c` of the decompressed `compression/compress.txt.xz` |
| `archives/upstream-bla.tar.lz4` | upstream `bla.tar.lz4` | unmodified; framed LZ4 |
| `archives/upstream-bla.tar.block_lz4` | upstream `bla.tar.block_lz4` | unmodified; block LZ4 |
| `archives/archive.tar.lz4` | generated | `FramedLZ4CompressorOutputStream` (Commons Compress 1.28.0) over `archives/archive.tar` |
| `compression/compress.txt.lz4` | generated | `FramedLZ4CompressorOutputStream` over the decompressed `compress.txt.xz` |
| `compression/compress.txt.block_lz4` | generated | `BlockLZ4CompressorOutputStream` over the decompressed `compress.txt.xz` |
| `archives/upstream-bla.tar.sz` | upstream `bla.tar.sz` | unmodified; framed Snappy |
| `compression/compress.txt.sz` | generated | `FramedSnappyCompressorOutputStream` (Commons Compress 1.28.0) over the decompressed `compress.txt.xz` |
