
package com.springrts.chanserv;


public class MuteListRequest {

	/** Name of the channel of which mute list has been requested */
	private String channelName;
	/** Name of the user to whom the mute list should be forwarded */
	private String sendTo;
	/**
	 * Time when this request has been issued.
	 * Old requests will be dropped.
	 * @see System.currentTimeMillis()
	 */
	private long requestTime;
	/**
	 * Name of the channel to which ChanServ should reply.
	 * If <code>""</code>, then ChanServ will reply directly to the user
	 * who requested the mute list (via private message).
	 */
	private String replyToChan;

	public MuteListRequest(String chanName, String sendTo, long requestTime, String replyToChan) {

		this.channelName = chanName;
		this.sendTo = sendTo;
		this.requestTime = requestTime;
		this.replyToChan = replyToChan;
	}

	/**
	 * Name of the channel to which ChanServ should reply.
	 * If <code>""</code>, then ChanServ will reply directly to the user
	 * who requested the mute list (via private message).
	 * @return the replyToChan
	 */
	public String getReplyToChan() {
		return replyToChan;
	}

	/**
	 * Time when this request has been issued.
	 * Old requests will be dropped.
	 * @see System.currentTimeMillis()
	 * @return the requestTime
	 */
	public long getRequestTime() {
		return requestTime;
	}

	/**
	 * Name of the user to whom the mute list should be forwarded
	 * @return the sendTo
	 */
	public String getSendTo() {
		return sendTo;
	}

	/**
	 * Name of the channel of which mute list has been requested
	 * @return the chanName
	 */
	public String getChannelName() {
		return channelName;
	}
}
