/*
	Copyright (c) 2006 Robin Vobruba <hoijui.quaero@gmail.com>

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


/**
 * This class is currently unused.
 * @author Betalord
 * @deprecated
 */
public class GlobalMapGrade {

	private String mapHash;
	private float avGrade = 0;
	private int noVotes = 0;

	public GlobalMapGrade(String mapHash) {
		this.mapHash = mapHash;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof GlobalMapGrade)) {
			return false;
		}
		return this.mapHash.equals(((GlobalMapGrade)obj).mapHash);
	}

	@Override
	public int hashCode() {
		return this.mapHash.hashCode();
	}
}
