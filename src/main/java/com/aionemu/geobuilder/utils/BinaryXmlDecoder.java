package com.aionemu.geobuilder.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import com.aionemu.geobuilder.xmldecoder.fileformats.BinaryXmlFile;
import com.aionemu.geobuilder.xmldecoder.fileformats.BinaryXmlNode;


/*
 * Code was converted from Aion Exporter
 * 
 * XML generation: http://www.genedavis.com/library/xml/java_dom_xml_creation.jsp
 */

public final class BinaryXmlDecoder {

	private static void WriteNode(Document doc, Node parent, BinaryXmlNode node) {
		Element child = doc.createElement(node.Name);
		parent.appendChild(child);

		for (Entry<String, String> keyValuePair : node.Attributes.entrySet())
			child.setAttribute(keyValuePair.getKey(), keyValuePair.getValue());

		for (BinaryXmlNode node1 : node.Children)
			WriteNode(doc, child, node1);

		if (node.Value != null)
			child.setTextContent(node.Value);
	}

	public static void Decode(InputStream input, OutputStream output) {
		BinaryXmlFile binaryXmlFile = new BinaryXmlFile();
		try {
			binaryXmlFile.Read(input);

			// Creating an empty XML Document
			DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
			Document doc = docBuilder.newDocument();

			WriteNode(doc, doc, binaryXmlFile.Root);

			// Output the XML

			// set up a transformer
			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer trans = transfac.newTransformer();
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty(OutputKeys.ENCODING, "Unicode");

			// write XML tree to the stream
			StreamResult result = new StreamResult(output);
			DOMSource source = new DOMSource(doc);
			trans.transform(source, result);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
