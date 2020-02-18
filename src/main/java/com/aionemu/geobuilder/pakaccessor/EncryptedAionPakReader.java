package com.aionemu.geobuilder.pakaccessor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class EncryptedAionPakReader extends InputStream {

  FileInputStream underlying;
  long startPos = 0;
  long curPos = 0;
  PakCentralDirFile dirFile;

  public EncryptedAionPakReader(FileInputStream stream, PakCentralDirFile dirFile) {
    underlying = stream;
    try {
      startPos = stream.getChannel().position();
      System.out.println("startpos: " + startPos);
      this.dirFile = dirFile;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public int available() throws IOException {
    return underlying.available();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    System.out.println("read 3");
    int bytesRead = 0;
    /*if (curPos < 32) {
      int tbloff = (int) dirFile.compressedSize & 0x3FF;
      while (curPos < 32 && bytesRead < len) {
        int c = underlying.read();
        if (c == -1) {
          System.out.println("nooo");
          break;
        }
        byte a = (byte) c;
        a ^= PakConstants.table2[tbloff + (int)curPos];
        b[off + bytesRead] = a;
        curPos++;
        System.out.println(a);
        bytesRead++;
      }
      return bytesRead;
    }
    */
    bytesRead = underlying.read(b, off, len);
    curPos += bytesRead;
    return bytesRead;
  }

  @Override
  public int read(byte[] b) throws IOException {
    System.out.println("read2");
    return super.read(b);
  }

  @Override
  public int read() throws IOException {
    System.out.println("read");
    return 0;
  }
}
