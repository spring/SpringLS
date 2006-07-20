public class MuteListRequest {
	public String chanName; // name of the channel whose mute list has been requested
	public String sendTo; // username of the user to whom the mute list should be forwarded.
	public long requestTime; // time when this request has been issued. Old requests will be dropped. Time refers to System.currentTimeMillis().
	public String replyToChan; // name of the channel to which ChanServ should reply. If "" then ChanServ will reply directly to user who requested the mute list (via private message).
	
	public MuteListRequest(String chanName, String sendTo, long requestTime, String replyToChan) {
		this.chanName = chanName;
		this.sendTo = sendTo;
		this.requestTime = requestTime;
		this.replyToChan = replyToChan;
	}
}
