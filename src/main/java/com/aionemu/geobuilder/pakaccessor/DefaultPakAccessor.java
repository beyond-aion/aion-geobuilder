package com.aionemu.geobuilder.pakaccessor;

//import java.io.DataInputStream;
//import java.io.DataOutputStream;
import com.aionemu.geobuilder.exceptions.AionException;
import com.aionemu.geobuilder.exceptions.PakFileFormatException;
import com.aionemu.geobuilder.utils.CommonUtils;
import com.aionemu.geobuilder.utils.DataInputStream;
import com.aionemu.geobuilder.utils.DataOutputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.CRC32;



/**
 * @author Haron
 */
public class DefaultPakAccessor {

	private final short PAK_SIGNATURE1 = (short) 0xB4AF;
	private final short PAK_SIGNATURE2_FILE = (short) 0xFBFC;
	private final short PAK_SIGNATURE2_DIR = (short) 0xFDFE;
	private final short PAK_SIGNATURE2_END = (short) 0xF9FA;

	private final short ZIP_SIGNATURE1 = (short) 0x4B50;
	private final short ZIP_SIGNATURE2_FILE = (short) 0x0403;
	private final short ZIP_SIGNATURE2_DIR = (short) 0x0201;
	private final short ZIP_SIGNATURE2_END = (short) 0x0605;

	private FileInputStream pakFileStream = null;

	private abstract class PakBlock {

		long bodyStartPosition;

		abstract long getBodySize();

		abstract void read(DataInputStream stream) throws IOException;

		abstract void write(DataOutputStream stream) throws IOException;
	}

	private class PakFileHeader extends PakBlock {

		byte extractVersion;
		byte extractSystem;
		short flags;
		short compMethod;
		short lastModTime;
		short lastModDate;
		int crc;
		int compressedSz;
		int uncompressedSz;
		short fileNameSz;
		short extraFieldsSz;

		byte fileNameBytes[];

		long blockStartPosition;
		int version;

		public int getSize() {
			return 4 + 26 + fileNameSz + extraFieldsSz + compressedSz;
		}

		public int getBodyShift() {
			return 4 + 26 + fileNameSz + extraFieldsSz;
		}

		
		public void read(final DataInputStream stream) throws IOException {
			extractVersion = stream.readByte();
			extractSystem = stream.readByte();
			flags = stream.readShort();
			compMethod = stream.readShort();
			lastModTime = stream.readShort();
			lastModDate = stream.readShort();
			crc = stream.readInt();
			compressedSz = stream.readInt();
			uncompressedSz = stream.readInt();
			fileNameSz = stream.readShort();
			extraFieldsSz = stream.readShort();

			fileNameBytes = new byte[fileNameSz];
			if (stream.read(fileNameBytes) != fileNameSz)
				throw new IOException("Cannot read file name from File block");
		}

		
		public void write(final DataOutputStream stream) throws IOException {
			stream.writeByte(extractVersion);
			stream.writeByte(extractSystem);
			stream.writeShort(flags);
			stream.writeShort(compMethod);
			stream.writeShort(lastModTime);
			stream.writeShort(lastModDate);
			stream.writeInt(crc);
			stream.writeInt(compressedSz);
			stream.writeInt(uncompressedSz);
			stream.writeShort(fileNameSz);
			stream.writeShort(extraFieldsSz);

			stream.write(fileNameBytes);
		}

		
		long getBodySize() {
			return extraFieldsSz + compressedSz;
		}
	}

	private class PakCentralDirHeader extends PakBlock {

		// byte stub[] = new byte[42];
		byte createVersion;
		byte createSystem;
		byte extractVersion;
		byte extractSystem;
		short flags;
		short compType;
		short time;
		short date;
		int crc;
		int compressedSz;
		int uncompressedSz;
		short fileNameSz;
		short extraFieldsSz;
		short commentSz;
		short diskNumStart;
		short intFileAttr;
		int extFileAttr;
		int localHeaderOffset;

		byte fileNameBytes[];

		
		public void read(final DataInputStream stream) throws IOException {
			// stream.readFully(stub);
			createVersion = stream.readByte();
			createSystem = stream.readByte();
			extractVersion = stream.readByte();
			extractSystem = stream.readByte();
			flags = stream.readShort();
			compType = stream.readShort();
			time = stream.readShort();
			date = stream.readShort();
			crc = stream.readInt();
			compressedSz = stream.readInt();
			uncompressedSz = stream.readInt();
			fileNameSz = stream.readShort();
			extraFieldsSz = stream.readShort();
			commentSz = stream.readShort();
			diskNumStart = stream.readShort();
			intFileAttr = stream.readShort();
			extFileAttr = stream.readInt();
			localHeaderOffset = stream.readInt();

			fileNameBytes = new byte[fileNameSz];
			if (stream.read(fileNameBytes) != fileNameSz)
				throw new IOException("Cannot read file name from Dir block");
		}

		
		public void write(final DataOutputStream stream) throws IOException {
			// stream.write(stub);
			stream.writeByte(createVersion);
			stream.writeByte(createSystem);
			stream.writeByte(extractVersion);
			stream.writeByte(extractSystem);
			stream.writeShort(flags);
			stream.writeShort(compType);
			stream.writeShort(time);
			stream.writeShort(date);
			stream.writeInt(crc);
			stream.writeInt(compressedSz);
			stream.writeInt(uncompressedSz);
			stream.writeShort(fileNameSz);
			stream.writeShort(extraFieldsSz);
			stream.writeShort(commentSz);
			stream.writeShort(diskNumStart);
			stream.writeShort(intFileAttr);
			stream.writeInt(extFileAttr);
			stream.writeInt(localHeaderOffset);

			stream.write(fileNameBytes);
		}

		
		long getBodySize() {
			return extraFieldsSz + commentSz;
		}
	}

	private class PakEndBlockHeader extends PakBlock {

		byte stub[] = new byte[18];

		// short data1[] = new short[4];
		// int data2[] = new int[2];
		// short data3;

		
		public void read(final DataInputStream stream) throws IOException {
			stream.readFully(stub);
			// data1[0] = stream.readShort();
			// data1[1] = stream.readShort();
			// data1[2] = stream.readShort();
			// data1[3] = stream.readShort();
			// data2[0] = stream.readInt();
			// data2[1] = stream.readInt();
			// data3 = stream.readShort();
		}

		
		public void write(final DataOutputStream stream) throws IOException {
			stream.write(stub);
			// stream.writeShort(data1[0]);
			// stream.writeShort(data1[1]);
			// stream.writeShort(data1[2]);
			// stream.writeShort(data1[3]);
			// stream.writeInt(data2[0]);
			// stream.writeInt(data2[1]);
			// stream.writeShort(data3);
		}

		
		long getBodySize() {
			return 0;
		}
	}

	// open beta
	private final static byte[] table1 = { (byte) 0x2f, (byte) 0x5d, (byte) 0x51, (byte) 0xf7, (byte) 0x01, (byte) 0xe9, (byte) 0xb4, (byte) 0x93,
		(byte) 0x4e, (byte) 0x51, (byte) 0x81, (byte) 0x3e, (byte) 0xaf, (byte) 0x3f, (byte) 0xdf, (byte) 0x99, (byte) 0x80, (byte) 0x5e, (byte) 0x13,
		(byte) 0x83, (byte) 0x9b, (byte) 0x46, (byte) 0x57, (byte) 0xb5, (byte) 0x1b, (byte) 0x5c, (byte) 0xec, (byte) 0xb1, (byte) 0x29, (byte) 0x7c,
		(byte) 0xa9, (byte) 0x31, (byte) 0x68, (byte) 0xe5, (byte) 0xda, (byte) 0xa7, (byte) 0xf6, (byte) 0x4f, (byte) 0xae, (byte) 0x16, (byte) 0x9a,
		(byte) 0x7f, (byte) 0x03, (byte) 0xcf, (byte) 0x1d, (byte) 0x5e, (byte) 0xd0, (byte) 0x51, (byte) 0x5a, (byte) 0xe5, (byte) 0x02, (byte) 0xd9,
		(byte) 0x11, (byte) 0xd0, (byte) 0xfb, (byte) 0xf4, (byte) 0xf8, (byte) 0x7c, (byte) 0xa2, (byte) 0x88, (byte) 0x26, (byte) 0xd8, (byte) 0x1f,
		(byte) 0xa2, (byte) 0x43, (byte) 0xda, (byte) 0x33, (byte) 0xa9, (byte) 0xac, (byte) 0x4e, (byte) 0x5a, (byte) 0x0d, (byte) 0xed, (byte) 0x78,
		(byte) 0x86, (byte) 0x2d, (byte) 0xb2, (byte) 0x6a, (byte) 0xc4, (byte) 0x9b, (byte) 0xaa, (byte) 0x77, (byte) 0x85, (byte) 0x57, (byte) 0x6a,
		(byte) 0xa6, (byte) 0xd8, (byte) 0x35, (byte) 0xd8, (byte) 0x97, (byte) 0x6b, (byte) 0x17, (byte) 0x24, (byte) 0xb7, (byte) 0x7a, (byte) 0x1d,
		(byte) 0xd3, (byte) 0x3b, (byte) 0x9e, (byte) 0x79, (byte) 0xf2, (byte) 0xae, (byte) 0x9f, (byte) 0x01, (byte) 0xe6, (byte) 0x9d, (byte) 0x29,
		(byte) 0x40, (byte) 0xed, (byte) 0x2f, (byte) 0x9c, (byte) 0x16, (byte) 0xda, (byte) 0x18, (byte) 0xd1, (byte) 0x99, (byte) 0x0e, (byte) 0xd4,
		(byte) 0x0a, (byte) 0x63, (byte) 0x2d, (byte) 0x92, (byte) 0xd7, (byte) 0xeb, (byte) 0xb4, (byte) 0xa7, (byte) 0x50, (byte) 0x21, (byte) 0xd8,
		(byte) 0x0f, (byte) 0x45, (byte) 0xd6, (byte) 0xc6, (byte) 0xbf, (byte) 0xcc, (byte) 0x47, (byte) 0xcc, (byte) 0x59, (byte) 0xed, (byte) 0x3e,
		(byte) 0x71, (byte) 0xfe, (byte) 0xa0, (byte) 0x26, (byte) 0xfc, (byte) 0xd1, (byte) 0x07, (byte) 0x85, (byte) 0x8a, (byte) 0xee, (byte) 0x12,
		(byte) 0x36, (byte) 0x11, (byte) 0x5a, (byte) 0x60, (byte) 0xe1, (byte) 0x8f, (byte) 0xbd, (byte) 0x9e, (byte) 0xf7, (byte) 0xb6, (byte) 0x64,
		(byte) 0x39, (byte) 0xcd, (byte) 0x49, (byte) 0x5a, (byte) 0x9a, (byte) 0xf7, (byte) 0x90, (byte) 0x1c, (byte) 0xc1, (byte) 0xa2, (byte) 0x0b,
		(byte) 0xb3, (byte) 0x81, (byte) 0xf7, (byte) 0xca, (byte) 0xb8, (byte) 0x2a, (byte) 0x4b, (byte) 0x95, (byte) 0x13, (byte) 0xdc, (byte) 0x2e,
		(byte) 0x4a, (byte) 0xe5, (byte) 0x64, (byte) 0x16, (byte) 0x94, (byte) 0x99, (byte) 0xc9, (byte) 0xb1, (byte) 0x7b, (byte) 0x53, (byte) 0x76,
		(byte) 0xae, (byte) 0xc4, (byte) 0xdf, (byte) 0x26, (byte) 0xf7, (byte) 0xc8, (byte) 0x5f, (byte) 0x78, (byte) 0x31, (byte) 0xae, (byte) 0xaf,
		(byte) 0x5a, (byte) 0x7f, (byte) 0xa4, (byte) 0xe7, (byte) 0x29, (byte) 0x5e, (byte) 0x0e, (byte) 0xe2, (byte) 0xbb, (byte) 0x91, (byte) 0x41,
		(byte) 0x32, (byte) 0x2c, (byte) 0xf0, (byte) 0xce, (byte) 0x60, (byte) 0x9e, (byte) 0x27, (byte) 0xdc, (byte) 0xfa, (byte) 0xdc, (byte) 0x13,
		(byte) 0xac, (byte) 0x37, (byte) 0xf7, (byte) 0xf1, (byte) 0xb4, (byte) 0xa4, (byte) 0xcd, (byte) 0xf4, (byte) 0x7a, (byte) 0xdc, (byte) 0xa9,
		(byte) 0x7b, (byte) 0x95, (byte) 0x82, (byte) 0xda, (byte) 0x7d, (byte) 0xfb, (byte) 0x8d, (byte) 0x6b, (byte) 0x6e, (byte) 0x0c, (byte) 0x43,
		(byte) 0xe7, (byte) 0x23, (byte) 0x6c, (byte) 0xc0, (byte) 0x53, (byte) 0xf9, (byte) 0x39, (byte) 0x82, (byte) 0x38, (byte) 0xde, (byte) 0x9b,
		(byte) 0xd0, (byte) 0xfe, (byte) 0x57, (byte) 0x3d, (byte) 0x75, (byte) 0x65, (byte) 0x43, (byte) 0xb0, (byte) 0xae, (byte) 0x5a, (byte) 0x6e,
		(byte) 0x4e, (byte) 0xb3, (byte) 0xfb, (byte) 0xae, (byte) 0x8c, (byte) 0xc4, (byte) 0x0f, (byte) 0x9b, (byte) 0x65, (byte) 0x27, (byte) 0xaf,
		(byte) 0xa2, (byte) 0xc6, (byte) 0xf1, (byte) 0x84, (byte) 0x91, (byte) 0x94, (byte) 0x1a, (byte) 0x39, (byte) 0x39, (byte) 0x53, (byte) 0xa5,
		(byte) 0x90, (byte) 0x64, (byte) 0xf0, (byte) 0x62, (byte) 0xcc, (byte) 0xb5, (byte) 0xbf, (byte) 0x1e, (byte) 0xbc, (byte) 0xa7, (byte) 0x28,
		(byte) 0xae, (byte) 0x33, (byte) 0x3f, (byte) 0x16, (byte) 0xc6, (byte) 0x30, (byte) 0xb7, (byte) 0xb1, (byte) 0xf2, (byte) 0x83, (byte) 0xb1,
		(byte) 0x5e, (byte) 0xb0, (byte) 0x37, (byte) 0x20, (byte) 0x9d, (byte) 0xf7, (byte) 0x7b, (byte) 0x95, (byte) 0xbe, (byte) 0x35, (byte) 0x6e,
		(byte) 0x1b, (byte) 0x07, (byte) 0x05, (byte) 0x77, (byte) 0x32, (byte) 0x3a, (byte) 0xae, (byte) 0x8a, (byte) 0x39, (byte) 0x25, (byte) 0xaf,
		(byte) 0x10, (byte) 0xc5, (byte) 0x18, (byte) 0x56, (byte) 0xc2, (byte) 0x2b, (byte) 0xf9, (byte) 0xc4, (byte) 0x4b, (byte) 0xd6, (byte) 0xdc,
		(byte) 0x44, (byte) 0xd7, (byte) 0x9d, (byte) 0xa8, (byte) 0x5c, (byte) 0x7f, (byte) 0xad, (byte) 0xef, (byte) 0x88, (byte) 0xbc, (byte) 0x46,
		(byte) 0x5f, (byte) 0xfe, (byte) 0xc0, (byte) 0xe3, (byte) 0xde, (byte) 0x69, (byte) 0xe3, (byte) 0x03, (byte) 0xed, (byte) 0xf8, (byte) 0x06,
		(byte) 0x1f, (byte) 0x38, (byte) 0xc1, (byte) 0x22, (byte) 0x23, (byte) 0xf4, (byte) 0xc1, (byte) 0xd7, (byte) 0xe1, (byte) 0x11, (byte) 0x7b,
		(byte) 0x3c, (byte) 0xcb, (byte) 0xb4, (byte) 0x8d, (byte) 0xaf, (byte) 0x82, (byte) 0x23, (byte) 0x30, (byte) 0x0d, (byte) 0x78, (byte) 0x82,
		(byte) 0xf9, (byte) 0xed, (byte) 0x3e, (byte) 0x91, (byte) 0xe1, (byte) 0x52, (byte) 0xa7, (byte) 0xd5, (byte) 0xd5, (byte) 0x75, (byte) 0x71,
		(byte) 0x46, (byte) 0xda, (byte) 0x11, (byte) 0x97, (byte) 0xfb, (byte) 0x16, (byte) 0xdf, (byte) 0xea, (byte) 0xf3, (byte) 0xab, (byte) 0xa0,
		(byte) 0x32, (byte) 0x66, (byte) 0xdb, (byte) 0x5e, (byte) 0x5e, (byte) 0xb9, (byte) 0x43, (byte) 0x55, (byte) 0x0e, (byte) 0x9e, (byte) 0xa5,
		(byte) 0x2a, (byte) 0xfd, (byte) 0x5e, (byte) 0x31, (byte) 0xc6, (byte) 0x93, (byte) 0xd4, (byte) 0x9a, (byte) 0xa2, (byte) 0x2b, (byte) 0x37,
		(byte) 0x00, (byte) 0xb9, (byte) 0x46, (byte) 0x13, (byte) 0xf7, (byte) 0x05, (byte) 0x51, (byte) 0xa7, (byte) 0xb2, (byte) 0xaa, (byte) 0x22,
		(byte) 0x0c, (byte) 0x9d, (byte) 0xc5, (byte) 0xd2, (byte) 0x3d, (byte) 0x62, (byte) 0xf4, (byte) 0x28, (byte) 0x8c, (byte) 0xbc, (byte) 0x89,
		(byte) 0x25, (byte) 0x79, (byte) 0xfa, (byte) 0x9a, (byte) 0xfd, (byte) 0x8d, (byte) 0xa1, (byte) 0xbc, (byte) 0x02, (byte) 0x2b, (byte) 0x15,
		(byte) 0xb0, (byte) 0xb6, (byte) 0xe6, (byte) 0xa4, (byte) 0xcd, (byte) 0xbc, (byte) 0x72, (byte) 0xf8, (byte) 0x68, (byte) 0xb4, (byte) 0x9a,
		(byte) 0x33, (byte) 0x08, (byte) 0xba, (byte) 0x62, (byte) 0xb7, (byte) 0xb1, (byte) 0xb1, (byte) 0xca, (byte) 0x00, (byte) 0x08, (byte) 0x01,
		(byte) 0x40, (byte) 0x68, (byte) 0x8e, (byte) 0xe1, (byte) 0x49, (byte) 0x4f, (byte) 0xd8, (byte) 0xf2, (byte) 0x67, (byte) 0x85, (byte) 0xf0,
		(byte) 0x37, (byte) 0xc9, (byte) 0x61, (byte) 0xab, (byte) 0x1e, (byte) 0xc6, (byte) 0x6a, (byte) 0x4d, (byte) 0xca, (byte) 0xaf, (byte) 0x03,
		(byte) 0x2f, (byte) 0x36, (byte) 0x02, (byte) 0xf0, (byte) 0xbc, (byte) 0x5e, (byte) 0x81, (byte) 0x39, (byte) 0x8a, (byte) 0x25, (byte) 0x38,
		(byte) 0x2c, (byte) 0xca, (byte) 0x04, (byte) 0xf9, (byte) 0x0d, (byte) 0xf6, (byte) 0x44, (byte) 0x5b, (byte) 0x46, (byte) 0xdb, (byte) 0xde,
		(byte) 0xb7, (byte) 0x7b, (byte) 0xf4, (byte) 0xac, (byte) 0x3b, (byte) 0x7f, (byte) 0x36, (byte) 0x0d, (byte) 0x90, (byte) 0x7c, (byte) 0x2c,
		(byte) 0xb0, (byte) 0x20, (byte) 0x48, (byte) 0xab, (byte) 0xa9, (byte) 0x7f, (byte) 0x39, (byte) 0xdb, (byte) 0x6d, (byte) 0x0b, (byte) 0x80,
		(byte) 0xe2, (byte) 0xf1, (byte) 0x37, (byte) 0x50, (byte) 0xfa, (byte) 0x83, (byte) 0x9d, (byte) 0xd3, (byte) 0x3e, (byte) 0x8c, (byte) 0x54,
		(byte) 0x48, (byte) 0xeb, (byte) 0xe7, (byte) 0x92, (byte) 0x34, (byte) 0x6a, (byte) 0xeb, (byte) 0x2b, (byte) 0x18, (byte) 0xda, (byte) 0xda,
		(byte) 0xe5, (byte) 0x7c, (byte) 0x7e, (byte) 0xd3, (byte) 0x3d, (byte) 0xd9, (byte) 0xb1, (byte) 0xfd, (byte) 0x90, (byte) 0x28, (byte) 0xcd,
		(byte) 0x00, (byte) 0x45, (byte) 0x93, (byte) 0xb3, (byte) 0x86, (byte) 0xeb, (byte) 0x32, (byte) 0x4b, (byte) 0xe6, (byte) 0xea, (byte) 0x24,
		(byte) 0xb6, (byte) 0x97, (byte) 0xb4, (byte) 0x11, (byte) 0x94, (byte) 0xa0, (byte) 0x16, (byte) 0x53, (byte) 0xfb, (byte) 0xae, (byte) 0xa6,
		(byte) 0xd7, (byte) 0x9a, (byte) 0xe9, (byte) 0xd9, (byte) 0xfb, (byte) 0xa6, (byte) 0x41, (byte) 0xc2, (byte) 0x6d, (byte) 0xec, (byte) 0x4b,
		(byte) 0x0b, (byte) 0x59, (byte) 0xd7, (byte) 0x6c, (byte) 0x2e, (byte) 0xec, (byte) 0x9b, (byte) 0x5d, (byte) 0x6f, (byte) 0x76, (byte) 0x66,
		(byte) 0xcb, (byte) 0xb0, (byte) 0x23, (byte) 0xca, (byte) 0x2c, (byte) 0x8d, (byte) 0xb6, (byte) 0x3a, (byte) 0x6e, (byte) 0xdc, (byte) 0x29,
		(byte) 0xd1, (byte) 0xbd, (byte) 0x1d, (byte) 0x89, (byte) 0x3f, (byte) 0xeb, (byte) 0xc7, (byte) 0x22, (byte) 0x09, (byte) 0xb8, (byte) 0x1d,
		(byte) 0x2e, (byte) 0x04, (byte) 0x98, (byte) 0x71, (byte) 0x1a, (byte) 0x35, (byte) 0x26, (byte) 0x7d, (byte) 0xaa, (byte) 0xf2, (byte) 0xdb,
		(byte) 0xc0, (byte) 0x01, (byte) 0x8a, (byte) 0x56, (byte) 0x76, (byte) 0xd1, (byte) 0x27, (byte) 0xa3, (byte) 0x2b, (byte) 0xc8, (byte) 0x58,
		(byte) 0xea, (byte) 0x76, (byte) 0x72, (byte) 0xe6, (byte) 0xf9, (byte) 0xea, (byte) 0xa0, (byte) 0x54, (byte) 0xf4, (byte) 0xb2, (byte) 0xa4,
		(byte) 0xc0, (byte) 0xbb, (byte) 0xec, (byte) 0x54, (byte) 0x81, (byte) 0x3f, (byte) 0x58, (byte) 0x37, (byte) 0x3c, (byte) 0x69, (byte) 0x45,
		(byte) 0xc8, (byte) 0xb7, (byte) 0xb1, (byte) 0x60, (byte) 0x3b, (byte) 0x3d, (byte) 0x20, (byte) 0x5b, (byte) 0x97, (byte) 0xce, (byte) 0xd2,
		(byte) 0xfc, (byte) 0xb1, (byte) 0xf2, (byte) 0xaf, (byte) 0xa2, (byte) 0xcb, (byte) 0x67, (byte) 0x74, (byte) 0xad, (byte) 0x58, (byte) 0x79,
		(byte) 0xc8, (byte) 0xfe, (byte) 0xc1, (byte) 0x54, (byte) 0x71, (byte) 0xea, (byte) 0x98, (byte) 0x0b, (byte) 0x59, (byte) 0xc6, (byte) 0x21,
		(byte) 0xa0, (byte) 0x94, (byte) 0x7f, (byte) 0x91, (byte) 0xde, (byte) 0xfd, (byte) 0x61, (byte) 0xfc, (byte) 0x3c, (byte) 0xa1, (byte) 0x71,
		(byte) 0x47, (byte) 0x9f, (byte) 0x97, (byte) 0x89, (byte) 0x0d, (byte) 0x43, (byte) 0x74, (byte) 0x97, (byte) 0xec, (byte) 0x85, (byte) 0xfe,
		(byte) 0x2e, (byte) 0x0d, (byte) 0xe7, (byte) 0x49, (byte) 0xca, (byte) 0x55, (byte) 0x0e, (byte) 0xdd, (byte) 0xf4, (byte) 0x38, (byte) 0xf8,
		(byte) 0x22, (byte) 0xb1, (byte) 0x7e, (byte) 0x55, (byte) 0x9e, (byte) 0x56, (byte) 0xea, (byte) 0x0f, (byte) 0x4a, (byte) 0x3a, (byte) 0x3d,
		(byte) 0x0f, (byte) 0x86, (byte) 0x64, (byte) 0x57, (byte) 0x51, (byte) 0xf9, (byte) 0xa3, (byte) 0x0c, (byte) 0x23, (byte) 0xe4, (byte) 0x2a,
		(byte) 0x6a, (byte) 0xdf, (byte) 0x20, (byte) 0x31, (byte) 0xf8, (byte) 0xdd, (byte) 0x6d, (byte) 0xa8, (byte) 0xc4, (byte) 0xdf, (byte) 0x42,
		(byte) 0x7d, (byte) 0xae, (byte) 0xd2, (byte) 0xac, (byte) 0x7d, (byte) 0xd7, (byte) 0x1f, (byte) 0x85, (byte) 0x67, (byte) 0xa4, (byte) 0x4f,
		(byte) 0x97, (byte) 0x21, (byte) 0x25, (byte) 0x61, (byte) 0xd0, (byte) 0xa9, (byte) 0x6b, (byte) 0x77, (byte) 0x47, (byte) 0xc7, (byte) 0x97,
		(byte) 0x47, (byte) 0x13, (byte) 0x03, (byte) 0x1a, (byte) 0xfa, (byte) 0xc8, (byte) 0xe2, (byte) 0x05, (byte) 0xd7, (byte) 0xa6, (byte) 0x0e,
		(byte) 0xda, (byte) 0x71, (byte) 0x18, (byte) 0x42, (byte) 0xc5, (byte) 0xaa, (byte) 0xd8, (byte) 0xb0, (byte) 0x96, (byte) 0x53, (byte) 0x2f,
		(byte) 0xd3, (byte) 0x78, (byte) 0xad, (byte) 0x8f, (byte) 0x2b, (byte) 0xc4, (byte) 0x91, (byte) 0x3b, (byte) 0x07, (byte) 0xd7, (byte) 0x90,
		(byte) 0x09, (byte) 0xcb, (byte) 0x55, (byte) 0xcc, (byte) 0xf7, (byte) 0xcc, (byte) 0xbd, (byte) 0xcf, (byte) 0xc5, (byte) 0x3b, (byte) 0xc1,
		(byte) 0x34, (byte) 0x1d, (byte) 0x35, (byte) 0x3c, (byte) 0x59, (byte) 0x8d, (byte) 0x75, (byte) 0x35, (byte) 0xf7, (byte) 0xf7, (byte) 0xb7,
		(byte) 0xdb, (byte) 0xd6, (byte) 0x90, (byte) 0x53, (byte) 0xdb, (byte) 0x66, (byte) 0x20, (byte) 0x0e, (byte) 0xf7, (byte) 0x98, (byte) 0xb0,
		(byte) 0xbd, (byte) 0x51, (byte) 0xa4, (byte) 0x49, (byte) 0xb4, (byte) 0x3f, (byte) 0x1d, (byte) 0xe2, (byte) 0x82, (byte) 0x2b, (byte) 0x04,
		(byte) 0x3c, (byte) 0x13, (byte) 0x4b, (byte) 0x39, (byte) 0xb6, (byte) 0xbd, (byte) 0xa8, (byte) 0x00, (byte) 0xe7, (byte) 0x33, (byte) 0x60,
		(byte) 0xe5, (byte) 0xfa, (byte) 0xf1, (byte) 0x7b, (byte) 0xd5, (byte) 0x9b, (byte) 0x2b, (byte) 0x4c, (byte) 0x9f, (byte) 0x81, (byte) 0xb6,
		(byte) 0xb9, (byte) 0xb8, (byte) 0x55, (byte) 0x16, (byte) 0x5f, (byte) 0x7a, (byte) 0x05, (byte) 0x07, (byte) 0xe6, (byte) 0xb3, (byte) 0x3e,
		(byte) 0xbc, (byte) 0x8b, (byte) 0xc3, (byte) 0x2f, (byte) 0x37, (byte) 0x23, (byte) 0x19, (byte) 0x39, (byte) 0xd1, (byte) 0xa2, (byte) 0x4c,
		(byte) 0xba, (byte) 0x81, (byte) 0x78, (byte) 0xa3, (byte) 0x99, (byte) 0xd3, (byte) 0xb0, (byte) 0x53, (byte) 0xb9, (byte) 0x38, (byte) 0x44,
		(byte) 0x2b, (byte) 0xfc, (byte) 0x8f, (byte) 0x7b, (byte) 0x0f, (byte) 0xfe, (byte) 0x99, (byte) 0xca, (byte) 0xfb, (byte) 0x37, (byte) 0x3e,
		(byte) 0x1d, (byte) 0xd4, (byte) 0x99, (byte) 0x3c, (byte) 0xdd, (byte) 0xd5, (byte) 0x6f, (byte) 0x48, (byte) 0xc2, (byte) 0xe1, (byte) 0x83,
		(byte) 0x23, (byte) 0xab, (byte) 0x7f, (byte) 0x52, (byte) 0xa9, (byte) 0x89, (byte) 0xc4, (byte) 0x61, (byte) 0x6f, (byte) 0xae, (byte) 0x02,
		(byte) 0x66, (byte) 0xe9, (byte) 0x7a, (byte) 0x67, (byte) 0x67, (byte) 0xad, (byte) 0xb7, (byte) 0x80, (byte) 0x7f, (byte) 0xc8, (byte) 0xa8,
		(byte) 0xb5, (byte) 0x61, (byte) 0xc9, (byte) 0x1a, (byte) 0xb3, (byte) 0x57, (byte) 0x73, (byte) 0x6c, (byte) 0xe9, (byte) 0xd3, (byte) 0xa0,
		(byte) 0xfa, (byte) 0xfe, (byte) 0x43, (byte) 0x70, (byte) 0xc3, (byte) 0x71, (byte) 0x46, (byte) 0x2e, (byte) 0xbe, (byte) 0x2e, (byte) 0x02,
		(byte) 0x17, (byte) 0xca, (byte) 0x78, (byte) 0xa0 };

	// closed beta
	private final static byte[] table2 = { (byte) 0x86, (byte) 0xFA, (byte) 0x1A, (byte) 0x1C, (byte) 0x07, (byte) 0xBD, (byte) 0xD8, (byte) 0x64,
		(byte) 0xCE, (byte) 0xEE, (byte) 0x59, (byte) 0x88, (byte) 0xCD, (byte) 0xA9, (byte) 0x1D, (byte) 0x06, (byte) 0xF7, (byte) 0x3D, (byte) 0x31,
		(byte) 0x58, (byte) 0x83, (byte) 0xA1, (byte) 0x5C, (byte) 0x7E, (byte) 0xDF, (byte) 0xA6, (byte) 0x50, (byte) 0x9E, (byte) 0x89, (byte) 0xA8,
		(byte) 0x12, (byte) 0xD2, (byte) 0x25, (byte) 0x49, (byte) 0x75, (byte) 0xE2, (byte) 0x07, (byte) 0x0F, (byte) 0xEB, (byte) 0x01, (byte) 0x97,
		(byte) 0x4A, (byte) 0x66, (byte) 0x35, (byte) 0xAB, (byte) 0x32, (byte) 0x9D, (byte) 0xA7, (byte) 0x4E, (byte) 0xA2, (byte) 0x89, (byte) 0x62,
		(byte) 0x0F, (byte) 0x55, (byte) 0x41, (byte) 0xC5, (byte) 0x52, (byte) 0x10, (byte) 0x1F, (byte) 0x47, (byte) 0xB0, (byte) 0xA0, (byte) 0x63,
		(byte) 0xA6, (byte) 0xF0, (byte) 0x1C, (byte) 0x1C, (byte) 0x4C, (byte) 0x9B, (byte) 0x3C, (byte) 0xAC, (byte) 0xE2, (byte) 0xB3, (byte) 0x4E,
		(byte) 0x9F, (byte) 0xF1, (byte) 0xA4, (byte) 0x91, (byte) 0x29, (byte) 0x82, (byte) 0xE4, (byte) 0x76, (byte) 0x0D, (byte) 0x8D, (byte) 0x4F,
		(byte) 0xA3, (byte) 0x34, (byte) 0x4A, (byte) 0xCC, (byte) 0x1C, (byte) 0xC7, (byte) 0x18, (byte) 0x48, (byte) 0x8E, (byte) 0xFE, (byte) 0x18,
		(byte) 0x79, (byte) 0x08, (byte) 0x87, (byte) 0x28, (byte) 0x8E, (byte) 0x24, (byte) 0xB7, (byte) 0x6B, (byte) 0x38, (byte) 0xF2, (byte) 0x58,
		(byte) 0x01, (byte) 0x2D, (byte) 0xA8, (byte) 0x58, (byte) 0x0E, (byte) 0x9C, (byte) 0x54, (byte) 0x29, (byte) 0xCF, (byte) 0xA1, (byte) 0xAE,
		(byte) 0x0A, (byte) 0xD2, (byte) 0x3B, (byte) 0x4A, (byte) 0x10, (byte) 0xF8, (byte) 0xD8, (byte) 0x19, (byte) 0x31, (byte) 0x7D, (byte) 0xF3,
		(byte) 0xAE, (byte) 0x1B, (byte) 0x90, (byte) 0xD2, (byte) 0x2F, (byte) 0x16, (byte) 0xC7, (byte) 0xE5, (byte) 0x3B, (byte) 0xCC, (byte) 0xEF,
		(byte) 0xE1, (byte) 0xE1, (byte) 0x2C, (byte) 0x86, (byte) 0x00, (byte) 0xDD, (byte) 0x35, (byte) 0x67, (byte) 0x8D, (byte) 0x25, (byte) 0xFC,
		(byte) 0xED, (byte) 0x32, (byte) 0x1F, (byte) 0xA9, (byte) 0x1A, (byte) 0x12, (byte) 0x6D, (byte) 0xB0, (byte) 0xF7, (byte) 0x3D, (byte) 0xB6,
		(byte) 0x1F, (byte) 0xE8, (byte) 0x81, (byte) 0x4D, (byte) 0x36, (byte) 0xE7, (byte) 0x25, (byte) 0x30, (byte) 0x21, (byte) 0x90, (byte) 0x86,
		(byte) 0x30, (byte) 0x0E, (byte) 0xEE, (byte) 0x40, (byte) 0xBE, (byte) 0x6E, (byte) 0xDA, (byte) 0xC1, (byte) 0x3A, (byte) 0xAF, (byte) 0xF2,
		(byte) 0xEC, (byte) 0x28, (byte) 0x2C, (byte) 0xF1, (byte) 0xCD, (byte) 0x44, (byte) 0x98, (byte) 0x72, (byte) 0xDA, (byte) 0xCD, (byte) 0xC6,
		(byte) 0xD9, (byte) 0xDF, (byte) 0xF7, (byte) 0xEE, (byte) 0x88, (byte) 0x04, (byte) 0xE1, (byte) 0x62, (byte) 0x00, (byte) 0x08, (byte) 0x0E,
		(byte) 0xCD, (byte) 0x16, (byte) 0x37, (byte) 0xAB, (byte) 0xF9, (byte) 0xF5, (byte) 0x14, (byte) 0xAA, (byte) 0x2E, (byte) 0x00, (byte) 0x4E,
		(byte) 0xF8, (byte) 0x18, (byte) 0x41, (byte) 0x0B, (byte) 0xD9, (byte) 0x6F, (byte) 0x9B, (byte) 0xFA, (byte) 0xAD, (byte) 0x2B, (byte) 0x54,
		(byte) 0x56, (byte) 0x2E, (byte) 0x7F, (byte) 0x2C, (byte) 0x3B, (byte) 0x6A, (byte) 0x82, (byte) 0xA1, (byte) 0x7C, (byte) 0x7C, (byte) 0xA6,
		(byte) 0x8F, (byte) 0x66, (byte) 0x5E, (byte) 0xE7, (byte) 0xCF, (byte) 0x83, (byte) 0xB9, (byte) 0xEA, (byte) 0xFC, (byte) 0xE2, (byte) 0x31,
		(byte) 0xD4, (byte) 0x10, (byte) 0xF3, (byte) 0xF4, (byte) 0x22, (byte) 0xEC, (byte) 0x73, (byte) 0x14, (byte) 0x4F, (byte) 0x94, (byte) 0x78,
		(byte) 0x79, (byte) 0x8F, (byte) 0x1E, (byte) 0x29, (byte) 0xEA, (byte) 0x5F, (byte) 0x21, (byte) 0x1E, (byte) 0x08, (byte) 0x37, (byte) 0xB8,
		(byte) 0xF6, (byte) 0x9A, (byte) 0x2D, (byte) 0xC5, (byte) 0x36, (byte) 0x34, (byte) 0xC1, (byte) 0x97, (byte) 0xDC, (byte) 0x75, (byte) 0xB2,
		(byte) 0xAD, (byte) 0xD7, (byte) 0xE3, (byte) 0x04, (byte) 0xA7, (byte) 0xC0, (byte) 0xC9, (byte) 0x1C, (byte) 0x1A, (byte) 0x00, (byte) 0xE9,
		(byte) 0x2D, (byte) 0x6F, (byte) 0xD6, (byte) 0x8D, (byte) 0xBC, (byte) 0x73, (byte) 0x52, (byte) 0xC0, (byte) 0x8A, (byte) 0xB6, (byte) 0xBA,
		(byte) 0x2C, (byte) 0xA6, (byte) 0x7D, (byte) 0x7B, (byte) 0x6F, (byte) 0xF4, (byte) 0x47, (byte) 0x1A, (byte) 0x72, (byte) 0xE9, (byte) 0xB2,
		(byte) 0x30, (byte) 0x7D, (byte) 0xD4, (byte) 0xD3, (byte) 0x09, (byte) 0x9C, (byte) 0x65, (byte) 0xB0, (byte) 0xD0, (byte) 0x17, (byte) 0xCF,
		(byte) 0xFC, (byte) 0xF2, (byte) 0xFF, (byte) 0x46, (byte) 0xD2, (byte) 0xA6, (byte) 0x43, (byte) 0x11, (byte) 0x76, (byte) 0x2B, (byte) 0xE5,
		(byte) 0x1D, (byte) 0xE5, (byte) 0xC9, (byte) 0x47, (byte) 0x2F, (byte) 0x4B, (byte) 0x1B, (byte) 0xDD, (byte) 0x9A, (byte) 0xFD, (byte) 0x9D,
		(byte) 0x20, (byte) 0xB6, (byte) 0x43, (byte) 0x1A, (byte) 0x64, (byte) 0xE3, (byte) 0x68, (byte) 0xF3, (byte) 0x21, (byte) 0x57, (byte) 0x68,
		(byte) 0xD4, (byte) 0x04, (byte) 0x8F, (byte) 0xC3, (byte) 0xCE, (byte) 0xAF, (byte) 0xA3, (byte) 0xAB, (byte) 0x69, (byte) 0xA3, (byte) 0x3C,
		(byte) 0x34, (byte) 0xBE, (byte) 0x1F, (byte) 0x84, (byte) 0xA8, (byte) 0x0E, (byte) 0x74, (byte) 0xCB, (byte) 0xB7, (byte) 0xE6, (byte) 0xB1,
		(byte) 0x39, (byte) 0x8D, (byte) 0x68, (byte) 0x00, (byte) 0x3A, (byte) 0x9B, (byte) 0x9C, (byte) 0xB1, (byte) 0x09, (byte) 0x1C, (byte) 0x7D,
		(byte) 0x52, (byte) 0x15, (byte) 0x12, (byte) 0xA6, (byte) 0xB0, (byte) 0x83, (byte) 0xD3, (byte) 0x40, (byte) 0x47, (byte) 0x9B, (byte) 0xE4,
		(byte) 0x22, (byte) 0xE3, (byte) 0x6E, (byte) 0x30, (byte) 0xC4, (byte) 0xFC, (byte) 0x6F, (byte) 0x4F, (byte) 0xFE, (byte) 0x9F, (byte) 0x51,
		(byte) 0x14, (byte) 0x13, (byte) 0x57, (byte) 0xF1, (byte) 0xEB, (byte) 0x25, (byte) 0xF7, (byte) 0x95, (byte) 0x4C, (byte) 0x92, (byte) 0xB6,
		(byte) 0x3C, (byte) 0xD0, (byte) 0x34, (byte) 0x79, (byte) 0x59, (byte) 0x33, (byte) 0x20, (byte) 0xBE, (byte) 0xB8, (byte) 0xBF, (byte) 0xE0,
		(byte) 0x0A, (byte) 0xD2, (byte) 0x77, (byte) 0xBC, (byte) 0x43, (byte) 0x5C, (byte) 0x7D, (byte) 0xFC, (byte) 0xE1, (byte) 0x59, (byte) 0x00,
		(byte) 0xDE, (byte) 0x5A, (byte) 0x7D, (byte) 0x44, (byte) 0x11, (byte) 0xAC, (byte) 0x13, (byte) 0xF2, (byte) 0x64, (byte) 0x84, (byte) 0x4F,
		(byte) 0x5D, (byte) 0xA2, (byte) 0xC4, (byte) 0x36, (byte) 0xD7, (byte) 0x23, (byte) 0xFA, (byte) 0xF8, (byte) 0xD1, (byte) 0x14, (byte) 0x8D,
		(byte) 0xF9, (byte) 0xDD, (byte) 0x17, (byte) 0x1D, (byte) 0x52, (byte) 0x41, (byte) 0x22, (byte) 0xF5, (byte) 0x1A, (byte) 0x42, (byte) 0x39,
		(byte) 0xFE, (byte) 0x36, (byte) 0xD5, (byte) 0x0A, (byte) 0x10, (byte) 0x01, (byte) 0xD2, (byte) 0xEA, (byte) 0x12, (byte) 0x82, (byte) 0x5A,
		(byte) 0x48, (byte) 0xD2, (byte) 0x94, (byte) 0x95, (byte) 0x0A, (byte) 0xF7, (byte) 0xAB, (byte) 0x70, (byte) 0xF7, (byte) 0xF2, (byte) 0x98,
		(byte) 0x89, (byte) 0xA1, (byte) 0x68, (byte) 0xF9, (byte) 0xE1, (byte) 0xD6, (byte) 0xE1, (byte) 0xBD, (byte) 0x92, (byte) 0x38, (byte) 0x45,
		(byte) 0x5F, (byte) 0x19, (byte) 0xE2, (byte) 0xEA, (byte) 0x46, (byte) 0x76, (byte) 0xC5, (byte) 0xC3, (byte) 0xF2, (byte) 0xB4, (byte) 0x9F,
		(byte) 0x70, (byte) 0x53, (byte) 0x09, (byte) 0x3F, (byte) 0xB8, (byte) 0x06, (byte) 0x3A, (byte) 0xF3, (byte) 0x46, (byte) 0xC8, (byte) 0x6A,
		(byte) 0xCD, (byte) 0x0A, (byte) 0xE3, (byte) 0xF0, (byte) 0xAA, (byte) 0x34, (byte) 0xD9, (byte) 0x72, (byte) 0x98, (byte) 0x34, (byte) 0x23,
		(byte) 0xD1, (byte) 0x96, (byte) 0x8C, (byte) 0x32, (byte) 0x32, (byte) 0x3B, (byte) 0x00, (byte) 0xA3, (byte) 0x9E, (byte) 0x4F, (byte) 0xED,
		(byte) 0xBC, (byte) 0x97, (byte) 0xD4, (byte) 0x4A, (byte) 0x26, (byte) 0x15, (byte) 0x96, (byte) 0x1D, (byte) 0x0E, (byte) 0x36, (byte) 0xB8,
		(byte) 0xEE, (byte) 0x86, (byte) 0x45, (byte) 0x57, (byte) 0x04, (byte) 0x6D, (byte) 0x2B, (byte) 0xC0, (byte) 0xDB, (byte) 0x91, (byte) 0x0A,
		(byte) 0x46, (byte) 0xCE, (byte) 0x7C, (byte) 0x1F, (byte) 0x3C, (byte) 0x3A, (byte) 0x81, (byte) 0x94, (byte) 0x22, (byte) 0x26, (byte) 0x82,
		(byte) 0x6D, (byte) 0x83, (byte) 0xBD, (byte) 0x13, (byte) 0x2D, (byte) 0x96, (byte) 0x91, (byte) 0x53, (byte) 0x6C, (byte) 0x26, (byte) 0x0C,
		(byte) 0x44, (byte) 0xFE, (byte) 0xBD, (byte) 0xEE, (byte) 0xDA, (byte) 0xCC, (byte) 0xBD, (byte) 0x52, (byte) 0xA6, (byte) 0x11, (byte) 0x3E,
		(byte) 0x10, (byte) 0x42, (byte) 0x20, (byte) 0x60, (byte) 0xEB, (byte) 0x5F, (byte) 0x5B, (byte) 0x0D, (byte) 0x7C, (byte) 0xBB, (byte) 0x80,
		(byte) 0xAC, (byte) 0x2F, (byte) 0xB9, (byte) 0xF9, (byte) 0xD2, (byte) 0x4A, (byte) 0xEB, (byte) 0x54, (byte) 0x80, (byte) 0x60, (byte) 0x62,
		(byte) 0x85, (byte) 0xE5, (byte) 0x1A, (byte) 0xF0, (byte) 0x30, (byte) 0x45, (byte) 0xB7, (byte) 0x44, (byte) 0x82, (byte) 0xEF, (byte) 0x3A,
		(byte) 0x0C, (byte) 0xE0, (byte) 0xE5, (byte) 0x94, (byte) 0xFA, (byte) 0xFD, (byte) 0x2E, (byte) 0xD9, (byte) 0xEB, (byte) 0x8D, (byte) 0x5A,
		(byte) 0xC2, (byte) 0xEF, (byte) 0x39, (byte) 0x51, (byte) 0x71, (byte) 0x92, (byte) 0xFA, (byte) 0xDB, (byte) 0xEF, (byte) 0x14, (byte) 0x88,
		(byte) 0x00, (byte) 0xFF, (byte) 0xE3, (byte) 0xF6, (byte) 0xB5, (byte) 0x34, (byte) 0x34, (byte) 0x40, (byte) 0xF5, (byte) 0xBB, (byte) 0xC8,
		(byte) 0xD3, (byte) 0xB5, (byte) 0xBD, (byte) 0xF6, (byte) 0xCF, (byte) 0xC7, (byte) 0xB1, (byte) 0xF9, (byte) 0x18, (byte) 0x3D, (byte) 0xA2,
		(byte) 0x74, (byte) 0xEF, (byte) 0x40, (byte) 0xBC, (byte) 0x6B, (byte) 0x39, (byte) 0xF2, (byte) 0xC8, (byte) 0x6E, (byte) 0x00, (byte) 0x64,
		(byte) 0x78, (byte) 0x52, (byte) 0x88, (byte) 0x13, (byte) 0xF4, (byte) 0x27, (byte) 0x74, (byte) 0x14, (byte) 0x8F, (byte) 0xCE, (byte) 0x34,
		(byte) 0x5E, (byte) 0xF9, (byte) 0xE0, (byte) 0x6D, (byte) 0x47, (byte) 0xFC, (byte) 0x38, (byte) 0x6D, (byte) 0xB0, (byte) 0x03, (byte) 0xED,
		(byte) 0x6C, (byte) 0xF6, (byte) 0x68, (byte) 0x00, (byte) 0xAC, (byte) 0x2B, (byte) 0xFE, (byte) 0x73, (byte) 0x2C, (byte) 0x94, (byte) 0x9E,
		(byte) 0x3F, (byte) 0x17, (byte) 0x0C, (byte) 0x33, (byte) 0xB9, (byte) 0x8F, (byte) 0x33, (byte) 0x34, (byte) 0xDE, (byte) 0x05, (byte) 0x18,
		(byte) 0xE1, (byte) 0x2B, (byte) 0xB9, (byte) 0x42, (byte) 0x3F, (byte) 0x5F, (byte) 0xA2, (byte) 0xB4, (byte) 0x1E, (byte) 0xE9, (byte) 0x45,
		(byte) 0xF3, (byte) 0x38, (byte) 0x43, (byte) 0xBB, (byte) 0x8E, (byte) 0xB0, (byte) 0x0A, (byte) 0x94, (byte) 0x39, (byte) 0xEE, (byte) 0xFF,
		(byte) 0x9A, (byte) 0xF4, (byte) 0x2D, (byte) 0x6C, (byte) 0x4B, (byte) 0x66, (byte) 0xB1, (byte) 0x1E, (byte) 0x0F, (byte) 0xC2, (byte) 0x18,
		(byte) 0x32, (byte) 0xE1, (byte) 0x74, (byte) 0xFF, (byte) 0x90, (byte) 0x94, (byte) 0xF2, (byte) 0x38, (byte) 0xDD, (byte) 0x56, (byte) 0xDC,
		(byte) 0x78, (byte) 0x91, (byte) 0x96, (byte) 0xD1, (byte) 0x04, (byte) 0x03, (byte) 0x09, (byte) 0x21, (byte) 0x39, (byte) 0xB2, (byte) 0xD4,
		(byte) 0xCC, (byte) 0x2A, (byte) 0xA8, (byte) 0xAB, (byte) 0xE8, (byte) 0x99, (byte) 0x1C, (byte) 0xE7, (byte) 0xE4, (byte) 0x43, (byte) 0x3B,
		(byte) 0x58, (byte) 0xC1, (byte) 0x59, (byte) 0x54, (byte) 0xE8, (byte) 0xBD, (byte) 0x9C, (byte) 0x28, (byte) 0xC6, (byte) 0x81, (byte) 0xFC,
		(byte) 0xAD, (byte) 0x33, (byte) 0x4F, (byte) 0x24, (byte) 0x16, (byte) 0xA0, (byte) 0x47, (byte) 0xD1, (byte) 0x4C, (byte) 0x4D, (byte) 0x39,
		(byte) 0x7A, (byte) 0xC1, (byte) 0xF7, (byte) 0x1D, (byte) 0x04, (byte) 0xCF, (byte) 0xE7, (byte) 0xAE, (byte) 0x17, (byte) 0x71, (byte) 0xD9,
		(byte) 0x37, (byte) 0xDC, (byte) 0x9C, (byte) 0x0A, (byte) 0x0E, (byte) 0x9D, (byte) 0x0E, (byte) 0x04, (byte) 0xD7, (byte) 0x24, (byte) 0xC1,
		(byte) 0x50, (byte) 0x0C, (byte) 0x49, (byte) 0xE3, (byte) 0xBC, (byte) 0xCA, (byte) 0x98, (byte) 0x89, (byte) 0x55, (byte) 0x86, (byte) 0x73,
		(byte) 0xF1, (byte) 0xC3, (byte) 0x8D, (byte) 0x8F, (byte) 0x99, (byte) 0x34, (byte) 0xF7, (byte) 0x4B, (byte) 0xE7, (byte) 0x69, (byte) 0x0A,
		(byte) 0xB0, (byte) 0xC1, (byte) 0x2F, (byte) 0x85, (byte) 0x97, (byte) 0xBF, (byte) 0xC3, (byte) 0xFD, (byte) 0xD0, (byte) 0x62, (byte) 0x75,
		(byte) 0xB1, (byte) 0xAD, (byte) 0xF3, (byte) 0x04, (byte) 0xF3, (byte) 0xF3, (byte) 0x77, (byte) 0x06, (byte) 0xAA, (byte) 0x77, (byte) 0x5A,
		(byte) 0xE7, (byte) 0xEB, (byte) 0x67, (byte) 0x3F, (byte) 0xB5, (byte) 0x40, (byte) 0xA1, (byte) 0x9C, (byte) 0x53, (byte) 0x96, (byte) 0xFD,
		(byte) 0x85, (byte) 0x53, (byte) 0x6E, (byte) 0xED, (byte) 0x52, (byte) 0x05, (byte) 0x3B, (byte) 0x6E, (byte) 0x89, (byte) 0xEF, (byte) 0x95,
		(byte) 0x98, (byte) 0xB6, (byte) 0x66, (byte) 0x34, (byte) 0xD0, (byte) 0x8A, (byte) 0x3F, (byte) 0x44, (byte) 0xEA, (byte) 0x06, (byte) 0x86,
		(byte) 0x13, (byte) 0x39, (byte) 0xEF, (byte) 0x20, (byte) 0xAD, (byte) 0xE4, (byte) 0x73, (byte) 0x2C, (byte) 0x61, (byte) 0x77, (byte) 0x10,
		(byte) 0x3D, (byte) 0xB9, (byte) 0x0B, (byte) 0xC2, (byte) 0x0C, (byte) 0xFD, (byte) 0xF2, (byte) 0x99, (byte) 0xD8, (byte) 0xB1, (byte) 0x57,
		(byte) 0x83, (byte) 0x1B, (byte) 0x24, (byte) 0xA6, (byte) 0xA0, (byte) 0xAB, (byte) 0x97, (byte) 0x3E, (byte) 0xE5, (byte) 0x09, (byte) 0x07,
		(byte) 0x3F, (byte) 0x43, (byte) 0xED, (byte) 0x12, (byte) 0xE3, (byte) 0x36, (byte) 0xCE, (byte) 0x16, (byte) 0x58, (byte) 0xF2, (byte) 0x78,
		(byte) 0x00, (byte) 0x63, (byte) 0xF7, (byte) 0x67, (byte) 0xDC, (byte) 0xD9, (byte) 0x5F, (byte) 0x0D, (byte) 0xAA, (byte) 0x3E, (byte) 0x9A,
		(byte) 0xA3, (byte) 0x83, (byte) 0x72, (byte) 0xFE, (byte) 0xBA, (byte) 0x92, (byte) 0xE9, (byte) 0xD4, (byte) 0x22, (byte) 0xF0, (byte) 0x38,
		(byte) 0x38, (byte) 0x61, (byte) 0xE2, (byte) 0x79, (byte) 0x9B, (byte) 0x5E, (byte) 0x8A, (byte) 0x62, (byte) 0x27, (byte) 0x59, (byte) 0x84,
		(byte) 0x71, (byte) 0xC0, (byte) 0xEB, (byte) 0x95, (byte) 0x28, (byte) 0x0D, (byte) 0x34, (byte) 0xCB, (byte) 0xAB, (byte) 0x25, (byte) 0xC6,
		(byte) 0x3B, (byte) 0xBC, (byte) 0x52, (byte) 0xA5, (byte) 0xCA, (byte) 0x6B, (byte) 0x93, (byte) 0xCA, (byte) 0x23, (byte) 0x6D, (byte) 0x35,
		(byte) 0x87, (byte) 0x41, (byte) 0x87, (byte) 0x3E, (byte) 0x48, (byte) 0xB9, (byte) 0xDF, (byte) 0x0E, (byte) 0xFD, (byte) 0x30, (byte) 0xB8,
		(byte) 0xD1, (byte) 0xB8, (byte) 0x10, (byte) 0x68, (byte) 0x3D, (byte) 0xBC, (byte) 0x09, (byte) 0x04, (byte) 0x31, (byte) 0x94, (byte) 0x5C,
		(byte) 0x91, (byte) 0xAF, (byte) 0x6C };

	private int detectVersion(final InputStream stream, final PakFileHeader pakFileHeader) throws PakFileFormatException, IOException {
		if (pakFileHeader.compMethod != 0 && pakFileHeader.compMethod != 8)
			throw new PakFileFormatException("Unknown compression method " + Short.toString(pakFileHeader.compMethod));

		final int blockSize = pakFileHeader.getSize();
		final byte[] blockData = CommonUtils.bufferedDup(stream, blockSize);

		// fix signature
		for (int i = 0; i < 4; i++) {
			blockData[i] = (byte) ~blockData[i];
		}

		final int blockBodyShift = pakFileHeader.getBodyShift();

		final int xorsize = pakFileHeader.compressedSz < 32 ? pakFileHeader.compressedSz : 32;

		final CRC32 checksum = new CRC32();

		byte[] bytes = null;

		// version 1
		int tbloff = (pakFileHeader.compressedSz & 0x1F) * 32;

		for (int i = 0; i < xorsize; i++) {
			blockData[blockBodyShift + i] ^= table1[tbloff + i];
		}

		boolean compOk = true;
		if (pakFileHeader.compMethod == 8) {
			try {
				bytes = CommonUtils.decompress(blockData);
			} catch (final Exception e) {
				compOk = false;
			}
		}

		for (int i = 0; i < xorsize; i++) {
			blockData[i] ^= table1[tbloff + i];
		}

		if (compOk) {
			checksum.update(bytes);
			if (bytes.length == pakFileHeader.uncompressedSz && pakFileHeader.crc == (int) checksum.getValue())
				return 1;
		}

		// version 2
		tbloff = (pakFileHeader.compressedSz & 0x3FF);

		for (int i = 0; i < xorsize; i++) {
			blockData[blockBodyShift + i] ^= table2[tbloff + i];
		}

		compOk = true;
		if (pakFileHeader.compMethod == 8) {
			try {
				bytes = CommonUtils.decompress(blockData);
			} catch (final Exception e) {
				compOk = false;
			}
		}

		// for (int i = 0; i < xorsize; i++)
		// blockData[blockBodyShift + i] ^= table2[tbloff + i];

		if (compOk) {
			checksum.update(bytes);
			if (bytes.length == pakFileHeader.uncompressedSz && pakFileHeader.crc == (int) checksum.getValue())
				return 2;
		}

		return 0;
	}

	private final LinkedHashSet<PakBlock> pakBlocksSet = new LinkedHashSet<>();

	private final LinkedHashMap<String, PakFileHeader> filesMap = new LinkedHashMap<>();
	private final LinkedHashSet<PakCentralDirHeader> dirsSet = new LinkedHashSet<>();
	private PakEndBlockHeader pakEndBlockHeader = null;
	private int aionVersion = 0;

	/*
	 * (non-Javadoc)
	 * @see
	 * com.beyondaion.geo.databuilders.interfaces.IAionPakAccessor#setPakFile(java
	 * .lang.String)
	 */
	public void setPakFile(final String path, final int version) throws IOException, PakFileFormatException {
		if (path.contains("\\Levels\\common\\")) {
			String[] splitted = path.split("\\\\Levels\\\\common\\\\");
			if (splitted.length > 0) {
				String prefix = splitted[splitted.length-1];
				prefix = prefix.replaceAll("Mesh_Meshes_\\d\\d\\d\\.pak", "").replaceAll("\\\\", "/");
				prefix = "levels/common/" + prefix;
				setPakFile(path, version, prefix);
			} else {
				System.out.println("levels splitted is zero");
			}
		} else if (path.contains("\\Objects\\")) {
			String[] splitted = path.split("\\\\Objects\\\\");
			if (splitted.length > 0) {
				String prefix = splitted[splitted.length-1];
				prefix = prefix.replaceAll("Mesh_Meshes_\\d\\d\\d\\.pak", "");
				String[] split2 = prefix.split("\\\\");
				if (split2.length > 0) {
					if (split2[split2.length - 1].endsWith("_Meshes.pak")) {
						prefix = "";
						for (int i = 0; i < split2.length - 1; i++) {
							prefix += split2[i] + "/";
						}
					}
				}
				prefix = "objects/" + prefix.replaceAll("\\\\", "/").toLowerCase();
				setPakFile(path, version, prefix);
			} else {
				System.out.println("objects splitted is zero");
			}
		} else if (path.contains("\\Levels\\idabpro\\")) {
			String[] splitted = path.split("\\\\Levels\\\\idabpro\\\\");
			if (splitted.length > 0) {
				String prefix = splitted[splitted.length - 1];
				prefix = prefix.replaceAll("idabpro.pak", "").replaceAll("\\\\", "/");
				prefix = "levels/idabpro/" + prefix;
				setPakFile(path, version, prefix);
			} else {
				System.out.println("Levels/idabpro splitted is zero.");
			}
		}
		else {
			setPakFile(path, version, "");
		}
	}

	public void setPakFile(final String path, final int version, final String prefix) throws IOException, PakFileFormatException {
		if (version == 0)
			throw new PakFileFormatException("Unknown Aion version: " + version);

		close();

		aionVersion = version;
		pakFileStream = new FileInputStream(path);
		final DataInputStream pakInputStream = new DataInputStream(pakFileStream);

		do {
			short signature1;
			final long blockStartPosition = pakFileStream.getChannel().position();
			try {
				signature1 = pakInputStream.readShort();
			} catch (final Exception e) {
				break;
			}
			if (signature1 != PAK_SIGNATURE1)
				throw new PakFileFormatException("Unknown Pak signature1: " + Short.toString(signature1));

			final short signature2 = pakInputStream.readShort();
			switch (signature2) {
				case PAK_SIGNATURE2_FILE:
					final PakFileHeader pakFileHeader = new PakFileHeader();
					pakFileHeader.blockStartPosition = blockStartPosition;
					pakFileHeader.read(pakInputStream); // TODO Check bytes count read

					pakFileHeader.bodyStartPosition = pakFileStream.getChannel().position();

					// if (aionVersion == 0)
					// {
					// pakFileStream.getChannel().position(pakFileHeader.blockStartPosition);
					// //pakFileHeader.version = 2;
					// aionVersion = detectVersion(pakFileStream, pakFileHeader);
					//
					// pakFileStream.getChannel().position(pakFileHeader.bodyStartPosition);
					// //if (pakFileHeader.version == 0)
					// if (aionVersion == 0)
					// throw new PakFileFormatException("Unknown Aion version");
					// System.out.println("Aion version: " + Integer.toString(pakFileHeader.version));
					// }

					pakFileStream.skip(pakFileHeader.getBodySize());

					final String fileName = new String(pakFileHeader.fileNameBytes);

					filesMap.put(prefix + fileName.toLowerCase().replace('\\', '/').trim(), pakFileHeader);
					pakBlocksSet.add(pakFileHeader);
					break;
				case PAK_SIGNATURE2_DIR:
					final PakCentralDirHeader pakCentralDirHeader = new PakCentralDirHeader();
					pakCentralDirHeader.read(pakInputStream); // TODO Check bytes count read

					if (pakCentralDirHeader.getBodySize() > 0) {
						pakCentralDirHeader.bodyStartPosition = pakFileStream.getChannel().position();

						pakFileStream.skip(pakCentralDirHeader.getBodySize());
					} else {
						pakCentralDirHeader.bodyStartPosition = 0;
					}

					dirsSet.add(pakCentralDirHeader);
					pakBlocksSet.add(pakCentralDirHeader);
					break;
				case PAK_SIGNATURE2_END:
					if (pakEndBlockHeader == null) {
						pakEndBlockHeader = new PakEndBlockHeader();
						pakEndBlockHeader.read(pakInputStream); // TODO Check bytes count read

						pakBlocksSet.add(pakEndBlockHeader);
					} else {
						System.out.println("WARNING: Duplicate End signature");
					}
					break;
				default:
					throw new PakFileFormatException("Unknown Pak signature2: " + Short.toString(signature2));
			}
		} while (true);
	}

	/*
	 * (non-Javadoc)
	 * @see com.beyondaion.geo.databuilders.interfaces.IAionPakAccessor#close()
	 */
	public void close() {
		if (pakFileStream != null) {
			try {
				pakFileStream.close();
			} catch (final IOException e) {
				System.err.println(e.toString());
				e.printStackTrace();
			}
		}
		pakBlocksSet.clear();
		filesMap.clear();
		dirsSet.clear();
		pakEndBlockHeader = null;
		aionVersion = 0;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.beyondaion.geo.databuilders.interfaces.IAionPakAccessor#convertToZip()
	 */
	public void convertToZip(final OutputStream outputStream) throws AionException, IOException {
		if (pakFileStream == null)
			throw new AionException("Pak file is not set");

		// System.out.println("Total: " + pakBlocksSet.size());
		// int k = 0;
		final DataOutputStream zipOutputStream = new DataOutputStream(outputStream);
		for (final PakBlock block : pakBlocksSet) {
			int bodySize = 0;
			zipOutputStream.writeShort(ZIP_SIGNATURE1);
			if (block instanceof PakFileHeader) {
				final PakFileHeader pakFileHeader = (PakFileHeader) block;

				zipOutputStream.writeShort(ZIP_SIGNATURE2_FILE);
				block.write(zipOutputStream);

				pakFileStream.getChannel().position(block.bodyStartPosition);

				// write extra block
				if (pakFileHeader.extraFieldsSz > 0) {
					CommonUtils.bufferedCopy(pakFileStream, zipOutputStream, pakFileHeader.extraFieldsSz);
				}

				bodySize = pakFileHeader.compressedSz;

				// Decrypt top of body block
				final int decryptBlockSize = bodySize < 32 ? bodySize : 32;
				final byte[] decryptBlock = new byte[decryptBlockSize];
				pakFileStream.read(decryptBlock); // TODO Check bytes count read

				if (aionVersion == 1) {
					final int tbloff = (bodySize & 0x1F) * 32;
					for (int i = 0; i < decryptBlockSize; i++) {
						decryptBlock[i] ^= table1[tbloff + i];
					}
				} else if (aionVersion == 2) {
					final int tbloff = (bodySize & 0x3FF);
					for (int i = 0; i < decryptBlockSize; i++) {
						decryptBlock[i] ^= table2[tbloff + i];
					}
				} else
					// impossible situation
					throw new PakFileFormatException("Unknown Aion version");

				zipOutputStream.write(decryptBlock);

				bodySize -= decryptBlockSize;

			} else if (block instanceof PakCentralDirHeader) {
				zipOutputStream.writeShort(ZIP_SIGNATURE2_DIR);
				block.write(zipOutputStream);

				bodySize = (int) block.getBodySize();

				if (bodySize > 0) {
					pakFileStream.getChannel().position(block.bodyStartPosition);
				}
			} else if (block instanceof PakEndBlockHeader) {
				zipOutputStream.writeShort(ZIP_SIGNATURE2_END);
				block.write(zipOutputStream);
			} else
				throw new PakFileFormatException("Unsupported block type: " + block.getClass().toString());

			// System.out.println(k++);
			if (bodySize == 0) {
				continue;
			}

			// store body
			CommonUtils.bufferedCopy(pakFileStream, zipOutputStream, bodySize);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.beyondaion.geo.databuilders.interfaces.IAionPakAccessor#getFilesName()
	 */
	
	public Set<String> getFilesName() {
		return filesMap.keySet();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.beyondaion.geo.databuilders.interfaces.IAionPakAccessor#getUpakedFileSize
	 * (java.lang.String)
	 */
	
	public int getUnpakedFileSize(final String fileName) throws AionException {
		if (!filesMap.containsKey(fileName))
			throw new AionException("File [" + fileName + " doesn't exist in the Pak file");
		return filesMap.get(fileName).uncompressedSz;
	}
}
