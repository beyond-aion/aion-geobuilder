package com.aionemu.geobuilder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.DefaultConsole;

public class GeoBuilder {

  public static void main(String[] args) {
    AionLevelsProcessor processor = new AionLevelsProcessor();
    try {
      JCommander.newBuilder().addObject(processor).console(new DefaultConsole(System.err)).args(args).build().parse();
    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      e.usage();
      return;
    }
    processor.process();
  }
}
