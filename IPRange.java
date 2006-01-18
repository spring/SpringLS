/*
 * Created on 2005.9.1
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IPRange {
	public long IP_FROM;
	public long IP_TO;
	public String COUNTRY_CODE2;
	public String COUNTRY_CODE3;
	public String COUNTRY_NAME;
	
	public IPRange(long IP_FROM, long IP_TO, String COUNTRY_CODE2, String COUNTRY_CODE3, String COUNTRY_NAME) {
		this.IP_FROM = IP_FROM;
		this.IP_TO = IP_TO;
		this.COUNTRY_CODE2 = COUNTRY_CODE2;
		this.COUNTRY_CODE3 = COUNTRY_CODE3;
		this.COUNTRY_NAME = COUNTRY_NAME;
	}
}
