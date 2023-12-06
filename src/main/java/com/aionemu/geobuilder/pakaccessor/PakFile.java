package com.aionemu.geobuilder.pakaccessor;

import com.aionemu.geobuilder.utils.PathSanitizer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import static com.aionemu.geobuilder.pakaccessor.PakConstants.*;

public class PakFile implements AutoCloseable {

  private final FileChannel fileChannel;
  private final MappedByteBuffer buffer;
  private final List<PakBlock> pakBlocks = new ArrayList<>();
  private final Map<String, PakFileHeader> fileHeaders = new LinkedHashMap<>();
  private DecryptTable decryptTable;

  public static PakFile open(Path pakFile) throws IOException {
    return open(pakFile, null);
  }

  public static PakFile open(Path pakFile, Path basePath) throws IOException {
    String prefix = "";
    if (basePath != null) {
      prefix = basePath.relativize(pakFile.getParent()).toString() + '/';
      prefix = PathSanitizer.sanitize(prefix);
    }
    return new PakFile(pakFile, prefix);
  }

  private PakFile(Path file, String pathPrefix) throws IOException {
    fileChannel = FileChannel.open(file);
    buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    init(pathPrefix);
  }

  private void init(String pathPrefix) throws IOException {
    while (true) {
      int blockStartPosition = buffer.position();
      short signature1 = buffer.getShort();
      if (signature1 != PAK_SIGNATURE1 && signature1 != ZIP_SIGNATURE1)
        throw new PakFileFormatException("Unknown Pak signature1: " + signature1);
      short signature2 = buffer.getShort();
      switch (signature2) {
        case PAK_SIGNATURE2_FILE:
        case ZIP_SIGNATURE2_FILE:
          PakFileHeader pakFileHeader = new PakFileHeader();
          pakFileHeader.blockStartPosition = blockStartPosition;
          pakFileHeader.read(buffer);

          pakFileHeader.bodyStartPosition = buffer.position();

          if (pakBlocks.isEmpty() && signature2 != ZIP_SIGNATURE2_FILE) {
            setupDecryptTable(pakFileHeader);
          } else {
            buffer.position(buffer.position() + (int) pakFileHeader.getBodySize());
          }

          String fileName = new String(pakFileHeader.fileNameBytes);
          fileHeaders.put(pathPrefix + PathSanitizer.sanitize(fileName), pakFileHeader);
          pakBlocks.add(pakFileHeader);
          break;
        case PAK_SIGNATURE2_DIR:
        case ZIP_SIGNATURE2_DIR:
          PakCentralDirHeader pakCentralDirHeader = new PakCentralDirHeader();
          pakCentralDirHeader.read(buffer);

          if (pakCentralDirHeader.getBodySize() > 0) {
            pakCentralDirHeader.bodyStartPosition = buffer.position();

            buffer.position(buffer.position() + (int) pakCentralDirHeader.getBodySize());
          } else {
            pakCentralDirHeader.bodyStartPosition = 0;
          }

          pakBlocks.add(pakCentralDirHeader);
          break;
        case PAK_SIGNATURE2_END:
        case ZIP_SIGNATURE2_END:
          PakEndBlockHeader pakEndBlockHeader = new PakEndBlockHeader();
          pakEndBlockHeader.read(buffer);
          pakBlocks.add(pakEndBlockHeader);
          if (buffer.remaining() == 0)
            return;
          System.err.println("Found content after pak end marker, trying to read...");
        default:
          throw new PakFileFormatException("Unknown Pak signature2: " + signature2);
      }
    }
  }

  public void close() throws IOException {
    fileChannel.close();
    buffer.clear();
    pakBlocks.clear();
    fileHeaders.clear();
  }

  public ByteBuffer unpak(String fileName) throws IOException {
    PakFileHeader pakFileHeader = fileHeaders.get(fileName);
    return pakFileHeader == null ? null : unpak(pakFileHeader);
  }

  private ByteBuffer unpak(PakFileHeader pakFileHeader) throws IOException {
    ByteBufferArrayOutputStream out = new ByteBufferArrayOutputStream(pakFileHeader.uncompressedSz);
    DataOutputStream outputStream = new DataOutputStream(pakFileHeader.compMethod == 0 ? out : new InflaterOutputStream(out, new Inflater(true)));
    writeBody(outputStream, pakFileHeader);
    return out.getByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
  }

  public void convertToZip(OutputStream outputStream) throws IOException {
    DataOutputStream zipOutputStream = new DataOutputStream(outputStream);
    for (PakBlock block : pakBlocks) {
      zipOutputStream.writeShort(ZIP_SIGNATURE1);
      if (block instanceof PakFileHeader pakFileHeader) {
        zipOutputStream.writeShort(ZIP_SIGNATURE2_FILE);
        block.write(zipOutputStream);

        writeBody(zipOutputStream, pakFileHeader);
      } else if (block instanceof PakCentralDirHeader) {
        zipOutputStream.writeShort(ZIP_SIGNATURE2_DIR);
        block.write(zipOutputStream);

        write(zipOutputStream, (int) block.bodyStartPosition, (int) block.getBodySize());
      } else if (block instanceof PakEndBlockHeader) {
        zipOutputStream.writeShort(ZIP_SIGNATURE2_END);
        block.write(zipOutputStream);
      } else
        throw new PakFileFormatException("Unsupported block type: " + block.getClass());
    }
  }

  private void writeBody(DataOutputStream os, PakFileHeader pakFileHeader) throws IOException {
    synchronized (buffer) {
      buffer.position((int) pakFileHeader.bodyStartPosition);

      // write extra block if present
      write(os, (int) pakFileHeader.bodyStartPosition, pakFileHeader.extraFieldsSz);

      byte[] data = new byte[pakFileHeader.compressedSz];
      buffer.get(data);
      if (decryptTable != null) {
        decryptTable.decrypt(data);
      }
      os.write(data);
    }
  }

  public Set<String> getFileNames() {
    return fileHeaders.keySet();
  }

  public int getUnpakedFileSize(String fileName) {
    PakFileHeader header = fileHeaders.get(fileName);
    return header == null ? -1 : header.uncompressedSz;
  }

  private void setupDecryptTable(PakFileHeader pakFileHeader) throws PakFileFormatException {
    CRC32 checksum = new CRC32();
    for (DecryptTable table : DecryptTable.values()) {
      this.decryptTable = table;
      try {
        ByteBuffer decrypted = unpak(pakFileHeader);
        checksum.update(decrypted);
        if (decrypted.capacity() == pakFileHeader.uncompressedSz && Integer.toUnsignedLong(pakFileHeader.crc) == checksum.getValue())
          return;
      } catch (IOException ignored) {
      } finally {
        checksum.reset();
      }
    }
    throw new PakFileFormatException("Unsupported pak file encryption algorithm");
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
