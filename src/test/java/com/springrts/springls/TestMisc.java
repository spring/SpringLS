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

package com.springrts.springls;


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
}
