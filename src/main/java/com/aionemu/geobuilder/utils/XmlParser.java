package com.aionemu.geobuilder.utils;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Parent;
import org.jdom2.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class XmlParser {

  private XmlParser() {
  }

  public static Document parse(ByteBuffer buffer) throws IOException {
    int position = buffer.position();
    if (buffer.get() == (byte) 128) { // binary xml marker
      return parseBinaryXml(buffer);
    }
    int offset = position + buffer.arrayOffset();
    int length = buffer.limit() - position;
    try {
      return new SAXBuilder().build(new ByteArrayInputStream(buffer.array(), offset, length));
    } catch (JDOMException e) {
      throw new IOException(e);
    }
  }

  private static Document parseBinaryXml(ByteBuffer buffer) {
    byte[] stringTable = new byte[readPackedInt(buffer)];
    buffer.get(stringTable);

    Document doc = new Document();
    readElement(doc, buffer, stringTable);
    return doc;
  }

  private static void readElement(Parent parent, ByteBuffer buffer, byte[] stringTable) {
    Element child = new Element(nextString(buffer, stringTable));
    parent.addContent(child);
    byte types = buffer.get();
    if ((types & 1) == 1)
      child.setText(nextString(buffer, stringTable));
    if ((types & 2) == 2) {
      int attributeCount = readPackedInt(buffer);
      for (int index = 0; index < attributeCount; ++index)
        child.setAttribute(nextString(buffer, stringTable), nextString(buffer, stringTable));
    }
    if ((types & 4) != 4)
      return;
    int childElements = readPackedInt(buffer);
    for (int index = 0; index < childElements; ++index) {
      readElement(child, buffer, stringTable);
    }
  }

  public static int readPackedInt(ByteBuffer buffer) {
    int num1 = buffer.get() & 0xFF;
    int num2 = 0;
    int num3 = 0;
    for (; num1 >= 128; num1 = buffer.get() & 0xFF) {
      num2 |= (num1 & Byte.MAX_VALUE) << num3;
      num3 += 7;
    }
    return num2 | num1 << num3;
  }

  private static String nextString(ByteBuffer buffer, byte[] stringTable) {
    int index = readPackedInt(buffer);
    return index == 0 ? "" : readUTF16Z(stringTable, index * 2);
  }

  public static String readUTF16Z(byte[] data, int startIndex) {
    int endIndex = startIndex;
    while (endIndex < data.length && (data[endIndex] != 0 || data[endIndex + 1] != 0))
      endIndex += 2;
    if (endIndex == startIndex)
      return "";
    return new String(data, startIndex, endIndex - startIndex, StandardCharsets.UTF_16LE);
  }
}
