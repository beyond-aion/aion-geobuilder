package com.aionemu.geobuilder.pakaccessor;

import com.aionemu.geobuilder.utils.DataInputStream;

public class PakFileEntry {

  public int signature1;
  public int signature2;
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

  public String fileName;

  public static PakFileEntry read(DataInputStream stream) throws Exception {
    PakFileEntry result = new PakFileEntry();
    result.signature1 = stream.readUnsignedShort();
    result.signature2 = stream.readUnsignedShort();
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

    byte[] fileNameBytes = new byte[result.filenameLength];
    stream.readFully(fileNameBytes);
    String name = new String(fileNameBytes);
    result.fileName = name.toLowerCase().replace('/', '\\');

    return result;
  }
}
