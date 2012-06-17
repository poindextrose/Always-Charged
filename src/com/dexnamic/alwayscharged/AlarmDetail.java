package com.dexnamic.alwayscharged;

public class AlarmDetail {
    
	private Integer ID;
	private Integer enabled;
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

	public int getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
	}

	public CharSequence getTime() {

		return "" + hour + ":" + minute;
	}

	public static String repeatToString(int repeat) {
		StringBuffer repeatString = new StringBuffer();
		if ((repeat & 1) == 1)
			repeatString.append("Mon,");
		if ((repeat >> 1 & 1) == 1)
			repeatString.append("Tue,");
		if ((repeat >> 2 & 1) == 1)
			repeatString.append("Wed,");
		if ((repeat >> 3 & 1) == 1)
			repeatString.append("Thu,");
		if ((repeat >> 4 & 1) == 1)
			repeatString.append("Fri,");
		if ((repeat >> 5 & 1) == 1)
			repeatString.append("Sat,");
		if ((repeat >> 6 & 1) == 1)
			repeatString.append("Sun,");
		if (repeatString.length() == 0)
			repeatString.append("No repeat");
		else
			repeatString.deleteCharAt(repeatString.length() - 1);
		return repeatString.toString();
	}

}
