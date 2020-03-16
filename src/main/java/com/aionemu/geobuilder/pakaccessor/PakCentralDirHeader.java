package com.aionemu.geobuilder.pakaccessor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

class PakCentralDirHeader extends PakBlock {

  byte createVersion;
  byte createSystem;
  byte extractVersion;
  byte extractSystem;
  short flags;
  short compType;
  short time;
  short date;
  int crc;
  int compressedSz;
  int uncompressedSz;
  short fileNameSz;
  short extraFieldsSz;
  short commentSz;
  short diskNumStart;
  short intFileAttr;
  int extFileAttr;
  int localHeaderOffset;

  byte fileNameBytes[];

  public void read(ByteBuffer stream) {
    createVersion = stream.get();
    createSystem = stream.get();
    extractVersion = stream.get();
    extractSystem = stream.get();
    flags = stream.getShort();
    compType = stream.getShort();
    time = stream.getShort();
    date = stream.getShort();
    crc = stream.getInt();
    compressedSz = stream.getInt();
    uncompressedSz = stream.getInt();
    fileNameSz = stream.getShort();
    extraFieldsSz = stream.getShort();
    commentSz = stream.getShort();
    diskNumStart = stream.getShort();
    intFileAttr = stream.getShort();
    extFileAttr = stream.getInt();
    localHeaderOffset = stream.getInt();

    fileNameBytes = new byte[fileNameSz];
    stream.get(fileNameBytes);
  }

  public void write(DataOutputStream stream) throws IOException {
    stream.writeByte(createVersion);
    stream.writeByte(createSystem);
    stream.writeByte(extractVersion);
    stream.writeByte(extractSystem);
    stream.writeShort(flags);
    stream.writeShort(compType);
    stream.writeShort(time);
    stream.writeShort(date);
    stream.writeInt(crc);
    stream.writeInt(compressedSz);
    stream.writeInt(uncompressedSz);
    stream.writeShort(fileNameSz);
    stream.writeShort(extraFieldsSz);
    stream.writeShort(commentSz);
    stream.writeShort(diskNumStart);
    stream.writeShort(intFileAttr);
    stream.writeInt(extFileAttr);
    stream.writeInt(localHeaderOffset);

    stream.write(fileNameBytes);
  }

  long getBodySize() {
    return extraFieldsSz + commentSz;
  }
}
