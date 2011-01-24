/*
	Copyright (c) 2011 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.springrts.tasserver.util;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility methods for un-zipping stuff from a simple zip file.
 * @author hoijui
 */
public class ZipUtil {

	private ZipUtil() {}

	/**
	 * Will decompress a ZIP archive to the current folder.
	 * Code copied from
	 * <a href="http://www.rgagnon.com/javadetails/java-0067.html">here</a>,
	 * and slightly modified.
	 */
	public static void unzip(File archive) throws IOException {
		unzip(archive, null);
	}

	/**
	 * Will unzip only the first entry from the archive, and save it to a local
	 * file.
	 * If no file is found inside the archive, no file is created, and no error
	 * reported.
	 */
	public static void unzipSingleFile(File archive, File toFile)
			throws IOException
	{
		unzip(archive, toFile);
	}

	private static void unzip(File archive, File singleOutputFile)
			throws IOException
	{
		InputStream fin = null;
		InputStream bin = null;
		ZipInputStream zin = null;
		try {
			fin = new FileInputStream(archive);
			bin = new BufferedInputStream(fin);
			zin = new ZipInputStream(bin);

			ZipEntry zEntry = zin.getNextEntry();
			do {
				File toFile = (singleOutputFile == null)
						? new File(zEntry.getName())
						: singleOutputFile;
				unzipSingleEntry(zin, toFile);
				zEntry = zin.getNextEntry();
			} while ((singleOutputFile == null) && (zEntry != null));
		} finally {
			if (zin != null) {
				zin.close();
			} else if (bin != null) {
				bin.close();
			} else if (fin != null) {
				fin.close();
			}
		}
	}

	/**
	 * Will unzip next entry from given ZipInputStream
	 */
	private static void unzipSingleEntry(ZipInputStream zin, File toFile)
			throws IOException
	{
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(toFile);
			byte[] b = new byte[512];
			int len = 0;
			while ((len = zin.read(b)) != -1) {
				out.write(b, 0, len);
			}
		} finally {
			out.close();
		}
	}
}
