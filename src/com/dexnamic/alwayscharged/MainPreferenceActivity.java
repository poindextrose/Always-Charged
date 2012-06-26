package com.dexnamic.alwayscharged;

import com.dexnamic.alwayscharged.billing.BillingService;
import com.dexnamic.alwayscharged.billing.Consts;
import com.dexnamic.alwayscharged.billing.PurchaseObserver;
import com.dexnamic.alwayscharged.billing.ResponseHandler;
import com.dexnamic.alwayscharged.billing.UpgradeProActivity;
import com.dexnamic.alwayscharged.billing.BillingService.RequestPurchase;
import com.dexnamic.alwayscharged.billing.BillingService.RestoreTransactions;
import com.dexnamic.alwayscharged.billing.Consts.PurchaseState;
import com.dexnamic.alwayscharged.billing.Consts.ResponseCode;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class MainPreferenceActivity extends PreferenceActivity implements
		Preference.OnPreferenceClickListener {

	static final int FIRST_TIME_DIALOG_ID = 1;
	static final int ABOUT_DIAlOG_ID = 2;
	static final int CHANGELOG_DIALOG_ID = 3;
	static final int UPGRADE_DIALOG_ID = 4;
	
	static final String TAG = "MainPreferenceActivity";

	private Preference mPreferenceAbout;
	private Preference mPreferenceAdvanced;
	private Preference mPreferenceUpgrade;
	private Preference mPreferenceSetAlarm;

	SharedPreferences mSettings;
	SharedPreferences.Editor mEditor;

	public final static String KEY_UPGRADE = "key_upgrade";
	public final static String KEY_SET_ALARM = "key_set_alarm";
	public final static String KEY_ADVANCED = "key_advanced";
	// public final static String KEY_HOUR = "key_hour";
	// public final static String KEY_MINUTE = "key_minute";
	public final static String KEY_FIRST_TIME = "key_first_time";
	public final static String KEY_ABOUT = "key_about";
	public final static String KEY_VERSION_CODE = "key_version_code";

	public final static String KEY_SNOOZE_TIME_MIN = "key_snooze";
	public final static String KEY_DURATION = "key_alarm_duration";
	public final static String KEY_MOTION_TOLERANCE = "key_motion_tolerance";

	public final static String KEY_REPEAT_COUNT = "key_repeat_count";
	public static final int TIMES_TO_REPEAT = 2;

	private boolean mFirstInstance = true;

	// private DungeonsPurchaseObserver mDungeonsPurchaseObserver;
	private Handler mHandler;

	private BillingService mBillingService;
	private Boolean mHasPurchased;
	private UpgradePurchaseObserver mUpgradePurchaseObserver;

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

//		mRestorePurchaseObserver = new RestorePurchaseObserver(this, mHandler);

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

		// if app is starting for the first time
		if (mFirstInstance && mSettings.getBoolean(KEY_FIRST_TIME, true)) {

			checkIfUserPurchasedUpgradeToPro();

			showDialog(FIRST_TIME_DIALOG_ID);
		}
		
		mHandler = new Handler();
		mUpgradePurchaseObserver = new UpgradePurchaseObserver(mHandler);
		ResponseHandler.register(mUpgradePurchaseObserver);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		ResponseHandler.unregister(mUpgradePurchaseObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();

		mHasPurchased = ResponseHandler.hasPurchased(this);
		if (mHasPurchased) {
			upgradeToPro();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void checkIfUserPurchasedUpgradeToPro() {
		mBillingService = new BillingService();
		mBillingService.setContext(this);
		mBillingService.restoreTransactions();
		Toast.makeText(this, R.string.check_purchase, Toast.LENGTH_LONG).show();
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
			sendFeedbackEmail();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	private void sendFeedbackEmail() {
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
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == mPreferenceAbout) {
			showDialog(ABOUT_DIAlOG_ID);
		} else if (preference == mPreferenceAdvanced) {
			Intent intent = new Intent(this, AdvancedPreferences.class);
			startActivity(intent);
		} else if (preference == mPreferenceUpgrade) {
			// showDialog(UPGRADE_DIALOG_ID);
			Intent intent = new Intent(this, UpgradeProActivity.class);
			startActivity(intent);
		} else if (preference == mPreferenceSetAlarm) {
			Intent intent = new Intent(this, ListAlarmsActivity.class);
			startActivity(intent);
		}
		return false;
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
			// builder = upgradeDialogBuilder();
			// alertDialog = builder.create();
			// return alertDialog;
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

	private void upgradeToPro() {
		PreferenceScreen ps = getPreferenceScreen();
		ps.removePreference(mPreferenceUpgrade);
		mPreferenceAdvanced.setEnabled(true);
	}

	/**
	 * A {@link PurchaseObserver} is used to get callbacks when Android Market
	 * sends messages to this application so that we can update the UI.
	 */
	private class UpgradePurchaseObserver extends PurchaseObserver {
		public UpgradePurchaseObserver(Handler handler) {
			super(MainPreferenceActivity.this, handler);
		}

		@Override
		public void onBillingSupported(boolean supported, String type) {
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState, String itemId, int quantity,
				long purchaseTime, String developerPayload) {

			if (purchaseState == PurchaseState.PURCHASED) {
				upgradeToPro();
			}
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request, ResponseCode responseCode) {
			if (Consts.DEBUG) {
				Log.d(TAG, request.mProductId + ": " + responseCode);
			}
			if (responseCode == ResponseCode.RESULT_OK) {
				if (Consts.DEBUG) {
					Log.i(TAG, "purchase was successfully sent to server");
				}
//				logProductActivity(request.mProductId, "sending purchase request");
			} else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
				if (Consts.DEBUG) {
					Log.i(TAG, "user canceled purchase");
				}
//				logProductActivity(request.mProductId, "dismissed purchase dialog");
			} else {
				if (Consts.DEBUG) {
					Log.i(TAG, "purchase failed");
				}
//				logProductActivity(request.mProductId, "request purchase returned " + responseCode);
			}
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions request,
				ResponseCode responseCode) {
			if (responseCode == ResponseCode.RESULT_OK) {
				if (Consts.DEBUG) {
					Log.d(TAG, "completed RestoreTransactions request");
				}
			} else {
				if (Consts.DEBUG) {
					Log.d(TAG, "RestoreTransactions error: " + responseCode);
				}
			}
		}
	}

}
