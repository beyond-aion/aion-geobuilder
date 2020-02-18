package com.aionemu.geobuilder.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

	public static final void zipDirectory(File directory, File zip) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
		zip(directory, directory, zos);
		zos.close();
	}

	private static final void zip(File directory, File base, ZipOutputStream zos) throws IOException {
		File[] files = directory.listFiles();
		byte[] buffer = new byte[8192];
		int read = 0;
		for (File file : files) {
			if (file.isDirectory()) {
				zip(file, base, zos);
			} else {
				FileInputStream in = new FileInputStream(file);
				ZipEntry entry = new ZipEntry(file.getPath().substring(base.getPath().length() + 1));
				zos.putNextEntry(entry);
				while (-1 != (read = in.read(buffer))) {
					zos.write(buffer, 0, read);
				}
				in.close();
			}
		}
	}

	public static final void unzip(File zipFile, File extractTo) throws IOException {
		ZipFile archive = new ZipFile(zipFile);
		Enumeration<? extends ZipEntry> e = archive.entries();
		while (e.hasMoreElements()) {
			ZipEntry entry = e.nextElement();
			File file = new File(extractTo, entry.getName());

			if (entry.isDirectory() && !file.exists())
				file.mkdirs();
			else {
				if (!file.getParentFile().exists())
					file.getParentFile().mkdirs();

				InputStream in = archive.getInputStream(entry);
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));

				CommonUtils.bufferedCopy(in, out);

				in.close();
				out.close();
			}
		}
		archive.close();
	}

	public static final void unzipEntry(File zipFile, String filter, OutputStream outputStream) throws IOException {
		ZipFile archive = new ZipFile(zipFile);
		Enumeration<? extends ZipEntry> e = archive.entries();
		while (e.hasMoreElements()) {
			ZipEntry entry = e.nextElement();
			if (entry.isDirectory()) // search only for file
				continue;

			String entryName = entry.getName();
			if (!entryName.matches(filter))
				continue;

			InputStream in = archive.getInputStream(entry);
			BufferedOutputStream out = new BufferedOutputStream(outputStream);

			CommonUtils.bufferedCopy(in, out);

			in.close();
			out.close();

			archive.close();
			return;
		}
		archive.close();
		throw new FileNotFoundException("There is no file in zip archive that match mask: " + filter);
	}

	public static final void unzipEntry(File zipFile, Map<String, OutputStream> filterStreamMap, IStringComparer comparer, boolean toLowerCase, Charset charset) throws IOException {
		ZipFile archive = new ZipFile(zipFile);
		if (charset != null) {
			archive = new ZipFile(zipFile, charset);
		}
		Enumeration<? extends ZipEntry> e = archive.entries();
		while (e.hasMoreElements()) {
			ZipEntry entry = e.nextElement();
			if (entry.isDirectory()) // search only for file
				continue;

			String entryName = entry.getName();
			if (toLowerCase) {
				entryName = entryName.toLowerCase();
			}
			String filter = null;
			OutputStream outputStream = null;
			for (Entry<String, OutputStream> filterStream : filterStreamMap.entrySet()) {
				if (!entryName.contains("levels/") && comparer.compare(entryName, filterStream.getKey())) { // sometimes there's another leveldata.xml file in levels/levelname/ which is empty
					filter = filterStream.getKey();
					outputStream = filterStream.getValue();
					break;
				}
			}
			if (filter == null)
				continue;

			filterStreamMap.remove(filter);

			InputStream in = archive.getInputStream(entry);
			BufferedOutputStream out = new BufferedOutputStream(outputStream);

			CommonUtils.bufferedCopy(in, out);

			out.close();
			in.close();

			if (filterStreamMap.size() == 0)
				break;
		}
		archive.close();
		if (filterStreamMap.size() != 0) {
			if (filterStreamMap.remove("brush.lst") != null) {
				System.out.println(". . [INFO] Level does not contain brush.lst");
			}
			if (filterStreamMap.remove("objects.lst") != null) {
				System.out.println(". . [INFO] Level does not contain objects.lst");
			}
			if (filterStreamMap.remove("mission_mission0.xml") != null) {
				System.out.println(". . [INFO] Level does not contain mission_mission0.xml");
			}
			if (filterStreamMap.size() != 0) {
				throw new FileNotFoundException("There are no files in zip archive that match masks: " + filterStreamMap.keySet());
			}
		}

	}
}
