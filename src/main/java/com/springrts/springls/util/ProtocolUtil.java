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

package com.springrts.springls.util;


import java.awt.Color;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;

import net.iharder.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for spring lobby protocol related stuff, mostly conversion.
 * @author hoijui
 */
public final class ProtocolUtil {

	private static final Logger LOG = LoggerFactory.getLogger(ProtocolUtil.class);

	private ProtocolUtil() {}

	/**
	 * Converts a boolean to a number, according to the lobby protocol standard.
	 * @param aBoolean to be converted into a numerical representation
	 * @return <tt>1</tt> if <tt>aBoolean</tt> is <tt>true</tt>,
	 *   <tt>false</tt> otherwise
	 * @see #numberToBool(byte)
	 */
	public static byte boolToNumber(boolean aBoolean) {
		return (aBoolean ? ((byte) 1) : ((byte) 0));
	}

	/**
	 * Converts a number to a boolean, according to the lobby protocol standard.
	 * @param aNumber to be converted into a boolean
	 * @return <tt>true</tt> if <tt>aNumber == 1</tt>, <tt>false</tt> otherwise
	 * @see #boolToNumber(boolean)
	 */
	public static boolean numberToBool(byte aNumber) {
		return (aNumber == 1);
	}

	// TODO why long (64bit) and not int (32bit)? (IP v6 woudl be 128bit)
	/**
	 * Converts an IP v4 number to a 64bit (long) number, according to the lobby
	 * protocol standard.
	 * @param ip an IP v4 (<tt>Inet4Address</tt>)
	 * @return a 64 bit number representing the supplied IP
	 */
	public static long ip2Long(InetAddress ip) {

		long res;

		byte[] addr = ip.getAddress();
		final long f1 = (long) addr[0] << 24;
		final long f2 = (long) addr[1] << 16;
		final long f3 = (long) addr[2] << 8;
		final long f4 = (long) addr[3];
		res = f1 + f2 + f3 + f4;

		return res;
	}

	/**
	 * This method encodes plain-text passwords to MD5 hashed ones in base-64
	 * form.
	 */
	public static String encodePassword(String plainPassword) {

		String encodedPassword = null;

		byte[] md5Digest = null;
		try {
			md5Digest = Misc.getMD5(plainPassword);
		} catch (NoSuchAlgorithmException ex) {
			LOG.error("Failed to encode password", ex);
		}
		encodedPassword = Base64.encodeBytes(md5Digest);

		return encodedPassword;
	}

	/**
	 * @see #colorSpringToJava(int)
	 */
	public static Color colorSpringStringToJava(String springColor) {

		Color color = null;

		try {
			color = colorSpringToJava(Integer.parseInt(springColor));
		} catch (NumberFormatException ex) {
			LOG.debug("Invalid Spring color format number", ex);
		}

		return color;
	}

	/**
	 * This can be used for converting a lobby protocol color into a java color.
	 * See the myteamcolor argument of the MYBATTLESTATUS command for an
	 * example.
	 * Should be 32-bit signed integer in decimal form (e.g. 255 and not FF)
	 * where each color channel should occupy 1 byte (e.g. in hexadecimal:
	 * "00BBGGRR", B = blue, G = green, R = red).
	 * Example: 255 stands for "000000FF".
	 * @see #colorJavaToSpring(Color)
	 */
	public static Color colorSpringToJava(int springColor) {

		Color color = null;

		int red   = springColor       & 255;
		int green = springColor >> 8  & 255;
		int blue  = springColor >> 16 & 255;
//		int alpha = springColor >> 24 & 255;
		color = new Color(red, green, blue/*, alpha*/);

		return color;
	}

	/**
	 * This can be used for converting a java color into a lobby protocol color.
	 * @see #colorSpringToJava(int)
	 */
	public static int colorJavaToSpring(Color color) {

		int springColor = 0;

//		springColor += color.getAlpha() << 24;
		springColor += color.getBlue()  << 16;
		springColor += color.getGreen() << 8;
		springColor += color.getRed();

		return springColor;
	}
}
