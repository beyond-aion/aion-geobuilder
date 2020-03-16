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

  byte fileNameBytes[];

  long blockStartPosition;

  public int getSize() {
    return 4 + 26 + fileNameSz + extraFieldsSz + compressedSz;
  }

  public int getBodyShift() {
    return 4 + 26 + fileNameSz + extraFieldsSz;
  }

  public void read(ByteBuffer stream) throws IOException {
    extractVersion = stream.get();
    extractSystem = stream.get();
    flags = stream.getShort();
    compMethod = stream.getShort();
    lastModTime = stream.getShort();
    lastModDate = stream.getShort();
    crc = stream.getInt();
    compressedSz = stream.getInt();
    uncompressedSz = stream.getInt();
    fileNameSz = stream.getShort();
    extraFieldsSz = stream.getShort();

    fileNameBytes = new byte[fileNameSz];
    stream.get(fileNameBytes);
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
