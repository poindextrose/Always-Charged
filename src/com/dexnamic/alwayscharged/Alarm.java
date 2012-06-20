package com.dexnamic.alwayscharged;

import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;

public class Alarm {
    
	private Integer ID;
	private Boolean enabled;
	private String label;
	private Integer hour;
	private Integer minute;
	private Integer repeats;
	private String ringtone;
	private Boolean vibrate;

	public Alarm() {
		ID = -1;
		setEnabled(false);
		setLabel("my daily bedtime");
		setHour(21);
		setMinute(30);
		setRepeats(127); 
		setRingtone("content://settings/system/ringtone");
		setVibrate(true);
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
	
	/**
	 * 
	 * @param day Monday = 0, Tuesday = 1, ..., Sunday = 6
	 * @return 
	 */
	public Boolean getRepeats(Integer day) {
		return ((repeats >> day & 1) == 1);
	}

	public void setRepeats(Integer repeats) {
		this.repeats = repeats;
	}
	
	/**
	 * 
	 * @param day - Monday = 0, Tuesday = 1, ..., Sunday = 6
	 * @param repeat_on_day
	 */
	public void setRepeats(Integer day, Boolean repeat_on_day) {
		if(repeat_on_day) {
			repeats = (1 << day | repeats);
		} else {
			int mask = 0xFF ^ (1 << day);
			repeats = repeats & mask;
		}
	}

	public String getRingtone() {
		return ringtone;
	}

	public void setRingtone(String ringtone) {
		this.ringtone = ringtone;
	}

	public Boolean getVibrate() {
		return vibrate;
	}

	public void setVibrate(Boolean vibrate) {
		this.vibrate = vibrate;
	}

	public Integer getID() {
		return ID;
	}

	public void setID(Integer iD) {
		ID = iD;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public CharSequence getTime(Context context) {

		return formatTime(context, hour, minute);
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
			repeatString.append("Never");
		else
			repeatString.deleteCharAt(repeatString.length() - 1);
		return repeatString.toString();
	}
	

	public static String formatTime(Context context, int hourOfDay, int minute) {
		String suffix = "";
		if (minute == 0) {
			switch (hourOfDay) {
			case 0:
				return "midnight";
			case 12:
				return "noon";
			}
		}
		int timeFormat = Settings.System.getInt(context.getContentResolver(),
				Settings.System.TIME_12_24, 12);
		if (timeFormat == 12) {
			if (hourOfDay >= 12) {
				if (hourOfDay > 12)
					hourOfDay -= 12;
				suffix = " pm";
			} else {
				if (hourOfDay == 0)
					hourOfDay = 12;
				suffix = " am";
			}
		}
		return String.format("%d", hourOfDay) + ":"
				+ String.format("%02d", minute) + suffix;
	}

	public String getRingerName(Context context) {
		String ringerName;// = context.getString(R.string.silent);
		try {
			Uri uri = Uri.parse(ringtone);
//			if (ringtone.length() > 0) {
				ringerName = RingtoneManager.getRingtone(context, uri).getTitle(
						context);
//			}
		} catch (Exception e) {
			ringerName = context.getString(R.string.default_ringtone);
			try {
				Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context,
						RingtoneManager.TYPE_RINGTONE);
				if (ringtone.length() > 0) {
					ringerName = RingtoneManager.getRingtone(context, uri).getTitle(
							context);
				}
			} catch (Exception e2) {
				ringerName = context.getString(R.string.unknown);
			}
		} finally {
			// TODO: call checkVolume() from somewhere
//			checkVolume();
		}
		return ringerName;
	}
	
}
