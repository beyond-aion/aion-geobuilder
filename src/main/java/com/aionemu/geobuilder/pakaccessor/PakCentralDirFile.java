package com.aionemu.geobuilder.pakaccessor;


import com.aionemu.geobuilder.utils.DataInputStream;

import java.util.Objects;

public class PakCentralDirFile {

  public boolean isAionFormat; // true for aion header, false for zip format

  public int signature1;
  public int signature2;
  public int createVersion;
  public int extractVersion;
  public int flags;
  public int compressionMethod;
  public int time;
  public int date;
  public long crc;
  public long compressedSize;
  public long uncompressedSize;
  public int filenameLength;
  public int extraFieldLength;
  public int fileCommentLength;
  public int diskNumStart;
  public int internalFileAttr;
  public long externalFileAttr;
  public long localHeaderOffset;

  public String fileName;
  
  public static PakCentralDirFile read(DataInputStream stream) throws Exception{
    PakCentralDirFile result = new PakCentralDirFile();
    result.signature1 = stream.readUnsignedShort();
    result.signature2 = stream.readUnsignedShort();
    result.createVersion = stream.readUnsignedShort();
    result.extractVersion = stream.readUnsignedShort();
    result.flags = stream.readUnsignedShort();
    result.compressionMethod = stream.readUnsignedShort();
    result.time = stream.readUnsignedShort();
    result.date = stream.readUnsignedShort();
    result.crc = Integer.toUnsignedLong(stream.readInt());
    result.compressedSize = Integer.toUnsignedLong(stream.readInt());
    result.uncompressedSize = Integer.toUnsignedLong(stream.readInt());
    result.filenameLength = stream.readUnsignedShort();
    result.extraFieldLength = stream.readUnsignedShort();
    result.fileCommentLength = stream.readUnsignedShort();
    result.diskNumStart = stream.readUnsignedShort();
    result.internalFileAttr = stream.readUnsignedShort();
    result.externalFileAttr = Integer.toUnsignedLong(stream.readInt());
    result.localHeaderOffset = Integer.toUnsignedLong(stream.readInt());

    byte[] fileNameBytes = new byte[result.filenameLength];
    stream.readFully(fileNameBytes);
    String name = new String(fileNameBytes);
    result.fileName = name.toLowerCase().replace('/', '\\').trim();

    if (result.signature1 == PakConstants.PAK_SIGNATURE1 &&
      result.signature2 == PakConstants.PAK_SIGNATURE2_DIR) {
      result.isAionFormat = true;
    } else {
      if (result.signature1 != PakConstants.ZIP_SIGNATURE1 ||
        result.signature2 != PakConstants.ZIP_SIGNATURE2_DIR)
        throw new Exception("bad central dir signature");
    }

    if (result.extraFieldLength != 0) {
      byte[] b = new byte[result.extraFieldLength];
      stream.readFully(b);
    }
    if (result.fileCommentLength != 0) {
      throw new Exception("file comment not supported");
    }

    if (result.diskNumStart != 0) {
      throw new Exception("disk num not supported");
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PakCentralDirFile that = (PakCentralDirFile) o;
    return isAionFormat == that.isAionFormat &&
        signature1 == that.signature1 &&
        signature2 == that.signature2 &&
        createVersion == that.createVersion &&
        extractVersion == that.extractVersion &&
        flags == that.flags &&
        compressionMethod == that.compressionMethod &&
        time == that.time &&
        date == that.date &&
        crc == that.crc &&
        compressedSize == that.compressedSize &&
        uncompressedSize == that.uncompressedSize &&
        filenameLength == that.filenameLength &&
        extraFieldLength == that.extraFieldLength &&
        fileCommentLength == that.fileCommentLength &&
        diskNumStart == that.diskNumStart &&
        internalFileAttr == that.internalFileAttr &&
        externalFileAttr == that.externalFileAttr &&
        localHeaderOffset == that.localHeaderOffset &&
        fileName.equals(that.fileName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isAionFormat, signature1, signature2, createVersion, extractVersion, flags, compressionMethod, time, date, crc, compressedSize, uncompressedSize, filenameLength, extraFieldLength, fileCommentLength, diskNumStart, internalFileAttr, externalFileAttr, localHeaderOffset, fileName);
  }
}
