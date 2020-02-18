package com.aionemu.geobuilder.xmldecoder.fileformats;

import com.aionemu.geobuilder.xmldecoder.helpers.StreamHelpers;

import java.io.IOException;
import java.io.InputStream;


public class BinaryXmlFile {

	public BinaryXmlNode Root;

	public void Read(InputStream input) throws IOException, Exception {
		if (StreamHelpers.ReadU8(input) != 128)
			throw new Exception("not a binary XML file");
		BinaryXmlStringTable table = new BinaryXmlStringTable();
		table.Read(input);
		this.Root = new BinaryXmlNode();
		this.Root.Read(input, table);
	}
}
