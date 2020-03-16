package com.aionemu.geobuilder.cgfData;

import java.nio.ByteBuffer;

public class CgfChunkHeader {
  public int chunkType, chunkVersion, chunkOffset, chunkId;

  public static CgfChunkHeader read(ByteBuffer bb) {
    CgfChunkHeader header = new CgfChunkHeader();
    header.chunkType = bb.getInt();
    header.chunkVersion = bb.getInt();
    header.chunkOffset = bb.getInt();
    header.chunkId = bb.getInt();
    return header;
  }
}