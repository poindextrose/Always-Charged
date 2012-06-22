package com.dexnamic.alwayscharged;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

	/**
	 * Intent action passed to {@link AlarmActivity} during ACTION_DISCONNECTED
	 * to signal that activity should check battery level and snooze alarm if
	 * power snooze enabled
	 */

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action != null) {

			/*
			 * If user clicked the notification, then cancel the snooze(s)
			 */
			if (action.equals(Scheduler.TYPE_NOTIFY)) {
				Log.v(getClass().getSimpleName(), "onReceive(), Nofication clicked");
				Scheduler.cancelSnooze(context);
				Scheduler.disablePowerSnooze(context);
				return;
			}

			Bundle extras = intent.getExtras();
			if (extras != null) {
				Alarm alarm = (Alarm) extras.getSerializable(Scheduler.TYPE_ALARM);
				int id = alarm.getID();
				// snooze alarm received
				if (action.equals(Scheduler.TYPE_SNOOZE)) {
					Log.v(getClass().getSimpleName(), "onReceive(), Snooze alarm, id=" + id);
					startAlarmService(context, action, alarm);

					// remove enabled from alarm if it does not repeat
					if (alarm.getRepeats() == 0) {
						DatabaseHelper database = new DatabaseHelper(context);
						Alarm alarm_edit = database.getAlarm(id);
						alarm_edit.setEnabled(false);
						database.updateAlarm(alarm_edit);
						database.close();
						Log.v(getClass().getSimpleName(), "onReceive(), disable alarm, id=");
					}

					return;
					// initial alarm received
				} else if (action.contains(Scheduler.TYPE_ALARM)) {
					Log.v(getClass().getSimpleName(), "onReceive(), TYPE_ALARM, id=" + id);
					// original single alarm
					Scheduler.cancelSnooze(context);
					Scheduler.resetRepeatCount(context, null);
					startAlarmService(context, action, alarm);
				}
			}
		}
	}

	static public void startAlarmService(Context context, String action, Alarm alarm) {
		Intent intent = new Intent(context, AlarmService.class);
		intent.setAction(action);
		intent.putExtra(Scheduler.TYPE_ALARM, alarm);
		context.startService(intent);

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		Scheduler.setPartialWakeLock(pm);
	}

	static public void startPowerSnoozeService(Context context, Alarm alarm) {
		Intent intent = new Intent(context, PowerSnoozeService.class);
		intent.putExtra(Scheduler.TYPE_ALARM, alarm);
		context.startService(intent);

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		Scheduler.setPartialWakeLock(pm);
	}

}
