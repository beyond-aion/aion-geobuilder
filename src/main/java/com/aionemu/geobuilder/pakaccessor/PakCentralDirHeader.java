package com.aionemu.geobuilder.pakaccessor;

import com.aionemu.geobuilder.utils.DataInputStream;
import com.aionemu.geobuilder.utils.DataOutputStream;

import java.io.IOException;

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

  public void read(DataInputStream stream) throws IOException {
    createVersion = stream.readByte();
    createSystem = stream.readByte();
    extractVersion = stream.readByte();
    extractSystem = stream.readByte();
    flags = stream.readShort();
    compType = stream.readShort();
    time = stream.readShort();
    date = stream.readShort();
    crc = stream.readInt();
    compressedSz = stream.readInt();
    uncompressedSz = stream.readInt();
    fileNameSz = stream.readShort();
    extraFieldsSz = stream.readShort();
    commentSz = stream.readShort();
    diskNumStart = stream.readShort();
    intFileAttr = stream.readShort();
    extFileAttr = stream.readInt();
    localHeaderOffset = stream.readInt();

    fileNameBytes = new byte[fileNameSz];
    if (stream.read(fileNameBytes) != fileNameSz)
      throw new IOException("Cannot read file name from Dir block");
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
