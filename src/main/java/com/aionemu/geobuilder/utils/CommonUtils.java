package com.aionemu.geobuilder.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public final class CommonUtils {

	public final static boolean isLittleEndianByteOrder = (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);
	public final static int BUFFER_MAX_SIZE = 512 * 1024;

	public final static void bufferedCopy(InputStream in, OutputStream out, int size) throws IOException {
		if (size < BUFFER_MAX_SIZE) {
			byte buffer[] = new byte[size];
			if (in.read(buffer) != size)
				throw new IOException("Cannot read bytes from input stream (1)");
			out.write(buffer);
		} else {
			byte buffer[] = new byte[BUFFER_MAX_SIZE];
			int bytesToRead = BUFFER_MAX_SIZE;
			int bytesLeft = size;
			while (bytesLeft > 0) {
				if (in.read(buffer, 0, bytesToRead) != bytesToRead)
					throw new IOException("Cannot read bytes from input stream (2)");
				out.write(buffer, 0, bytesToRead);
				bytesLeft -= bytesToRead;
				bytesToRead = Math.min(bytesLeft, BUFFER_MAX_SIZE);
			}
		}
	}

	public final static void bufferedCopy(InputStream in, OutputStream out) throws IOException {
		byte buffer[] = new byte[BUFFER_MAX_SIZE];
		do {
			int bytesRead = in.read(buffer);
			if (bytesRead == -1)
				break;
			out.write(buffer, 0, bytesRead);
		} while (true);
	}

	public final static byte[] bufferedDup(InputStream in, int size) throws IOException {
		if (size < BUFFER_MAX_SIZE) {
			byte res[] = new byte[size];
			if (in.read(res) != size)
				throw new IOException("Cannot read bytes from input stream (1)");
			return res;
		} else {
			ByteArrayOutputStream res = new ByteArrayOutputStream(size);
			byte buffer[] = new byte[BUFFER_MAX_SIZE];
			int bytesToRead = BUFFER_MAX_SIZE;
			int bytesLeft = size;
			while (bytesLeft > 0) {
				if (in.read(buffer, 0, bytesToRead) != bytesToRead)
					throw new IOException("Cannot read bytes from input stream (2)");
				res.write(buffer, 0, bytesToRead);
				bytesLeft -= bytesToRead;
				bytesToRead = Math.min(bytesLeft, BUFFER_MAX_SIZE);
			}
			return res.toByteArray();
		}
	}

	public final static byte[] decompress(byte[] input) throws DataFormatException, IOException {

		GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(input));

		// Create an expandable byte array to hold the decompressed data
		ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

		// Decompress the data
		int count = 0;
		byte[] buf = new byte[BUFFER_MAX_SIZE];
		while ((count = gzipInputStream.read(buf)) > 0) {
			bos.write(buf, 0, count);
		}

		try {
			gzipInputStream.close();
			bos.close();
		} catch (IOException e) {
		}

		// Get the decompressed data
		return bos.toByteArray();

		// ByteArrayOutputStream dest = new ByteArrayOutputStream();
		// ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(input));
		// int count;
		// byte data[] = new byte[COPY_BLOCK_MAX_SIZE];
		//
		// while (zis.getNextEntry() != null) {
		// while ((count = zis.read(data, 0, COPY_BLOCK_MAX_SIZE)) != -1) {
		// dest.write(data, 0, count);
		// }
		// }
		// dest.flush();
		// data = dest.toByteArray();
		// dest.close();
		// zis.close();
		//
		// return data;

		// Inflater inflator = new Inflater();
		// inflator.setInput(input);
		//
		// ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
		// byte[] buf = new byte[COPY_BLOCK_MAX_SIZE];
		//
		// try {
		// while (true) {
		// int count = inflator.inflate(buf);
		// if (count > 0) {
		// bos.write(buf, 0, count);
		// } else if (count == 0 && inflator.finished()) {
		// break;
		// } else {
		// throw new RuntimeException("bad zip data, size:" + input.length);
		// }
		// }
		// } finally {
		// inflator.end();
		// }
		//
		// return bos.toByteArray();
	}

	public final static List<String> getServerMapIdList(File serverWorldMapsFile) throws IOException, JDOMException {
		SAXBuilder builder = new SAXBuilder();

		// read server maps
		List<String> serverMapIdList = new ArrayList<>();
		Document document = builder.build(serverWorldMapsFile);
		Element rootNode = document.getRootElement();
		List<?> list = rootNode.getChildren("map");

		for (Object aList : list) {
			Element node = (Element) aList;

			serverMapIdList.add(node.getAttributeValue("id"));
		}
		return serverMapIdList;
	}

	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static <T> T[] concatAll(T[] first, T[]... rest) {
		int totalLength = first.length;
		for (T[] array : rest) {
			totalLength += array.length;
		}
		T[] result = Arrays.copyOf(first, totalLength);
		int offset = first.length;
		for (T[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		return result;
	}

}
