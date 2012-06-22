package com.dexnamic.alwayscharged;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class MainPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener,
		Preference.OnPreferenceClickListener
		{

	public static Boolean UPGRADED_TO_PRO = false;
	
	static final int FIRST_TIME_DIALOG_ID = 1;
	static final int ABOUT_DIAlOG_ID = 2;
	static final int CHANGELOG_DIALOG_ID = 3;
	static final int UPGRADE_DIALOG_ID = 4;

	private Preference mPreferenceAbout;
	private Preference mPreferenceAdvanced;
	private Preference mPreferenceUpgrade;
	private Preference mPreferenceSetAlarm;

	SharedPreferences mSettings;
	SharedPreferences.Editor mEditor;

	public final static String KEY_UPGRADE = "key_upgrade";
	public final static String KEY_SET_ALARM = "key_set_alarm";
	public final static String KEY_ADVANCED = "key_advanced";
//	public final static String KEY_HOUR = "key_hour";
//	public final static String KEY_MINUTE = "key_minute";
	public final static String KEY_FIRST_TIME = "key_first_time";
	public final static String KEY_ABOUT = "key_about";
	public final static String KEY_VERSION_CODE = "key_version_code";

	public final static String KEY_SNOOZE_TIME_MIN = "key_snooze";
	public final static String KEY_DURATION = "key_alarm_duration";
	public final static String KEY_MOTION_TOLERANCE = "key_motion_tolerance";

	public final static String KEY_REPEAT_COUNT = "key_repeat_count";
	public static final int TIMES_TO_REPEAT = 2;

	private boolean mFirstInstance = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		PreferenceManager.setDefaultValues(this, R.xml.advanced_preferences, false);

		PreferenceScreen ps = getPreferenceScreen();
		mSettings = ps.getSharedPreferences();
		mEditor = mSettings.edit();

		mPreferenceAdvanced = ps.findPreference(KEY_ADVANCED);
		mPreferenceAdvanced.setOnPreferenceClickListener(this);
		if(UPGRADED_TO_PRO == false) {
//			mPreferenceAdvanced.
		}

		mPreferenceUpgrade = ps.findPreference(KEY_UPGRADE);
		mPreferenceUpgrade.setOnPreferenceClickListener(this);

		mPreferenceAbout = ps.findPreference(KEY_ABOUT);
		mPreferenceAbout.setOnPreferenceClickListener(this);

		mPreferenceSetAlarm = ps.findPreference(KEY_SET_ALARM);
		mPreferenceSetAlarm.setOnPreferenceClickListener(this);

		// set wallpaper as background
		// TODO: fix stretching issue
		// try {
		// Class<?> _WallpaperManager = Class
		// .forName("android.app.WallpaperManager");
		// Class<?>[] parameterTypes = { Context.class };
		// Method _WM_getinstance = _WallpaperManager.getMethod("getInstance",
		// parameterTypes);
		// Object[] args = { this };
		// Object wm = _WM_getinstance.invoke(null, args);
		// Method _WM_getDrawable = _WallpaperManager.getMethod("getDrawable",
		// (Class[]) null);
		// Drawable drawable = (Drawable) _WM_getDrawable.invoke(wm,
		// (Object[]) null);
		// int width = getWindow().getAttributes().width;
		// int height = getWindow().getAttributes().height;
		// drawable.setBounds(new Rect(0,0,width,height));
		// LayoutParams lp = new LayoutParams();
		//
		// getWindow().setAttributes(LayoutParams.)
		// getWindow().setBackgroundDrawable(drawable);
		// } catch (Exception e) {
		// Log.e(LOG_TAG, e.getMessage());
		// }

		setVolumeControlStream(AudioManager.STREAM_RING);

		if (savedInstanceState == null) // savedInstanceState is null during
										// first instantiation of class
			mFirstInstance = true;
		else
			mFirstInstance = false;
		
		if(UPGRADED_TO_PRO)
			upgradeToPro();
		else
			advertiseForPro();
	}

	@Override
	protected void onStart() {
		super.onStart();

		// display a change log if the user just upgraded this application
		try {
			PackageManager pm = getPackageManager();
			PackageInfo packageInfo = pm.getPackageInfo(this.getPackageName(), 0);
			if (mFirstInstance && (packageInfo.versionCode > mSettings.getInt(KEY_VERSION_CODE, 0))) {
				mEditor.putInt(KEY_VERSION_CODE, packageInfo.versionCode);
				mEditor.commit();
				showDialog(CHANGELOG_DIALOG_ID);
			}
		} catch (NameNotFoundException e) {
		}

		// display a short educational dialog if this is a new user
		if (mFirstInstance && mSettings.getBoolean(KEY_FIRST_TIME, true))
			showDialog(FIRST_TIME_DIALOG_ID);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
				this);

		mEditor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.email_dev:
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("text/plain");
			String contactEmail = getString(R.string.contact_email);
			i.putExtra(Intent.EXTRA_EMAIL, new String[] { contactEmail });
			String feedback = getString(R.string.feedback);
			String appName = getString(R.string.app_name);
			i.putExtra(Intent.EXTRA_SUBJECT, feedback + " " + "(" + appName + ")");
			// i.putExtra(Intent.EXTRA_TEXT , "body of email");
			Intent i2 = (Intent) i.clone();
			try {
				ComponentName cn = new ComponentName("com.google.android.gm",
						"com.google.android.gm.ComposeActivityGmail");
				i.setComponent(cn);
				startActivity(i);
			} catch (android.content.ActivityNotFoundException ex1) {
				try {
					startActivity(Intent.createChooser(i2, getString(R.string.sendmail)));
				} catch (android.content.ActivityNotFoundException ex2) {
					Toast.makeText(this, getString(R.string.noemail), Toast.LENGTH_SHORT).show();
				}
			}
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
			if (preference == mPreferenceAbout) {
			showDialog(ABOUT_DIAlOG_ID);
		} else if (preference == mPreferenceAdvanced) {
			Intent intent = new Intent(this, AdvancedPreferences.class);
			startActivity(intent);
		} else if (preference == mPreferenceUpgrade) {
			showDialog(UPGRADE_DIALOG_ID);
		} else if (preference == mPreferenceSetAlarm) {
			Intent intent = new Intent(this, ListAlarmsActivity.class);
			startActivity(intent);
		}
		return false;
	}

	private void prefsChanged() {
		try {
			// try to backup preferences to Google if supported on this device
			Class<?> _BackupManager = Class.forName("android.app.backup.BackupManager");
			Constructor<?> constructor = _BackupManager
					.getConstructor(new Class[] { Context.class });
			// reflection: BackupManager bm = new BackupManager(this);
			Object backupManager = constructor.newInstance(this);
			Method dataChanged = _BackupManager.getMethod("dataChanged", (Class[]) null);
			// reflection: backupManager.dataChanged();
			dataChanged.invoke(backupManager, (Object[]) null);
		} catch (Exception e) {
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		prefsChanged();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder;
		switch (id) {
		case FIRST_TIME_DIALOG_ID:
			builder = aboutDialogBuilder();
			builder.setNegativeButton(getString(R.string.dontshowagain),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							mEditor.putBoolean(KEY_FIRST_TIME, false);
							mEditor.commit();
							dialog.dismiss();
						}
					});
			AlertDialog alertDialog = builder.create();
			return alertDialog;
		case ABOUT_DIAlOG_ID:
			builder = aboutDialogBuilder();
			alertDialog = builder.create();
			return alertDialog;
		case CHANGELOG_DIALOG_ID:
			builder = changelogDialogBuilder();
			alertDialog = builder.create();
			return alertDialog;
		case UPGRADE_DIALOG_ID:
			builder = upgradeDialogBuilder();
			alertDialog = builder.create();
			return alertDialog;
		}
		return null;
	}

	private AlertDialog.Builder aboutDialogBuilder() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.first_time_dialog,
				(ViewGroup) findViewById(R.id.first_time_layout_root));
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setTitle(getString(R.string.app_name));
		builder.setPositiveButton(getString(R.string.close), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		return builder;
	}

	private AlertDialog.Builder changelogDialogBuilder() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.changelog_dialog,
				(ViewGroup) findViewById(R.id.changelog_layout_root));
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setTitle(getString(R.string.changelog_title));
		builder.setPositiveButton(getString(R.string.close), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		return builder;
	}

	private AlertDialog.Builder upgradeDialogBuilder() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.upgrade_dialog,
				(ViewGroup) findViewById(R.id.upgrade_layout_root));
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setTitle(getString(R.string.upgrade_to_pro));
		builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setPositiveButton(getString(R.string.purchase), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				upgradeToPro();
				dialog.dismiss();
			}
		});
		return builder;
	}
	
	private void advertiseForPro() {
		mPreferenceAdvanced.setEnabled(false);
	}
	
	private void upgradeToPro() {
		UPGRADED_TO_PRO = true;
		PreferenceScreen ps = getPreferenceScreen();
		ps.removePreference(mPreferenceUpgrade);
		
		mPreferenceAdvanced.setEnabled(true);
	}

}
