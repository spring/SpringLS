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

package com.springrts.tasserver;


import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

/**
 * @author hoijui
 */
public class TestMisc extends TestCase {

	public void testMakeSentence() {

		TestCase.assertEquals("", Misc.makeSentence(new String[] {}, 0));
		TestCase.assertEquals("", Misc.makeSentence(new String[] {}, 1));

		String yellowSubSent = "We all live in a yellow submarine";
		String[] yellowSub = yellowSubSent.split(" ");
		List<String> yellowSubList = Arrays.asList(yellowSub);

		// test the array based version
		TestCase.assertEquals(yellowSubSent, Misc.makeSentence(yellowSub));
		TestCase.assertEquals(yellowSubSent, Misc.makeSentence(yellowSub, 0));
		TestCase.assertEquals("submarine", Misc.makeSentence(yellowSub, 6));
		TestCase.assertEquals("yellow submarine", Misc.makeSentence(yellowSub, 5));
		TestCase.assertEquals("a yellow submarine", Misc.makeSentence(yellowSub, 4));
		TestCase.assertEquals("", Misc.makeSentence(yellowSub, 999));

		// test the List based version
		TestCase.assertEquals(yellowSubSent, Misc.makeSentence(yellowSubList, 0));
		TestCase.assertEquals("submarine", Misc.makeSentence(yellowSubList, 6));
		TestCase.assertEquals("yellow submarine", Misc.makeSentence(yellowSubList, 5));
		TestCase.assertEquals("a yellow submarine", Misc.makeSentence(yellowSubList, 4));
		TestCase.assertEquals("", Misc.makeSentence(yellowSubList, 999));
	}

	public void testParseIp() {

		TestCase.assertNull(Misc.parseIp("!@#$%^&*", false, false));
		TestCase.assertNull(Misc.parseIp("java.sun.com", false, false));
		TestCase.assertNull(Misc.parseIp("java.sun.com", true, false));
		TestCase.assertNull(Misc.parseIp("doc.java.sun.com", true, false));
		TestCase.assertNull(Misc.parseIp("localhost", true, false));
		TestCase.assertNull(Misc.parseIp("::", false, false));
		TestCase.assertNull(Misc.parseIp("999.999.999.999", false, false));
		TestCase.assertNull(Misc.parseIp("257.257.257.257", false, false));
		TestCase.assertNull(Misc.parseIp("-1.0.0.0", false, false));
		TestCase.assertNotNull(Misc.parseIp("java.sun.com", false, true));
		TestCase.assertNotNull(Misc.parseIp("java.sun.com", true, true));
		TestCase.assertNotNull(Misc.parseIp("doc.java.sun.com", true, true));
		TestCase.assertNotNull(Misc.parseIp("localhost", true, true));
		TestCase.assertNotNull(Misc.parseIp("::", true, true));
		TestCase.assertNotNull(Misc.parseIp("::", true, false));
		TestCase.assertNotNull(Misc.parseIp("::FFFF:123.124.125.126", true, false));
		TestCase.assertNotNull(Misc.parseIp("::123.124.125.126", true, false));
		TestCase.assertNotNull(Misc.parseIp("123.124.125.126", false, false));
		TestCase.assertNotNull(Misc.parseIp("123.124.125.126", true, false));
		TestCase.assertNotNull(Misc.parseIp("123.124.125.126", false, true));
		TestCase.assertNotNull(Misc.parseIp("123.124.125.126", true, true));
		TestCase.assertNotNull(Misc.parseIp("10.0.0.0", false, false));
		TestCase.assertNotNull(Misc.parseIp("127.0.0.1", false, false));
	}

	public void testBoolToNumber() {

		TestCase.assertEquals(Misc.boolToNumber(true),  (byte) 1);
		TestCase.assertEquals(Misc.boolToNumber(false), (byte) 0);
	}

	public void testNumberToBool() {

		TestCase.assertFalse(Misc.numberToBool((byte) -128));
		TestCase.assertFalse(Misc.numberToBool((byte) -99));
		TestCase.assertFalse(Misc.numberToBool((byte) -2));
		TestCase.assertFalse(Misc.numberToBool((byte) -1));
		TestCase.assertFalse(Misc.numberToBool((byte) 0));
		TestCase.assertTrue( Misc.numberToBool((byte) 1));
		TestCase.assertFalse(Misc.numberToBool((byte) 2));
		TestCase.assertFalse(Misc.numberToBool((byte) 99));
		TestCase.assertFalse(Misc.numberToBool((byte) 127));
	}

	public void testEncodePassword() {

		TestCase.assertEquals("1B2M2Y8AsgTpgAmY7PhCfg==", Misc.encodePassword(""));
		TestCase.assertEquals("gdyb21LQTcIANtvYMT7QVQ==", Misc.encodePassword("1234"));
		TestCase.assertEquals("4vxxTEcn7pOV8yTNLn8zHw==", Misc.encodePassword("abcd"));
		TestCase.assertEquals("txvQI0/0FGyLcVALYMZFnA==", Misc.encodePassword("123abcABC~!@#$%^&*()_+{}|:<>?[];',./'"));
	}

	public void testColorSpringToJava() {

		TestCase.assertEquals(Color.RED,   Misc.colorSpringStringToJava("-16776961"));
		TestCase.assertEquals(Color.RED,   Misc.colorSpringStringToJava("255"));
		TestCase.assertEquals(Color.GREEN, Misc.colorSpringStringToJava("65280"));
		TestCase.assertEquals(Color.BLUE,  Misc.colorSpringStringToJava("16711680"));
		TestCase.assertEquals(Color.WHITE, Misc.colorSpringStringToJava("16777215"));
		TestCase.assertEquals(Color.GRAY,  Misc.colorSpringStringToJava("8421504"));
		TestCase.assertEquals(Color.BLACK, Misc.colorSpringStringToJava("0"));
	}

	public void testColorJavaToSpring() {

		TestCase.assertEquals(Misc.colorJavaToSpring(Color.RED),   (-16776961 & 0xFFFFFF));
		TestCase.assertEquals(Misc.colorJavaToSpring(Color.RED),   255);
		TestCase.assertEquals(Misc.colorJavaToSpring(Color.GREEN), 65280);
		TestCase.assertEquals(Misc.colorJavaToSpring(Color.BLUE),  16711680);
		TestCase.assertEquals(Misc.colorJavaToSpring(Color.WHITE), 16777215);
		TestCase.assertEquals(Misc.colorJavaToSpring(Color.GRAY),  8421504);
		TestCase.assertEquals(Misc.colorJavaToSpring(Color.BLACK), 0);
	}
}
