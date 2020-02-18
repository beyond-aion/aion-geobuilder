package com.aionemu.geobuilder;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class GeoBuilder {

  public static void main(final String[] args) {
    final AionLevelsProcessor processor = new AionLevelsProcessor();
    final CmdLineParser parser = new CmdLineParser(processor);
    try {
      parser.parseArgument(args);
    } catch (final CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println("Usage: GeoBuilder <options>");
      parser.printUsage(System.err);
      return;
    }

    try {
      processor.process();
    } catch (final Exception e) {
      System.err.println(e.toString());
      e.printStackTrace();
    }
  }
}
