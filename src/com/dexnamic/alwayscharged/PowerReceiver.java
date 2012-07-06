package com.dexnamic.alwayscharged;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PowerReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action != null) {

			/*
			 * If user connects their device to a power source, then cancel the
			 * snooze
			 */
			try {
				if (action.equals((String) Intent.class.getField("ACTION_POWER_CONNECTED")
						.get(null))) {
					Log.v(getClass().getSimpleName(), "ACTION_POWER_CONNECTED");
					Scheduler.cancelSnooze(context);
					return;
				}
			} catch (Exception e) {
			}

			// final String ACTION_DISCONNECTED = "action_disconnected";
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

}
