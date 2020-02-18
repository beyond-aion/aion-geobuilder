package com.aionemu.geobuilder.pakaccessor;


import com.aionemu.geobuilder.utils.DataInputStream;

import java.util.Objects;

public class PakCentralDirEnd {

  public static final int HEADER_SIZE = 22;
  public int signature1;
  public int signature2;
  public int diskNum;
  public int firstDisk;
  public int thisDiskCentralDirCount;
  public int totalCentralDirCount;
  public long centralDirSize;
  public long centralDirOffset;
  public int commentLength;

  public static PakCentralDirEnd read(DataInputStream stream) throws Exception {
    PakCentralDirEnd result = new PakCentralDirEnd();
    result.signature1 = stream.readUnsignedShort();
    result.signature2 = stream.readUnsignedShort();
    result.diskNum = stream.readUnsignedShort();
    result.firstDisk = stream.readUnsignedShort();
    result.thisDiskCentralDirCount = stream.readUnsignedShort();
    result.totalCentralDirCount = stream.readUnsignedShort();
    result.centralDirSize = Integer.toUnsignedLong(stream.readInt());
    result.centralDirOffset = Integer.toUnsignedLong(stream.readInt());
    result.commentLength = stream.readUnsignedShort();

    if (result.signature1 != PakConstants.PAK_SIGNATURE1 ||
      result.signature2 != PakConstants.PAK_SIGNATURE2_END) {
      if (result.signature1 != PakConstants.ZIP_SIGNATURE1 ||
        result.signature2 != PakConstants.ZIP_SIGNATURE2_END) {
        throw new Exception("Bad EOCD signature " + result.signature1 + " " + result.signature2);
      }
    }

    if (result.diskNum != 0) {
      throw new Exception("Expected disk 0 but found " + result.diskNum + " . Multi disk not supported.");
    }
    if (result.thisDiskCentralDirCount == 0) {
      throw new Exception("Unexpected empty dir count.");
    }
    if (result.thisDiskCentralDirCount != result.totalCentralDirCount) {
      throw new Exception("Dir count("+result.thisDiskCentralDirCount+") does not match total dir count("+result.totalCentralDirCount+").");
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PakCentralDirEnd that = (PakCentralDirEnd) o;
    return signature1 == that.signature1 &&
        signature2 == that.signature2 &&
        diskNum == that.diskNum &&
        firstDisk == that.firstDisk &&
        thisDiskCentralDirCount == that.thisDiskCentralDirCount &&
        totalCentralDirCount == that.totalCentralDirCount &&
        centralDirSize == that.centralDirSize &&
        centralDirOffset == that.centralDirOffset &&
        commentLength == that.commentLength;
  }

  @Override
  public int hashCode() {
    return Objects.hash(signature1, signature2, diskNum, firstDisk, thisDiskCentralDirCount, totalCentralDirCount, centralDirSize, centralDirOffset, commentLength);
  }
}
