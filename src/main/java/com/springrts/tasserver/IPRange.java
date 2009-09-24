/*
 * Created on 2005.9.1
 */

package com.springrts.tasserver;


/**
 * @author Betalord
 */
public class IPRange implements Comparable<IPRange> {

	public long IP_FROM;
	public long IP_TO;
	public String COUNTRY_CODE2; // 2 character country code

	public IPRange(long IP_FROM, long IP_TO, String COUNTRY_CODE2) {

		this.IP_FROM = IP_FROM;
		this.IP_TO = IP_TO;
		this.COUNTRY_CODE2 = COUNTRY_CODE2;
	}

	/**
	 * We need this to be able to "naturally" sort IPRange objects
	 * in a TreeMap list.
	 *
	 * @param other ipRange to compare us with
	 */
	@Override
	public int compareTo(IPRange other) {

		if (this.IP_FROM < other.IP_FROM) {
			return -1;
		} else if (this.IP_FROM > other.IP_FROM) {
			return 1;
		} else {
			if (this.IP_TO < other.IP_TO) {
				return -1; // keep narrower ranges at the top
			} else if (this.IP_TO > other.IP_TO) {
				return 1;
			} else {
				return this.COUNTRY_CODE2.compareTo(other.COUNTRY_CODE2);
			}
		}
	}

	/**
	 * Neededs to fulfill "consistent with equals" criterion,
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
		return (this.IP_FROM == ((IPRange)obj).IP_FROM) &&
				(this.IP_TO == ((IPRange)obj).IP_TO) &&
				(this.COUNTRY_CODE2.equals(((IPRange)obj).COUNTRY_CODE2));
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 17 * hash + (int) (this.IP_FROM ^ (this.IP_FROM >>> 32));
		hash = 17 * hash + (int) (this.IP_TO ^ (this.IP_TO >>> 32));
		hash = 17 * hash + (this.COUNTRY_CODE2 != null ? this.COUNTRY_CODE2.hashCode() : 0);
		return hash;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append(IP_FROM).append(",")
				.append(IP_TO).append(",")
				.append(COUNTRY_CODE2).toString();
	}
}
