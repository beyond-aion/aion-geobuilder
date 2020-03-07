package com.aionemu.geobuilder.pakaccessor;

import com.aionemu.geobuilder.utils.CommonUtils;
import com.aionemu.geobuilder.utils.DataInputStream;
import com.aionemu.geobuilder.utils.DataOutputStream;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import static com.aionemu.geobuilder.pakaccessor.PakConstants.*;

public class PakFile implements AutoCloseable {

  private final FileInputStream pakFileStream;
  private final Set<PakBlock> pakBlocksSet = new LinkedHashSet<>();
  private final Map<String, PakFileHeader> filesMap = new LinkedHashMap<>();
  private final Set<PakCentralDirHeader> dirsSet = new LinkedHashSet<>();
  private byte[] decryptTable;
  private Function<Integer, Integer> decryptTableOffset;

  public static PakFile open(File pakFile) throws IOException {
    return open(pakFile, null);
  }

  public static PakFile open(File pakFile, File basePath) throws IOException {
    String prefix = "";
    if (basePath != null) {
      prefix = pakFile.getParentFile().getCanonicalPath().substring(basePath.getCanonicalPath().length());
      prefix = prefix.replace('\\', '/').toLowerCase();
      if (prefix.startsWith("/"))
        prefix = prefix.substring(1);
      if (!prefix.endsWith("/"))
        prefix += '/';
    }
    return new PakFile(pakFile, prefix);
  }

  private PakFile(File file, String prefix) throws IOException {
    pakFileStream = new FileInputStream(file);
    init(prefix);
  }

  private void init(String prefix) throws IOException {
    DataInputStream pakInputStream = new DataInputStream(pakFileStream);
    do {
      long blockStartPosition = pakFileStream.getChannel().position();
      short signature1 = pakInputStream.readShort();
      if (signature1 != PAK_SIGNATURE1)
        throw new PakFileFormatException("Unknown Pak signature1: " + signature1);
      short signature2 = pakInputStream.readShort();
      switch (signature2) {
        case PAK_SIGNATURE2_FILE:
          PakFileHeader pakFileHeader = new PakFileHeader();
          pakFileHeader.blockStartPosition = blockStartPosition;
          pakFileHeader.read(pakInputStream); // TODO Check bytes count read

          pakFileHeader.bodyStartPosition = pakFileStream.getChannel().position();

          if (pakBlocksSet.isEmpty()) {
            detectVersion(pakFileHeader);
          } else {
            pakFileStream.skip(pakFileHeader.getBodySize());
          }

          String fileName = new String(pakFileHeader.fileNameBytes);
          filesMap.put(prefix + fileName.toLowerCase().replace('\\', '/').trim(), pakFileHeader);
          pakBlocksSet.add(pakFileHeader);
          break;
        case PAK_SIGNATURE2_DIR:
          PakCentralDirHeader pakCentralDirHeader = new PakCentralDirHeader();
          pakCentralDirHeader.read(pakInputStream); // TODO Check bytes count read

          if (pakCentralDirHeader.getBodySize() > 0) {
            pakCentralDirHeader.bodyStartPosition = pakFileStream.getChannel().position();

            pakFileStream.skip(pakCentralDirHeader.getBodySize());
          } else {
            pakCentralDirHeader.bodyStartPosition = 0;
          }

          dirsSet.add(pakCentralDirHeader);
          pakBlocksSet.add(pakCentralDirHeader);
          break;
        case PAK_SIGNATURE2_END:
          PakEndBlockHeader pakEndBlockHeader = new PakEndBlockHeader();
          pakEndBlockHeader.read(pakInputStream); // TODO Check bytes count read
          pakBlocksSet.add(pakEndBlockHeader);
          if (pakInputStream.available() == 0)
            return;
          System.err.println("Found content after pak end marker, trying to read...");
        default:
          throw new PakFileFormatException("Unknown Pak signature2: " + signature2);
      }
    } while (true);
  }

  public void close() throws IOException {
    pakFileStream.close();
    pakBlocksSet.clear();
    filesMap.clear();
    dirsSet.clear();
  }

  public byte[] unpak(String fileName) throws IOException {
    PakFileHeader pakFileHeader = filesMap.get(fileName);
    return pakFileHeader == null ? null : unpak(pakFileHeader, decryptTable, decryptTableOffset);
  }

  private byte[] unpak(PakFileHeader pakFileHeader, byte[] decryptTable, Function<Integer, Integer> decryptTableOffset) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream(pakFileHeader.uncompressedSz);
    DataOutputStream outputStream = new DataOutputStream(pakFileHeader.compMethod == 0 ? out : new InflaterOutputStream(out, new Inflater(true)));
    writeBody(outputStream, pakFileHeader, decryptTable, decryptTableOffset);
    return out.toByteArray();
  }

  public void convertToZip(OutputStream outputStream) throws IOException {
    DataOutputStream zipOutputStream = new DataOutputStream(outputStream);
    for (PakBlock block : pakBlocksSet) {
      zipOutputStream.writeShort(ZIP_SIGNATURE1);
      if (block instanceof PakFileHeader) {
        PakFileHeader pakFileHeader = (PakFileHeader) block;

        zipOutputStream.writeShort(ZIP_SIGNATURE2_FILE);
        block.write(zipOutputStream);

        writeBody(zipOutputStream, pakFileHeader, decryptTable, decryptTableOffset);
      } else if (block instanceof PakCentralDirHeader) {
        zipOutputStream.writeShort(ZIP_SIGNATURE2_DIR);
        block.write(zipOutputStream);

        int bodySize = (int) block.getBodySize();
        if (bodySize > 0) {
          synchronized (pakFileStream) {
            pakFileStream.getChannel().position(block.bodyStartPosition);
            CommonUtils.bufferedCopy(pakFileStream, zipOutputStream, bodySize);
          }
        }
      } else if (block instanceof PakEndBlockHeader) {
        zipOutputStream.writeShort(ZIP_SIGNATURE2_END);
        block.write(zipOutputStream);
      } else
        throw new PakFileFormatException("Unsupported block type: " + block.getClass().toString());
    }
  }

  private void writeBody(DataOutputStream os, PakFileHeader pakFileHeader, byte[] decryptTable, Function<Integer, Integer> decryptTableOffset) throws IOException {
    synchronized (pakFileStream) {
      pakFileStream.getChannel().position(pakFileHeader.bodyStartPosition);

      // write extra block
      if (pakFileHeader.extraFieldsSz > 0)
        CommonUtils.bufferedCopy(pakFileStream, os, pakFileHeader.extraFieldsSz);

      int bodySize = pakFileHeader.compressedSz;
      // decrypt and write top of body block
      byte[] decryptBlock = decrypt(pakFileStream, bodySize, decryptTable, decryptTableOffset);
      os.write(decryptBlock);

      // write rest of body
      CommonUtils.bufferedCopy(pakFileStream, os, bodySize - decryptBlock.length);
    }
  }

  private static byte[] decrypt(FileInputStream fis, int bodySize, byte[] decryptTable, Function<Integer, Integer> decryptTableOffset) throws IOException {
    // Decrypt top of body block
    int decryptBlockSize = Math.min(bodySize, 32);
    byte[] decryptBlock = new byte[decryptBlockSize];
    fis.read(decryptBlock); // TODO Check bytes count read

    int offset = decryptTableOffset.apply(bodySize);
    for (int i = 0; i < decryptBlockSize; i++) {
      decryptBlock[i] ^= decryptTable[offset + i];
    }
    return decryptBlock;
  }

  public Set<String> getFileNames() {
    return filesMap.keySet();
  }

  public int getUnpakedFileSize(String fileName) {
    PakFileHeader header = filesMap.get(fileName);
    return header == null ? -1 : header.uncompressedSz;
  }

  private void detectVersion(PakFileHeader pakFileHeader) throws PakFileFormatException {
    CRC32 checksum = new CRC32();
    try {
      byte[] decrypted = unpak(pakFileHeader, table2, table2Offset);
      checksum.update(decrypted);
      if (decrypted.length == pakFileHeader.uncompressedSz && Integer.toUnsignedLong(pakFileHeader.crc) == checksum.getValue()) {
        decryptTable = table2;
        decryptTableOffset = table2Offset;
        return;
      }
    } catch (Exception ignored) {
    }
    checksum.reset();
    try {
      byte[] decrypted = unpak(pakFileHeader, table1, table1Offset);
      checksum.update(decrypted);
      if (decrypted.length == pakFileHeader.uncompressedSz && Integer.toUnsignedLong(pakFileHeader.crc) == checksum.getValue()) {
        decryptTable = table1;
        decryptTableOffset = table1Offset;
        return;
      }
    } catch (Exception ignored) {
    }
    throw new PakFileFormatException("Unknown Aion version");
  }
}
