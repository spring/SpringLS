/*
	Copyright (c) 2005 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.ip2country;


/**
 * @author Betalord
 * @author hoijui
 */
public class IPRange implements Comparable<IPRange> {

	private long fromIP;
	private long toIP;
	/** 2 character ISO country code, "XX" means unspecified */
	private String countryCode2;

	public IPRange(long fromIP, long toIP, String countryCode2) {

		this.fromIP = fromIP;
		this.toIP = toIP;
		this.countryCode2 = countryCode2;
	}

	/**
	 * We need this to be able to "naturally" sort IPRange objects
	 * in a TreeMap list.
	 *
	 * @param other ipRange to compare us with
	 */
	@Override
	public int compareTo(IPRange other) {

		if (this.getFromIP() < other.getFromIP()) {
			return -1;
		} else if (this.getFromIP() > other.getFromIP()) {
			return 1;
		} else {
			if (this.getToIP() < other.getToIP()) {
				return -1; // keep narrower ranges at the top
			} else if (this.getToIP() > other.getToIP()) {
				return 1;
			} else {
				return this.getCountryCode2()
						.compareTo(other.getCountryCode2());
			}
		}
	}

	/**
	 * Needs to fulfill "consistent with equals" criterion,
	 * i.e. compareTo(x) == 0 is true if and only if equals(x) == true.
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IPRange)) {
			return false;
		}
		IPRange other = (IPRange) obj;
		return (this.getFromIP() == other.getFromIP())
				&& (this.getToIP() == other.getToIP())
				&& (this.getCountryCode2().equals(other.getCountryCode2()));
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 17 * hash + (int) (this.getFromIP() ^ (this.getFromIP() >>> 32));
		hash = 17 * hash + (int) (this.getToIP() ^ (this.getToIP() >>> 32));
		hash = 17 * hash + ((this.getCountryCode2() != null)
				? this.getCountryCode2().hashCode() : 0);
		return hash;
	}

	@Override
	public String toString() {
		return String.format("%d,%d,%s",
				getFromIP(), getToIP(), getCountryCode2());
	}

	public long getFromIP() {
		return fromIP;
	}

	public long getToIP() {
		return toIP;
	}

	public void setToIP(long toIP) {
		this.toIP = toIP;
	}

	/**
	 * Checks whether a given IP is within our range (boundaries inclusive).
	 * @param ip to be checked whether it is in our range
	 * @return true if the supplied IP is in our range, false otherwise.
	 */
	public boolean contains(long ip) {
		return ((ip >= getFromIP()) && (ip <= getToIP()));
	}

	/**
	 * 2 character ISO country code, "XX" means unspecified
	 * @return the countryCode2
	 */
	public String getCountryCode2() {
		return countryCode2;
	}

	/**
	 * 2 character ISO country code, "XX" means unspecified
	 * @param countryCode2 the countryCode2 to set
	 */
	public void setCountryCode2(String countryCode2) {
		this.countryCode2 = countryCode2;
	}
}
