package com.aionemu.geobuilder.pakaccessor;

import com.aionemu.geobuilder.utils.DataInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;

public class PakReaderSlim {

  private FileInputStream fileInputStream = null;
  private DataInputStream dataInputStream = null;
  private PakCentralDirEnd centralDirEnd = null;
  private String fileName = "";

  private byte[] res = null;

  public PakReaderSlim(String fileName) {
    try {
      this.fileName = fileName;
      fileInputStream = new FileInputStream(fileName);
      dataInputStream = new DataInputStream(fileInputStream);
      readCentralDirEnd();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public List<PakCentralDirFile> readCentralDir() throws Exception{
    List<PakCentralDirFile> result = new ArrayList<>(centralDirEnd.thisDiskCentralDirCount);
    fileInputStream.getChannel().position(centralDirEnd.centralDirOffset);

    for (int i = 0; i < centralDirEnd.thisDiskCentralDirCount; i++) {
      PakCentralDirFile dirFile = PakCentralDirFile.read(dataInputStream);
      result.add(dirFile);
    }
    return result;
  }

  private void readCentralDirEnd() throws Exception {
    fileInputStream.getChannel().position(fileInputStream.available() - PakCentralDirEnd.HEADER_SIZE );
    centralDirEnd = PakCentralDirEnd.read(dataInputStream);
  }

  public String getOriginalPakPath() {
    return fileName;
  }

  public void close() throws Exception {
    System.out.println("closing");
    if (dataInputStream != null) {
      dataInputStream.close();
      dataInputStream = null;
    }
    if (fileInputStream != null) {
      fileInputStream.close();
      fileInputStream = null;
    }
  }

  public byte[] readFileBytes(PakCentralDirFile dirFile) throws Exception {
    try {
      fileInputStream.getChannel().position((int) dirFile.localHeaderOffset);
      PakFileEntry header = PakFileEntry.read(dataInputStream);
      if (header == null) {
        System.out.println("header null: " + dirFile.fileName);
      }
      if (!dirFile.fileName.equalsIgnoreCase(header.fileName) ||
          dirFile.compressedSize != header.compressedSize ||
          dirFile.uncompressedSize != header.uncompressedSize ||
          dirFile.compressionMethod != header.compressionMethod) {
        throw new Exception("Header mismatch " + dirFile.fileName);
      }
      byte[] result = new byte[(int) dirFile.compressedSize];
      if (dirFile.compressionMethod == 0) {
        dataInputStream.readFully(result);
        if (dirFile.isAionFormat) {
          decodeAionBytes(dirFile, result);
        }
      } else if (dirFile.compressionMethod == 8) {
        if (dirFile.isAionFormat) {
          fileInputStream.read(new byte[32]);
          System.out.println(fileName);
          System.out.println("fs av: " + fileInputStream.available());
          EncryptedAionPakReader enryptedReader = new EncryptedAionPakReader(fileInputStream /*new FileInputStream(fileName)*/, dirFile);
          //InflaterInputStream deflaterInputStream = new InflaterInputStream(enryptedReader);
          System.out.println("zip av:" + enryptedReader.available());
          InflaterInputStream inflaterInputStream = new InflaterInputStream(enryptedReader, new Inflater());
          //ZipInputStream is = new ZipInputStream(enryptedReader);
          DataInputStream st = new DataInputStream(inflaterInputStream);
          st.readFully(result);



          // stream2 = new DataInputStream(deflaterInputStream);
                   // DataInputStream stream2 = new DataInputStream(zipInputStream);
                   // stream2.readFully(result);
          //stream2.readFully(result);
          //int i = zipInputStream.read(result);
                   //System.out.println("read zip amount: " + i + " should: " + result.length);
        } else {
          ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
          int i = zipInputStream.read(result);
          System.out.println("read normal amount: " +i);
        }
      } else {
        throw new Exception("unsupported compression method: " + dirFile.compressionMethod);
      }

      return result;
    } catch (Exception e) {
      System.out.println(fileName);
      e.printStackTrace();
    }
    return null;
  }

  private void decodeAionBytes(PakCentralDirFile dirFile, byte[] bytesToModify) {
    int tbloff = (int) dirFile.compressedSize & 0x3FF;
    for (int i = 0; i < bytesToModify.length && i < 32; i++) {
      bytesToModify[i] ^= PakConstants.table2[tbloff + i];
    }
  }
}
