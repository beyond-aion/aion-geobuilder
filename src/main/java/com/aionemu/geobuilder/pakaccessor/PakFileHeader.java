package com.aionemu.geobuilder.pakaccessor;

import com.aionemu.geobuilder.utils.DataInputStream;
import com.aionemu.geobuilder.utils.DataOutputStream;

import java.io.IOException;

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

  public void read(DataInputStream stream) throws IOException {
    extractVersion = stream.readByte();
    extractSystem = stream.readByte();
    flags = stream.readShort();
    compMethod = stream.readShort();
    lastModTime = stream.readShort();
    lastModDate = stream.readShort();
    crc = stream.readInt();
    compressedSz = stream.readInt();
    uncompressedSz = stream.readInt();
    fileNameSz = stream.readShort();
    extraFieldsSz = stream.readShort();

    fileNameBytes = new byte[fileNameSz];
    if (stream.read(fileNameBytes) != fileNameSz)
      throw new PakFileFormatException("Cannot read file name from File block");
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
