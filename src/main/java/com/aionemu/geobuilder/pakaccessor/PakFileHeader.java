package com.aionemu.geobuilder.pakaccessor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

class PakFileHeader extends PakBlock {

  byte extractVersion;
  byte extractSystem;
  short flags;
  short compMethod;
  short lastModTime;
  short lastModDate;
  int crc;
  int compressedSz;
  int uncompressedSz;
  short fileNameSz;
  short extraFieldsSz;

  byte[] fileNameBytes;

  long blockStartPosition;

  public int getSize() {
    return 4 + 26 + fileNameSz + extraFieldsSz + compressedSz;
  }

  public int getBodyShift() {
    return 4 + 26 + fileNameSz + extraFieldsSz;
  }

  public void read(ByteBuffer buffer) throws IOException {
    extractVersion = buffer.get();
    extractSystem = buffer.get();
    flags = buffer.getShort();
    compMethod = buffer.getShort();
    lastModTime = buffer.getShort();
    lastModDate = buffer.getShort();
    crc = buffer.getInt();
    compressedSz = buffer.getInt();
    uncompressedSz = buffer.getInt();
    fileNameSz = buffer.getShort();
    extraFieldsSz = buffer.getShort();

    fileNameBytes = new byte[fileNameSz];
    buffer.get(fileNameBytes);
    if (compMethod != 0 && compMethod != 8)
      throw new PakFileFormatException("Unknown compression method " + compMethod);
  }

  public void write(DataOutputStream stream) throws IOException {
    stream.writeByte(extractVersion);
    stream.writeByte(extractSystem);
    stream.writeShort(flags);
    stream.writeShort(compMethod);
    stream.writeShort(lastModTime);
    stream.writeShort(lastModDate);
    stream.writeInt(crc);
    stream.writeInt(compressedSz);
    stream.writeInt(uncompressedSz);
    stream.writeShort(fileNameSz);
    stream.writeShort(extraFieldsSz);

    stream.write(fileNameBytes);
  }


  long getBodySize() {
    return extraFieldsSz + compressedSz;
  }
}
