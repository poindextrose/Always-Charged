package com.dexnamic.alwayscharged;

public class AlarmDetail {
    
	private Integer ID;
	private String label;
	private Integer hour;
	private Integer minute;
	private Integer repeats;
	private String ringtone;
	private Integer vibrate;

	public AlarmDetail() {
		ID = -1;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Integer getHour() {
		return hour;
	}

	public void setHour(Integer hour) {
		this.hour = hour;
	}

	public Integer getMinute() {
		return minute;
	}

	public void setMinute(Integer minute) {
		this.minute = minute;
	}

	public Integer getRepeats() {
		return repeats;
	}

	public void setRepeats(Integer repeats) {
		this.repeats = repeats;
	}

	public String getRingtone() {
		return ringtone;
	}

	public void setRingtone(String ringtone) {
		this.ringtone = ringtone;
	}

	public Integer getVibrate() {
		return vibrate;
	}

	public void setVibrate(Integer vibrate) {
		this.vibrate = vibrate;
	}

	public Integer getID() {
		return ID;
	}

	public void setID(Integer iD) {
		ID = iD;
	}
	
}
