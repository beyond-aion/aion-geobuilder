package com.aionemu.geobuilder.pakaccessor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

class PakEndBlockHeader extends PakBlock {

  int diskNum;
  int firstDisk;
  int thisDiskCentralDirCount;
  int totalCentralDirCount;
  long centralDirSize;
  long centralDirOffset;
  int commentLength;

  public void read(ByteBuffer stream) {
    diskNum = Short.toUnsignedInt(stream.getShort());
    firstDisk = Short.toUnsignedInt(stream.getShort());
    thisDiskCentralDirCount = Short.toUnsignedInt(stream.getShort());
    totalCentralDirCount = Short.toUnsignedInt(stream.getShort());
    centralDirSize = Integer.toUnsignedLong(stream.getInt());
    centralDirOffset = Integer.toUnsignedLong(stream.getInt());
    commentLength = Short.toUnsignedInt(stream.getShort());
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
