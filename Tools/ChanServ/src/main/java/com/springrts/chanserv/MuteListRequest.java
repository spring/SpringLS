
package com.springrts.chanserv;


public class MuteListRequest {

	/** Name of the channel of which mute list has been requested */
	public String chanName;
	/** Name of the user to whom the mute list should be forwarded */
	public String sendTo;
	/**
	 * Time when this request has been issued.
	 * Old requests will be dropped.
	 * @see System.currentTimeMillis()
	 */
	public long requestTime;
	/**
	 * Name of the channel to which ChanServ should reply.
	 * If <code>""</code>, then ChanServ will reply directly to the user
	 * who requested the mute list (via private message).
	 */
	public String replyToChan;

	public MuteListRequest(String chanName, String sendTo, long requestTime, String replyToChan) {

		this.chanName = chanName;
		this.sendTo = sendTo;
		this.requestTime = requestTime;
		this.replyToChan = replyToChan;
	}
}
