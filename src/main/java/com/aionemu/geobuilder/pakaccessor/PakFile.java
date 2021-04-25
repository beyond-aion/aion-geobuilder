package com.aionemu.geobuilder.pakaccessor;

import com.aionemu.geobuilder.utils.PathSanitizer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
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

  private final FileChannel fileChannel;
  private final MappedByteBuffer buffer;
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
      prefix = PathSanitizer.sanitize(prefix);
      if (prefix.startsWith("/"))
        prefix = prefix.substring(1);
      if (!prefix.endsWith("/"))
        prefix += '/';
    }
    return new PakFile(pakFile, prefix);
  }

  private PakFile(File file, String prefix) throws IOException {
    fileChannel = new RandomAccessFile(file, "r").getChannel();
    long size = fileChannel.size();
    if (size < 0 || size > Integer.MAX_VALUE) // buffer.position() is int based
      throw new IllegalArgumentException(file + " is too big. More than 2 GB cannot be addressed.");
    buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    init(prefix);
  }

  private void init(String prefix) throws IOException {
    do {
      int blockStartPosition = buffer.position();
      short signature1 = buffer.getShort();
      if (signature1 != PAK_SIGNATURE1)
        throw new PakFileFormatException("Unknown Pak signature1: " + signature1);
      short signature2 = buffer.getShort();
      switch (signature2) {
        case PAK_SIGNATURE2_FILE:
          PakFileHeader pakFileHeader = new PakFileHeader();
          pakFileHeader.blockStartPosition = blockStartPosition;
          pakFileHeader.read(buffer);

          pakFileHeader.bodyStartPosition = buffer.position();

          if (pakBlocksSet.isEmpty()) {
            detectVersion(pakFileHeader);
          } else {
            buffer.position(buffer.position() + (int) pakFileHeader.getBodySize());
          }

          String fileName = new String(pakFileHeader.fileNameBytes);
          filesMap.put(prefix + PathSanitizer.sanitize(fileName), pakFileHeader);
          pakBlocksSet.add(pakFileHeader);
          break;
        case PAK_SIGNATURE2_DIR:
          PakCentralDirHeader pakCentralDirHeader = new PakCentralDirHeader();
          pakCentralDirHeader.read(buffer);

          if (pakCentralDirHeader.getBodySize() > 0) {
            pakCentralDirHeader.bodyStartPosition = buffer.position();

            buffer.position(buffer.position() + (int) pakCentralDirHeader.getBodySize());
          } else {
            pakCentralDirHeader.bodyStartPosition = 0;
          }

          dirsSet.add(pakCentralDirHeader);
          pakBlocksSet.add(pakCentralDirHeader);
          break;
        case PAK_SIGNATURE2_END:
          PakEndBlockHeader pakEndBlockHeader = new PakEndBlockHeader();
          pakEndBlockHeader.read(buffer);
          pakBlocksSet.add(pakEndBlockHeader);
          if (buffer.remaining() == 0)
            return;
          System.err.println("Found content after pak end marker, trying to read...");
        default:
          throw new PakFileFormatException("Unknown Pak signature2: " + signature2);
      }
    } while (true);
  }

  public void close() throws IOException {
    fileChannel.close();
    buffer.clear();
    pakBlocksSet.clear();
    filesMap.clear();
    dirsSet.clear();
  }

  public ByteBuffer unpak(String fileName) throws IOException {
    PakFileHeader pakFileHeader = filesMap.get(fileName);
    return pakFileHeader == null ? null : unpak(pakFileHeader, decryptTable, decryptTableOffset);
  }

  private ByteBuffer unpak(PakFileHeader pakFileHeader, byte[] decryptTable, Function<Integer, Integer> decryptTableOffset) throws IOException {
    ByteBufferArrayOutputStream out = new ByteBufferArrayOutputStream(pakFileHeader.uncompressedSz);
    DataOutputStream outputStream = new DataOutputStream(pakFileHeader.compMethod == 0 ? out : new InflaterOutputStream(out, new Inflater(true)));
    writeBody(outputStream, pakFileHeader, decryptTable, decryptTableOffset);
    return out.getByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
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

        write(zipOutputStream, (int) block.bodyStartPosition, (int) block.getBodySize());
      } else if (block instanceof PakEndBlockHeader) {
        zipOutputStream.writeShort(ZIP_SIGNATURE2_END);
        block.write(zipOutputStream);
      } else
        throw new PakFileFormatException("Unsupported block type: " + block.getClass().toString());
    }
  }

  private void writeBody(DataOutputStream os, PakFileHeader pakFileHeader, byte[] decryptTable, Function<Integer, Integer> decryptTableOffset) throws IOException {
    synchronized (buffer) {
      buffer.position((int) pakFileHeader.bodyStartPosition);

      // write extra block if present
      write(os, (int) pakFileHeader.bodyStartPosition, pakFileHeader.extraFieldsSz);

      int bodySize = pakFileHeader.compressedSz;
      // decrypt and write top of body block
      byte[] decryptBlock = decrypt(buffer, bodySize, decryptTable, decryptTableOffset);
      os.write(decryptBlock);

      // write rest of body
      write(os, buffer.position(), bodySize - decryptBlock.length);
    }
  }

  private static byte[] decrypt(ByteBuffer fis, int bodySize, byte[] decryptTable, Function<Integer, Integer> decryptTableOffset) {
    // Decrypt top of body block
    int decryptBlockSize = Math.min(bodySize, 32);
    byte[] decryptBlock = new byte[decryptBlockSize];
    fis.get(decryptBlock);

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
    if (!setupDecryptTable(pakFileHeader, checksum, table2, table2Offset)) {
      checksum.reset();
      if (!setupDecryptTable(pakFileHeader, checksum, table1, table1Offset))
        throw new PakFileFormatException("Unknown Aion version");
    }
  }

  private boolean setupDecryptTable(PakFileHeader pakFileHeader, CRC32 checksum, byte[] table, Function<Integer, Integer> tableOffset) {
    try {
      ByteBuffer decrypted = unpak(pakFileHeader, table, tableOffset);
      checksum.update(decrypted);
      if (decrypted.capacity() == pakFileHeader.uncompressedSz && Integer.toUnsignedLong(pakFileHeader.crc) == checksum.getValue()) {
        decryptTable = table;
        decryptTableOffset = tableOffset;
        return true;
      }
    } catch (Exception ignored) {
    }
    return false;
  }

  private void write(DataOutputStream outputStream, int offset, int length) throws IOException {
    if (length == 0)
      return;
    synchronized (buffer) {
      int oldLimit = buffer.position(offset).limit();
      buffer.limit(buffer.position() + length);
      Channels.newChannel(outputStream).write(buffer);
      buffer.limit(oldLimit);
    }
  }

  private static class ByteBufferArrayOutputStream extends ByteArrayOutputStream {

    private final ByteBuffer byteBuffer;

    public ByteBufferArrayOutputStream(int size) {
      super(size);
      byteBuffer = ByteBuffer.wrap(buf);
    }

    private ByteBuffer getByteBuffer() {
      return byteBuffer;
    }
  }
}
