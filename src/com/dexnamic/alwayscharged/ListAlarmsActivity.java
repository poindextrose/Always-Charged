package com.dexnamic.alwayscharged;

import com.dexnamic.alwayscharged.billing.BillingService;
import com.dexnamic.alwayscharged.billing.Consts;
import com.dexnamic.alwayscharged.billing.PurchaseObserver;
import com.dexnamic.alwayscharged.billing.ResponseHandler;
import com.dexnamic.alwayscharged.billing.BillingService.RequestPurchase;
import com.dexnamic.alwayscharged.billing.BillingService.RestoreTransactions;
import com.dexnamic.alwayscharged.billing.Consts.PurchaseState;
import com.dexnamic.alwayscharged.billing.Consts.ResponseCode;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.View.OnClickListener;
import android.view.ViewParent;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.Toast;

public class ListAlarmsActivity extends ListActivity implements
		ListAlarmsCursorAdaptor.OnListClickListener, OnClickListener {

	static final int FIRST_TIME_DIALOG_ID = 1;
	static final int ABOUT_DIAlOG_ID = 2;
	static final int CHANGELOG_DIALOG_ID = 3;
	static final int UPGRADE_NEEDED_TO_ADD_DIALOG = 4;

	public final static String KEY_SHOW_INTRO_DIAGLOG = "key_show_intro_dialog";
	public final static String KEY_VERSION_CODE = "key_version_code";

	private DatabaseHelper dbHelper;

	private Button addButton;

	private Cursor cursor;

	private ListAlarmsCursorAdaptor mAdapter;
	private SharedPreferences mSharedPreferences;
	private Editor mEditor;
	private Button upgradeButton;
	private Boolean mHasPurchased;
	private boolean mFirstInstance;
	private Handler mHandler;
	private UpgradePurchaseObserver mUpgradePurchaseObserver;
	private BillingService mBillingService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarm_list);

		addButton = (Button) findViewById(R.id.add_alarm);
		upgradeButton = (Button) findViewById(R.id.upgrade);

		if (android.os.Build.VERSION.SDK_INT >= 11) {
			ViewManager viewManager = (ViewManager) addButton.getParent();
			viewManager.removeView(addButton);
			viewManager.removeView(upgradeButton);
		} else {
			addButton.setOnClickListener(this);
			upgradeButton.setOnClickListener(this);
		}

		dbHelper = new DatabaseHelper(this);

		fillData();

		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		mEditor = mSharedPreferences.edit();

		registerForContextMenu(getListView());

		setVolumeControlStream(AudioManager.STREAM_ALARM);

		mBillingService = new BillingService();
		mBillingService.setContext(this);

		if (savedInstanceState == null) // savedInstanceState is null during
										// first instantiation of class
			mFirstInstance = true;
		else
			mFirstInstance = false;

		mHasPurchased = ResponseHandler.hasPurchased(this);
		if (mHasPurchased) {
			upgradeToPro();
		}
	}

	private void fillData() {
		cursor = dbHelper.getAllAlarms();
		startManagingCursor(cursor);

		// Create the ListAdapter. A SimpleCursorAdapter lets you specify two
		// interesting things:
		// an XML template for your list item, and
		// The column to map to a specific item, by ID, in your template.
		mAdapter = new ListAlarmsCursorAdaptor(this, R.layout.alarm_item, // Use
				// a
				// template
				// that
				// displays
				// a
				// text
				// view
				cursor, // Give the cursor to the list adapter
				this);
		setListAdapter(mAdapter);
	}

	@Override
	protected void onStart() {
		super.onStart();

		// display a change log if the user just upgraded this application
		try {
			PackageManager pm = getPackageManager();
			PackageInfo packageInfo = pm.getPackageInfo(this.getPackageName(),
					0);
			if (mFirstInstance
					&& (packageInfo.versionCode > mSharedPreferences.getInt(
							KEY_VERSION_CODE, 0))) {
				mEditor.putInt(KEY_VERSION_CODE, packageInfo.versionCode);
				mEditor.commit();
				showDialog(CHANGELOG_DIALOG_ID);
			}
		} catch (NameNotFoundException e) {
		}

		SharedPreferences prefs = getSharedPreferences(
				Consts.PURCHASE_PREFERENCES, Context.MODE_PRIVATE);
		if (!prefs.getBoolean(Consts.PURCHASE_RESTORED, false)) {
			checkIfUserPurchasedUpgradeToPro();
		}

		// if app is starting for the first time
		if (mFirstInstance
				&& mSharedPreferences.getBoolean(KEY_SHOW_INTRO_DIAGLOG, true)) {

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
	}

	@SuppressLint("NewApi")
	private void upgradeToPro() {
		mHasPurchased = true;
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			invalidateOptionsMenu();
		}
		try {
			ViewGroup viewGroup = (ViewGroup) upgradeButton.getParent();
			viewGroup.removeView(upgradeButton);
		} catch (Exception e) {
		}
	}

	private void checkIfUserPurchasedUpgradeToPro() {
		if (!AlwaysCharged.isDebuggable) {
			Intent intent = new Intent(Consts.ACTION_RESTORE_TRANSACTIONS);
			intent.setClass(this, BillingService.class);
			startService(intent);
			Toast.makeText(this, R.string.check_purchase, Toast.LENGTH_LONG)
					.show();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mBillingService.unbind();
		if (dbHelper != null)
			dbHelper.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options, menu);
		if (mHasPurchased)
			menu.removeItem(R.id.upgrade);

		if (android.os.Build.VERSION.SDK_INT < 11) {
			menu.findItem(R.id.add_alarm).setIcon(null);
			menu.findItem(R.id.advanced).setIcon(null);
			menu.findItem(R.id.email_dev).setIcon(null);
			menu.findItem(R.id.rate_app).setIcon(null);
		}
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.upgrade:
			doUpgrade();
			return true;
		case R.id.add_alarm:
			addAlarm();
			return true;
		case R.id.email_dev:
			sendFeedbackEmail();
			return true;
		case R.id.rate_app:
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri
					.parse("market://details?id=com.dexnamic.alwayscharged"));
			startActivity(intent);
			return true;
		case R.id.change_log:
			showDialog(CHANGELOG_DIALOG_ID);
			return true;
		case R.id.advanced:
			startActivity(new Intent(this, AdvancedPreferences.class));
			return true;
		case R.id.about:
			showDialog(ABOUT_DIAlOG_ID);
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
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
							mEditor.putBoolean(KEY_SHOW_INTRO_DIAGLOG, false);
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
		case UPGRADE_NEEDED_TO_ADD_DIALOG:
			return createUpgradeDialog(R.string.add_upgrade_dialog);
		}
		return null;
	}

	AlertDialog createUpgradeDialog(int resid) {
		AlertDialog.Builder upGradebuilder = new AlertDialog.Builder(this);
		upGradebuilder
				.setMessage(getString(resid))
				.setCancelable(false)
				.setPositiveButton(getString(R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// startActivity(new
								// Intent(ListAlarmsActivity.this,
								// UpgradeProActivity.class));
								mBillingService.requestPurchase(
										Consts.mProductID,
										Consts.ITEM_TYPE_INAPP, null);
							}
						})
				.setNegativeButton(getString(R.string.cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		return upGradebuilder.create();
	}

	private AlertDialog.Builder aboutDialogBuilder() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.first_time_dialog,
				(ViewGroup) findViewById(R.id.first_time_layout_root));
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setTitle(getString(R.string.app_name));
		builder.setPositiveButton(getString(R.string.close),
				new DialogInterface.OnClickListener() {
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
		builder.setPositiveButton(getString(R.string.close),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		return builder;
	}

	private void sendFeedbackEmail() {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/html");
		String contactEmail = getString(R.string.contact_email);
		i.putExtra(Intent.EXTRA_EMAIL, new String[] { contactEmail });
		String feedback = getString(R.string.feedback);
		String appName = getString(R.string.app_name);
		i.putExtra(Intent.EXTRA_SUBJECT, feedback + " " + "(" + appName + ")");

		startActivity(Intent.createChooser(i, getString(R.string.feedback)));
	}

	@Override
	public void alarmChecked(int id, boolean isChecked) {
		Alarm alarm = dbHelper.getAlarm(id);
		alarm.setEnabled(isChecked);
		dbHelper.updateAlarm(alarm);
	}

	@Override
	public void alarmSelected(int id) {
		Intent intent = new Intent(this, EditAlarmPreferenceActivity.class);
		intent.putExtra("id", id);
		startActivity(intent);
	}

	@Override
	public void onClick(View view) {
		if (view == addButton) {
			addAlarm();
		} else if (view == upgradeButton) {
			doUpgrade();
		}

	}

	private void doUpgrade() {
		// Intent intent = new Intent(this, UpgradeProActivity.class);
		// startActivity(intent);
		mBillingService.requestPurchase(Consts.mProductID,
				Consts.ITEM_TYPE_INAPP, null);
	}

	private void addAlarm() {
		if (ResponseHandler.hasPurchased(this) == false
				&& cursor.getCount() > 0) {
			showDialog(UPGRADE_NEEDED_TO_ADD_DIALOG);
		} else {
			this.alarmSelected(-1);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
		int id = (int) ((AdapterContextMenuInfo) menuInfo).id;
		Alarm alarm = dbHelper.getAlarm(id);
		if (alarm.getEnabled()) {
			menu.getItem(2).setTitle(R.string.disable_alarm);
		} else {
			menu.getItem(2).setTitle(R.string.enable_alarm);
		}

		menu.setHeaderTitle(alarm.getTime(this));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		// Toast.makeText(this,
		// "info.id="+info.id+", info.positino="+info.position,
		// Toast.LENGTH_SHORT).show();
		switch (item.getItemId()) {
		case R.id.edit_alarm:
			alarmSelected((int) info.id);
			return true;
		case R.id.delete_alarm:
			dbHelper.deleteAlarm((int) info.id);
			mAdapter.getCursor().requery();
			mAdapter.notifyDataSetChanged();
			return true;
		case R.id.enable:
			Alarm alarm = dbHelper.getAlarm((int) info.id);
			alarm.setEnabled(!alarm.getEnabled());
			dbHelper.updateAlarm(alarm);
			mAdapter.getCursor().requery();
			mAdapter.notifyDataSetChanged();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * A {@link PurchaseObserver} is used to get callbacks when Android Market
	 * sends messages to this application so that we can update the UI.
	 */
	private class UpgradePurchaseObserver extends PurchaseObserver {
		public UpgradePurchaseObserver(Handler handler) {
			super(ListAlarmsActivity.this, handler);
		}

		@Override
		public void onBillingSupported(boolean supported, String type) {
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState,
				String itemId, int quantity, long purchaseTime,
				String developerPayload) {

			if (purchaseState == PurchaseState.PURCHASED) {
				upgradeToPro();
			}
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request,
				ResponseCode responseCode) {
			Log.v("UpgradePurchaseObserver", request.mProductId + ": "
					+ responseCode);
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions request,
				ResponseCode responseCode) {
			Log.v("UpgradePurchaseObserver",
					"onRestoreTranscationReponse() responseCode="
							+ responseCode);
		}
	}
}
