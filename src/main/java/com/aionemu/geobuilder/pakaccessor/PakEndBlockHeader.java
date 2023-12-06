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

  public void read(ByteBuffer buffer) {
    diskNum = Short.toUnsignedInt(buffer.getShort());
    firstDisk = Short.toUnsignedInt(buffer.getShort());
    thisDiskCentralDirCount = Short.toUnsignedInt(buffer.getShort());
    totalCentralDirCount = Short.toUnsignedInt(buffer.getShort());
    centralDirSize = Integer.toUnsignedLong(buffer.getInt());
    centralDirOffset = Integer.toUnsignedLong(buffer.getInt());
    commentLength = Short.toUnsignedInt(buffer.getShort());
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
