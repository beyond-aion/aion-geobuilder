package com.aionemu.geobuilder.utils;

public class PathSanitizer {

  public static String sanitize(String path) {
    path = path.trim();
    path = path.toLowerCase();
    path = path.replace('\\', '/');
    if (path.endsWith(".")) // two CGF files are referenced wrongly but loaded correctly by the client
      path = path.substring(0, path.length() - 1);
    return path;
  }
}
