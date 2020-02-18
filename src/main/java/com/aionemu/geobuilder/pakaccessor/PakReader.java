package com.aionemu.geobuilder.pakaccessor;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class PakReader {

  public List<File> files2 = new ArrayList<>();
  public LinkedHashMap<String, PakCentralDirFile> files = new LinkedHashMap<>();

  private PakReaderSlim pakReaderSlim;
  public String fileName = "";

  public PakReader(String fileName) {
    this.fileName = fileName;
    pakReaderSlim = new PakReaderSlim(fileName);
    loadFiles();
  }

  private void loadFiles() {
    LinkedHashMap<String, PakCentralDirFile> result = new LinkedHashMap<>();
    try {
      for (PakCentralDirFile centralDirFile : pakReaderSlim.readCentralDir()) {
        result.put(centralDirFile.fileName.replace('/', '\\'), centralDirFile);
      }
      files = result;
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public String getOriginalPakPath() {
    return pakReaderSlim.getOriginalPakPath();
  }

  public byte[] getFile(String fileName) throws Exception {
    return pakReaderSlim.readFileBytes(files.get(fileName.toLowerCase().replace('/', '\\')));
  }
}
