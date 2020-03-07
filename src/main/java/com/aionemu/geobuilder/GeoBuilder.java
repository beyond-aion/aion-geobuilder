package com.aionemu.geobuilder;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class GeoBuilder {

  public static void main(String[] args) {
    AionLevelsProcessor processor = new AionLevelsProcessor();
    CmdLineParser parser = new CmdLineParser(processor);
    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println("Usage: GeoBuilder <options>");
      parser.printUsage(System.err);
      return;
    }
    processor.process();
  }
}
