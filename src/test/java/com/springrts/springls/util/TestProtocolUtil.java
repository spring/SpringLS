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
import junit.framework.TestCase;

/**
 * @author hoijui
 */
public class TestProtocolUtil extends TestCase {

	public void testBoolToNumber() {

		TestCase.assertEquals(ProtocolUtil.boolToNumber(true),  (byte) 1);
		TestCase.assertEquals(ProtocolUtil.boolToNumber(false), (byte) 0);
	}

	public void testNumberToBool() {

		TestCase.assertFalse(ProtocolUtil.numberToBool((byte) -128));
		TestCase.assertFalse(ProtocolUtil.numberToBool((byte) -99));
		TestCase.assertFalse(ProtocolUtil.numberToBool((byte) -2));
		TestCase.assertFalse(ProtocolUtil.numberToBool((byte) -1));
		TestCase.assertFalse(ProtocolUtil.numberToBool((byte) 0));
		TestCase.assertTrue( ProtocolUtil.numberToBool((byte) 1));
		TestCase.assertFalse(ProtocolUtil.numberToBool((byte) 2));
		TestCase.assertFalse(ProtocolUtil.numberToBool((byte) 99));
		TestCase.assertFalse(ProtocolUtil.numberToBool((byte) 127));
	}

	public void testEncodePassword() {

		TestCase.assertEquals("1B2M2Y8AsgTpgAmY7PhCfg==", ProtocolUtil.encodePassword(""));
		TestCase.assertEquals("gdyb21LQTcIANtvYMT7QVQ==", ProtocolUtil.encodePassword("1234"));
		TestCase.assertEquals("4vxxTEcn7pOV8yTNLn8zHw==", ProtocolUtil.encodePassword("abcd"));
		TestCase.assertEquals("txvQI0/0FGyLcVALYMZFnA==", ProtocolUtil.encodePassword("123abcABC~!@#$%^&*()_+{}|:<>?[];',./'"));
	}

	public void testColorSpringToJava() {

		TestCase.assertEquals(Color.RED,   ProtocolUtil.colorSpringStringToJava("-16776961"));
		TestCase.assertEquals(Color.RED,   ProtocolUtil.colorSpringStringToJava("255"));
		TestCase.assertEquals(Color.GREEN, ProtocolUtil.colorSpringStringToJava("65280"));
		TestCase.assertEquals(Color.BLUE,  ProtocolUtil.colorSpringStringToJava("16711680"));
		TestCase.assertEquals(Color.WHITE, ProtocolUtil.colorSpringStringToJava("16777215"));
		TestCase.assertEquals(Color.GRAY,  ProtocolUtil.colorSpringStringToJava("8421504"));
		TestCase.assertEquals(Color.BLACK, ProtocolUtil.colorSpringStringToJava("0"));
	}

	public void testColorJavaToSpring() {

		TestCase.assertEquals(ProtocolUtil.colorJavaToSpring(Color.RED),   (-16776961 & 0xFFFFFF));
		TestCase.assertEquals(ProtocolUtil.colorJavaToSpring(Color.RED),   255);
		TestCase.assertEquals(ProtocolUtil.colorJavaToSpring(Color.GREEN), 65280);
		TestCase.assertEquals(ProtocolUtil.colorJavaToSpring(Color.BLUE),  16711680);
		TestCase.assertEquals(ProtocolUtil.colorJavaToSpring(Color.WHITE), 16777215);
		TestCase.assertEquals(ProtocolUtil.colorJavaToSpring(Color.GRAY),  8421504);
		TestCase.assertEquals(ProtocolUtil.colorJavaToSpring(Color.BLACK), 0);
	}
}
