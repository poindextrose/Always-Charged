package com.dexnamic.alwayscharged;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;

public class AlwaysCharged extends Application {

	// private static Context context;

	public static boolean isDebuggable;

	// public static final boolean DEBUG = false;


	// used for obfuscating purchase state in shared preferences
	public final static Long RAND_PURCHASED = 3923923932l;
	public final static Long RAND_REFUNDED = 4862394729l;

	public void onCreate() {
		super.onCreate();
		// AlwaysCharged.context = getApplicationContext();

		isDebuggable = (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
		if (isDebuggable)
			Log.d("AlwaysCharged", "isDebuggabled=" + isDebuggable);
	}

	// public static Context getAppContext() {
	// return AlwaysCharged.context;
	// }

}
