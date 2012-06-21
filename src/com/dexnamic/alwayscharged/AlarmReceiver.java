package com.dexnamic.alwayscharged;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

	/**
	 * Intent action passed to {@link AlarmActivity} during ACTION_DISCONNECTED
	 * to signal that activity should check battery level and snooze alarm if
	 * power snooze enabled
	 */
	public final static String ACTION_DISCONNECTED = "action_disconnected";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action != null) {
			Log.v(getClass().getSimpleName(), "onReceive(), action=" + action);

			/*
			 * If the device is rebooted, application reinstalled, or time zone
			 * changed, set the alarm.
			 */
			if (action.equals(Intent.ACTION_BOOT_COMPLETED)
					|| action.equals(Intent.ACTION_PACKAGE_REPLACED)
					|| action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				DatabaseHelper database = new DatabaseHelper(context);
				Cursor cursor = database.getAllActiveAlarms();
				if (cursor != null && cursor.moveToFirst()) {
					cursor.moveToFirst();
					do {
						int _id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_ID));
						Alarm alarm = database.getAlarm(_id);
						Scheduler.setDailyAlarm(context, alarm);
					} while (cursor.moveToNext());
					cursor.close();
					database.close();
				}
				return;
			} 
			
			/*
			 * If user connects their device to a power source, then cancel the
			 * snooze
			 */
			try {
				if (action.equals((String) Intent.class.getField("ACTION_POWER_CONNECTED")
						.get(null))) {
					// Log.v(getClass().getSimpleName(),
					// "ACTION_POWER_CONNECTED");
					Scheduler.cancelSnooze(context);
					return;
				}
			} catch (Exception e) {
			}

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
				int day = extras.getInt("day");
				// snooze alarm received
				if (action.equals(Scheduler.TYPE_SNOOZE)) {
					Log.v(getClass().getSimpleName(), "onReceive(), Snooze alarm, id=" + id
							+ ", day=" + day);
					startAlarmService(context, action, alarm);

					// remove enabled from alarm if it does not repeat
					if (day <= 0) {
						DatabaseHelper database = new DatabaseHelper(context);
						Alarm alarm_edit = database.getAlarm(id);
						alarm_edit.setEnabled(false);
						database.updateAlarm(alarm_edit);
						database.close();
						Log.v(getClass().getSimpleName(), "onReceive(), disable alarm, id=" + id
								+ ", day=" + day);
					}

					return;
					// initial alarm received
				} else if (action.contains(Scheduler.TYPE_ALARM)) {
					Log.v(getClass().getSimpleName(), "onReceive(), TYPE_ALARM, id=" + id
							+ ", day=" + day);
					// original single alarm
					Scheduler.cancelSnooze(context);
					Scheduler.resetRepeatCount(context, null);
					startAlarmService(context, action, alarm);
				}
			}

			// TODO: enable and test powersnooze
			// disabling power snooze for now until everything else is working
			// try {
			// if (action.equals((String)
			// Intent.class.getField("ACTION_POWER_DISCONNECTED").get(
			// null))) {
			// if (settings.getBoolean(MainActivity.KEY_ALARM_ENABLED, false)
			// && settings.getBoolean(AlarmScheduler.KEY_POWER_SNOOZE, false))
			// startPowerSnoozeService(context);
			// return;
			// }
			// } catch (Exception e) {
			// }

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
