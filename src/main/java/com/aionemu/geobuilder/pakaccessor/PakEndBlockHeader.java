package com.aionemu.geobuilder.pakaccessor;

import com.aionemu.geobuilder.utils.DataInputStream;
import com.aionemu.geobuilder.utils.DataOutputStream;

import java.io.IOException;

class PakEndBlockHeader extends PakBlock {

  int diskNum;
  int firstDisk;
  int thisDiskCentralDirCount;
  int totalCentralDirCount;
  long centralDirSize;
  long centralDirOffset;
  int commentLength;

  public void read(DataInputStream stream) throws IOException {
    diskNum = Short.toUnsignedInt(stream.readShort());
    firstDisk = Short.toUnsignedInt(stream.readShort());
    thisDiskCentralDirCount = Short.toUnsignedInt(stream.readShort());
    totalCentralDirCount = Short.toUnsignedInt(stream.readShort());
    centralDirSize = Integer.toUnsignedLong(stream.readInt());
    centralDirOffset = Integer.toUnsignedLong(stream.readInt());
    commentLength = Short.toUnsignedInt(stream.readShort());
  }

  public void write(DataOutputStream stream) throws IOException {
    stream.writeShort(diskNum);
    stream.writeShort(firstDisk);
    stream.writeShort(thisDiskCentralDirCount);
    stream.writeShort(totalCentralDirCount);
    stream.writeInt((int) centralDirSize);
    stream.writeInt((int) centralDirOffset);
    stream.writeShort(commentLength);
  }

  long getBodySize() {
    return 0;
  }
}
