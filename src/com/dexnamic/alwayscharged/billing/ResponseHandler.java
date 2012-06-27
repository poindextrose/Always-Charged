// Copyright 2010 Google Inc. All Rights Reserved.

package com.dexnamic.alwayscharged.billing;

import com.dexnamic.alwayscharged.Alarm;
import com.dexnamic.alwayscharged.DatabaseHelper;
import com.dexnamic.alwayscharged.Scheduler;
import com.dexnamic.alwayscharged.billing.BillingService.RequestPurchase;
import com.dexnamic.alwayscharged.billing.BillingService.RestoreTransactions;
import com.dexnamic.alwayscharged.billing.Consts.PurchaseState;
import com.dexnamic.alwayscharged.billing.Consts.ResponseCode;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.util.Log;

/**
 * This class contains the methods that handle responses from Android Market.
 * The implementation of these methods is specific to a particular application.
 * The methods in this example update the database and, if the main application
 * has registered a {@llink PurchaseObserver}, will also update the UI. An
 * application might also want to forward some responses on to its own server,
 * and that could be done here (in a background thread) but this example does
 * not do that.
 * 
 * You should modify and obfuscate this code before using it.
 */
public class ResponseHandler {
	private static final String TAG = "ResponseHandler";

	/**
	 * This is a static instance of {@link PurchaseObserver} that the
	 * application creates and registers with this class. The PurchaseObserver
	 * is used for updating the UI if the UI is visible.
	 */
	private static PurchaseObserver sPurchaseObserver;

	/**
	 * Registers an observer that updates the UI.
	 * 
	 * @param observer
	 *            the observer to register
	 */
	public static synchronized void register(PurchaseObserver observer) {
		sPurchaseObserver = observer;
	}

	/**
	 * Unregisters a previously registered observer.
	 * 
	 * @param observer
	 *            the previously registered observer.
	 */
	public static synchronized void unregister(PurchaseObserver observer) {
		sPurchaseObserver = null;
	}

	/**
	 * Notifies the application of the availability of the MarketBillingService.
	 * This method is called in response to the application calling
	 * {@link BillingService#checkBillingSupported()}.
	 * 
	 * @param supported
	 *            true if in-app billing is supported.
	 */
	public static void checkBillingSupportedResponse(boolean supported, String type) {
		if (sPurchaseObserver != null) {
			sPurchaseObserver.onBillingSupported(supported, type);
		}
	}

	/**
	 * Starts a new activity for the user to buy an item for sale. This method
	 * forwards the intent on to the PurchaseObserver (if it exists) because we
	 * need to start the activity on the activity stack of the application.
	 * 
	 * @param pendingIntent
	 *            a PendingIntent that we received from Android Market that will
	 *            create the new buy page activity
	 * @param intent
	 *            an intent containing a request id in an extra field that will
	 *            be passed to the buy page activity when it is created
	 */
	public static void buyPageIntentResponse(PendingIntent pendingIntent, Intent intent) {
		if (sPurchaseObserver == null) {
			if (Consts.DEBUG) {
				Log.d(TAG, "UI is not running");
			}
			return;
		}
		sPurchaseObserver.startBuyPageActivity(pendingIntent, intent);
	}

	/**
	 * Notifies the application of purchase state changes. The application can
	 * offer an item for sale to the user via
	 * {@link BillingService#requestPurchase(String)}. The BillingService calls
	 * this method after it gets the response. Another way this method can be
	 * called is if the user bought something on another device running this
	 * same app. Then Android Market notifies the other devices that the user
	 * has purchased an item, in which case the BillingService will also call
	 * this method. Finally, this method can be called if the item was refunded.
	 * 
	 * @param purchaseState
	 *            the state of the purchase request (PURCHASED, CANCELED, or
	 *            REFUNDED)
	 * @param productId
	 *            a string identifying a product for sale
	 * @param orderId
	 *            a string identifying the order
	 * @param purchaseTime
	 *            the time the product was purchased, in milliseconds since the
	 *            epoch (Jan 1, 1970)
	 * @param developerPayload
	 *            the developer provided "payload" associated with the order
	 */
	public static void purchaseResponse(final Context context, final PurchaseState purchaseState,
			final String productId, final String orderId, final long purchaseTime,
			final String developerPayload) {

		setPurchaseState(context, purchaseState);
		if (purchaseState == PurchaseState.REFUNDED) {
			// clear database if purchase refunded
			/*
			 * this should be done on a background thread but this is a tiny
			 * database
			 */
			DatabaseHelper database = new DatabaseHelper(context);
			Cursor cursor = database.getAllActiveAlarms();
			if (cursor != null && cursor.moveToFirst()) {
				cursor.moveToFirst();
				do {
					int _id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_ID));
					Alarm alarm = database.getAlarm(_id);
					Scheduler.cancelAlarm(context, alarm);
				} while (cursor.moveToNext());
			}
			cursor.close();
			database.removeAllAlarms();
			database.close();
		}

		// This needs to be synchronized because the UI thread can change the
		// value of sPurchaseObserver.
		synchronized (ResponseHandler.class) {
			if (sPurchaseObserver != null) {
				sPurchaseObserver.postPurchaseStateChange(purchaseState, productId, 1,
						purchaseTime, developerPayload);
			}
		}
	}

	// used for obfuscating purchase state in shared preferences
	private final static Long RAND_PURCHASED = 3923923932l;
	private final static Long RAND_REFUNDED = 4862394729l;

	public static void setPurchaseState(Context context, PurchaseState purchaseState) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				Consts.PURCHASE_PREFERENCES, Context.MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		Long id = getAndroidID(context);
		Long purchasePref;
		if (purchaseState == PurchaseState.PURCHASED)
			purchasePref = id ^ RAND_PURCHASED;
		else
			purchasePref = id ^ RAND_REFUNDED;
		editor.putLong(Consts.PURCHASE_PREFERENCES, purchasePref);
		editor.commit();
	}

	public static Boolean hasPurchased(Context context) {

		SharedPreferences sharedPreferences = context.getSharedPreferences(
				Consts.PURCHASE_PREFERENCES, Context.MODE_PRIVATE);
		long purchasePref = sharedPreferences.getLong(Consts.PURCHASE_PREFERENCES, 0);
		long id = getAndroidID(context);
		if ((purchasePref ^ id) == RAND_PURCHASED)
			return true;
		else
			return false;
	}

	private static long getAndroidID(Context context) {
		String unique_id = android.provider.Settings.Secure.getString(context.getContentResolver(),
				android.provider.Settings.Secure.ANDROID_ID);
		int length = unique_id.length();
		if (length > 63)
			length = 63;
		Long id = Long.parseLong(unique_id.substring(0, length - 1), 16);
		return id;
	}

	/**
	 * This is called when we receive a response code from Android Market for a
	 * RequestPurchase request that we made. This is used for reporting various
	 * errors and also for acknowledging that an order was sent successfully to
	 * the server. This is NOT used for any purchase state changes. All purchase
	 * state changes are received in the {@link BillingReceiver} and are handled
	 * in {@link Security#verifyPurchase(String, String)}.
	 * 
	 * @param context
	 *            the context
	 * @param request
	 *            the RequestPurchase request for which we received a response
	 *            code
	 * @param responseCode
	 *            a response code from Market to indicate the state of the
	 *            request
	 */
	public static void responseCodeReceived(Context context, RequestPurchase request,
			ResponseCode responseCode) {
		if (sPurchaseObserver != null) {
			sPurchaseObserver.onRequestPurchaseResponse(request, responseCode);
		}
	}

	/**
	 * This is called when we receive a response code from Android Market for a
	 * RestoreTransactions request.
	 * 
	 * @param context
	 *            the context
	 * @param request
	 *            the RestoreTransactions request for which we received a
	 *            response code
	 * @param responseCode
	 *            a response code from Market to indicate the state of the
	 *            request
	 */
	public static void responseCodeReceived(Context context, RestoreTransactions request,
			ResponseCode responseCode) {
//		Log.v("ResponseHandler", "responseCodeReceived = " + responseCode);
		if (responseCode == ResponseCode.RESULT_OK) {
			// Update the shared preferences so that we don't perform
			// a RestoreTransactions again.
			SharedPreferences prefs = context.getSharedPreferences(Consts.PURCHASE_PREFERENCES,
					Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putBoolean(Consts.PURCHASE_RESTORED, true);
			edit.commit();
		}
		if (sPurchaseObserver != null) {
			sPurchaseObserver.onRestoreTransactionsResponse(request, responseCode);
		}
	}
}
