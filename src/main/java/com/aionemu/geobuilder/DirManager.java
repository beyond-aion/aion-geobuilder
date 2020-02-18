package com.aionemu.geobuilder;

import com.aionemu.geobuilder.pakaccessor.PakReader;
import com.aionemu.geobuilder.utils.CommonUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DirManager {

  private String root;
  private LinkedHashMap<String, PakReader> files = new LinkedHashMap<>();

  public DirManager(String root) {
    setRootPath(root);
  }

  public DirManager(String root, File... subDirs) {
    setRootPath(root);
    for (File dir : subDirs) {
      loadSubDir(dir);
    }
  }

  private void setRootPath(String root) {
    if (!Files.isDirectory(Paths.get(root))) {
      try {
        throw new FileNotFoundException();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    } else {
      this.root = root;
    }
  }

  private void loadSubDir(File subDir) {
    File[] paksToLoad = collectMeshesPath(subDir);
    System.out.println("paks to load size: " + paksToLoad.length);
    for (File pakToLoad : paksToLoad) {
      PakReader pakReader = new PakReader(pakToLoad.getPath());
      for (String file : pakReader.files.keySet()) {

        String relativePath = getRelativePath(Paths.get(pakToLoad.getPath()).getParent() + "\\" + file);
        if (files.getOrDefault(relativePath, null) == null) {
          files.put(relativePath, pakReader);
        }
      }
    }
  }

  private File[] collectMeshesPath(final File... rootFolders) {
    File[] res = new File[0];
    for (final File file : rootFolders) {
      // collect meshes
      final File[] meshFiles = file.listFiles((dir, name) -> name.matches("Mesh_Meshes_\\d\\d\\d\\.pak") || name.endsWith("_Meshes.pak"));
      if (meshFiles.length > 0) {
        res = CommonUtils.concat(res, meshFiles);
      }

      // process subfolders recursively
      final File[] subFolders = file.listFiles(File::isDirectory);
      if (subFolders.length > 0) {
        res = CommonUtils.concat(res, collectMeshesPath(subFolders));
      }
    }
    return res;
  }

  public InputStream openFile(String relativePath) {
    String path = normalizePath(relativePath);
    PakReader pakReader = files.get(path);
    if (pakReader == null) {
      // localFile
      System.out.println("FILE IS LOCAL: " + relativePath);
    } else {
      try {
        return new ByteArrayInputStream(pakReader.getFile(makePathRelativeToPak(path, pakReader)));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private String makePathRelativeToPak(String relativePath, PakReader pakReader) {
    String relativeToPak = Paths.get(pakReader.getOriginalPakPath()).getParent().toString().substring(root.length());
    return relativePath.substring(relativeToPak.length());
  }

  private String getRelativePath(String path) {
    path = normalizePath(path);
    return path.substring(root.length() + 1); // +1 to remove last \
  }

  private String normalizePath(String path) {
    return path.toLowerCase().replace('/', '\\');
  }

  public boolean exists(String path) {
    return files.containsKey(normalizePath(path));
  }


}
