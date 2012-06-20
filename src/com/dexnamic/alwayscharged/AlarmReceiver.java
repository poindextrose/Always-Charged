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
		Bundle extras = intent.getExtras();
		if (extras == null) {
			Log.e("", "extras == null 938290493");
		}
		int id = extras.getInt("id");
		int day = extras.getInt("day");
		if (action != null) {
			if (action.equals(AlarmScheduler.TYPE_NOTIFY)) {
				AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE, id, day);
				AlarmScheduler.disablePowerSnooze(context);
				return;
			} else if (action.equals(AlarmScheduler.TYPE_SNOOZE)) {
				startAlarmService(context, action, id, day);
				// TODO: here is really the place to cancel the checkbox on an
				// alarm
				return;
			} else if (action.equals(AlarmScheduler.TYPE_ALARM)) {
				// original single alarm
				AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE, id, day);
				AlarmScheduler.resetRepeatCount(context, null);
				startAlarmService(context, action, id, day);
			}

			try {
				if (action.equals((String) Intent.class.getField("ACTION_POWER_CONNECTED")
						.get(null))) {
					AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_SNOOZE, id, day);
					return;
				}
			} catch (Exception e) {
			}

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

			DatabaseHelper database = new DatabaseHelper(context);

			Cursor cursor = database.getAllActiveAlarms();
			if (cursor == null)
				return;

			if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				do {
					int _id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_ID));
					int repeats = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_REPEATS));
					for (Integer i = 0; i < 7; i++) {
						if ((repeats & (1 << day)) > 0)
							AlarmScheduler
									.cancelAlarm(context, AlarmScheduler.TYPE_ALARM, _id, day);
					}
				} while (cursor.moveToNext());
				return;
			}
			if (action.equals(Intent.ACTION_BOOT_COMPLETED)
					|| action.equals(Intent.ACTION_PACKAGE_REPLACED)
					|| action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				do {
					int _id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_ID));
					int repeats = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_REPEATS));
					int hour = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_HOUR));
					int minute = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_MINUTE));
					AlarmScheduler.setDailyAlarm(context, repeats, hour, minute, _id);
				} while (cursor.moveToNext());
				return;
			}
		}
	}

	static public void startAlarmService(Context context, String action, int id, int day) {
		Intent intent = new Intent(context, AlarmService.class);
		intent.setAction(action);
		intent.putExtra("id", id);
		intent.putExtra("day", id);
		context.startService(intent);

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		AlarmScheduler.setPartialWakeLock(pm);
	}

	static public void startPowerSnoozeService(Context context, int id, int day) {
		Intent intent = new Intent(context, PowerSnoozeService.class);
		intent.putExtra("id", id);
		intent.putExtra("day", day);
		context.startService(intent);

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		AlarmScheduler.setPartialWakeLock(pm);
	}

}
