package com.aionemu.geobuilder.cgfData;

import com.aionemu.geobuilder.utils.DataInputStream;

import java.io.IOException;

public class CgfChunkHeader {
  public int chunkType, chunkVersion, chunkOffset, chunkId;

  public static CgfChunkHeader read(DataInputStream stream) throws IOException {
    CgfChunkHeader header = new CgfChunkHeader();
    header.chunkType = stream.readInt();
    header.chunkVersion = stream.readInt();
    header.chunkOffset = stream.readInt();
    header.chunkId = stream.readInt();
    return header;
  }
}