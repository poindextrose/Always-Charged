// Copyright 2010 Google Inc. All Rights Reserved.

package com.dexnamic.alwayscharged.billing;

import com.dexnamic.alwayscharged.billing.Consts.PurchaseState;
import com.dexnamic.alwayscharged.util.Base64;
import com.dexnamic.alwayscharged.util.Base64DecoderException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Security-related methods. For a secure implementation, all of this code
 * should be implemented on a server that communicates with the application on
 * the device. For the sake of simplicity and clarity of this example, this code
 * is included here and is executed on the device. If you must verify the
 * purchases on the phone, you should obfuscate this code to make it harder for
 * an attacker to replace the code with stubs that treat all purchases as
 * verified.
 */
public class Security {
	private static final String TAG = "Security";

	private static final String KEY_FACTORY_ALGORITHM = "RSA";
	private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
	private static final SecureRandom RANDOM = new SecureRandom();

	/**
	 * This keeps track of the nonces that we generated and sent to the server.
	 * We need to keep track of these until we get back the purchase state and
	 * send a confirmation message back to Android Market. If we are killed and
	 * lose this list of nonces, it is not fatal. Android Market will send us a
	 * new "notify" message and we will re-generate a new nonce. This has to be
	 * "static" so that the {@link BillingReceiver} can check if a nonce exists.
	 */
	private static HashSet<Long> sKnownNonces = new HashSet<Long>();

	/**
	 * A class to hold the verified purchase information.
	 */
	public static class VerifiedPurchase {

		public PurchaseState purchaseState;
		public String notificationId;
		public String productId;
		public String orderId;
		public long purchaseTime;
		public String developerPayload;

		public VerifiedPurchase(PurchaseState purchaseState, String notificationId,
				String productId, String orderId, long purchaseTime, String developerPayload) {
			this.purchaseState = purchaseState;
			this.notificationId = notificationId;
			this.productId = productId;
			this.orderId = orderId;
			this.purchaseTime = purchaseTime;
			this.developerPayload = developerPayload;
		}
	}

	/** Generates a nonce (a random number used once). */
	public static long generateNonce() {
		long nonce = RANDOM.nextLong();
		sKnownNonces.add(nonce);
		return nonce;
	}

	public static void removeNonce(long nonce) {
		sKnownNonces.remove(nonce);
	}

	public static boolean isNonceKnown(long nonce) {
		return sKnownNonces.contains(nonce);
	}

	/**
	 * Verifies that the data was signed with the given signature, and returns
	 * the list of verified purchases. The data is in JSON format and contains a
	 * nonce (number used once) that we generated and that was signed (as part
	 * of the whole data string) with a private key. The data also contains the
	 * {@link PurchaseState} and product ID of the purchase. In the general
	 * case, there can be an array of purchase transactions because there may be
	 * delays in processing the purchase on the backend and then several
	 * purchases can be batched together.
	 * 
	 * @param signedData
	 *            the signed JSON string (signed, not encrypted)
	 * @param signature
	 *            the signature for the data, signed with the private key
	 * @throws Base64DecoderException
	 */
	public static ArrayList<VerifiedPurchase> verifyPurchase(String signedData, String signature) {
		if (signedData == null) {
			Log.e(TAG, "data is null");
			return null;
		}
		Log.v(TAG, "signedData: " + signedData);
		boolean verified = false;
		if (!TextUtils.isEmpty(signature)) {
			/**
			 * Compute your public key (that you got from the Android Market
			 * publisher site).
			 * 
			 * Instead of just storing the entire literal string here embedded
			 * in the program, construct the key at runtime from pieces or use
			 * bit manipulation (for example, XOR with some other string) to
			 * hide the actual key. The key itself is not secret information,
			 * but we don't want to make it easy for an adversary to replace the
			 * public key with one of their own and then fake messages from the
			 * server.
			 * 
			 * Generally, encryption keys / passwords should only be kept in
			 * memory long enough to perform the operation they need to perform.
			 */
			// public key for shawnpoindexter@gmail.com
//			String encryptedPublicKey = "RDjcTIfWa7/x6/7dWttqtFuo1mi3m8Lmw/u91G01WiwWn1qFoOOzjnsRodkWX6OYvIxCoXBIeJGQZuAmgKyIZsfhGSlq+I605RDn89Ei17bQVV86bedEp8cpz0NN0L4ojoaueG1D1OpCu/8ZjFqtdfEAZja2fMsK6ri8uVqYhfyTt96gq2Ps8K4aLcrHia1SEO97QG0nS9IWIYWL5NbJaheL2Si7z+VxrOWpB1FmcwK+a+bvHdJ2aTkZrC/ugWcCgnS1rgjDivYlWXV39VoSeB59ToQ7AHcPRt0Q9D1mi1SRy7vZocc3SU8xzs4+ywe1slMcjLTC4533ily3IH/fQCFM2+lILNTj9A1nZ4E6l4VHsXQN9u7hcZVF84Fl3YigFwkGAwEB";
			// public key for shawn@poindexter.us
//			String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAruTKD1ES8RTVF1DwXZvGTuEn1A3uW0878lFrd7IlCjJMEUSqlP7EZpWctjkzkKbpOJhGkK/VqEBjNY8WT8xCe6yX6orjsG7sOLvlSyhMHVQF0kzBItGKopcXQmh2y4YJsid5ZIljQmokPgf/4NZc+B4DW3igafuwcuf5JBqAeRBSQmW9HQQVm1Xp0XgMo7narR5FcqOh7Z3h3YK6Bj0hbCUGVKJBgzHhpqFdleAoxiNszRLPzG0kJcVBIjrXyLTD6jeHwnegqloJui3yrv4mjoumd3Hhgn4cxlAJIwh1RrfzT/TdlRuQjSR3iruhzFnhihn0Ztf0GexYXsFMpr3i9wIDAQAB";
			String encryptedPublicKey2 = "RDjcTIfWa7/x6/7dWttqtFuo1mi3m8Lmw/u91G01WiwWpeHIDlES8RTVF1DwXZvGTuEn1A3uW0878lFrd7IlCkb2zCodT5Nyvfgq7ZTl+xOzlU4tpTUYTrMaiVF5+JdvbaeS6IvjsG7sOLvlSyhMHVQF0kzBItGKopcXQmh2y4Z9CPoX01IO9LFJiFxSNr3porPVME06pB1DC1snS63bVAZZR2e8HQQVm1Xp0XgMo7narR5FcqOh7Z3h3YK6Bj0hGJ/bOhWa7oc6yxcGODZDc3nBG3n6VqDC1rz9/FVgk5nV4TKFw3egqloJui3yrv4mjoumd3Hhgn4cxlAJIwh1RsNJkppqTnYmVknB0RZ3p+y7J8+fU005/x8h4h8jEebP4QkGAwEB";

			final long decryptionKey = 98572098420l;
			PublicKey key;
			try {
				key = Security.generatePublicKey(decryptKey(encryptedPublicKey2, decryptionKey));
//				key = Security.generatePublicKey(publicKey);
				verified = Security.verify(key, signedData, signature);
			} catch (Exception e) {
				verified = false;
			}
//			if(!verified) {
//				try {
//					key = Security.generatePublicKey(decryptKey(encryptedPublicKey, decryptionKey));
//					verified = Security.verify(key, signedData, signature);
//				} catch (Exception e) {
//				}
//			}
			if (!verified) {
				Log.w(TAG, "signature does not match data.");
				return null;
			}
		}

		JSONObject jObject;
		JSONArray jTransactionsArray = null;
		int numTransactions = 0;
		long nonce = 0L;
		try {
			jObject = new JSONObject(signedData);

			// The nonce might be null if the user backed out of the buy page.
			nonce = jObject.optLong("nonce");
			jTransactionsArray = jObject.optJSONArray("orders");
			if (jTransactionsArray != null) {
				numTransactions = jTransactionsArray.length();
			}
		} catch (JSONException e) {
			return null;
		}

		if (!Security.isNonceKnown(nonce)) {
			Log.w(TAG, "Nonce not found: " + nonce);
			return null;
		}

		ArrayList<VerifiedPurchase> purchases = new ArrayList<VerifiedPurchase>();
		try {
			for (int i = 0; i < numTransactions; i++) {
				JSONObject jElement = jTransactionsArray.getJSONObject(i);
				int response = jElement.getInt("purchaseState");
				PurchaseState purchaseState = PurchaseState.valueOf(response);
				String productId = jElement.getString("productId");
				// String packageName = jElement.getString("packageName");
				long purchaseTime = jElement.getLong("purchaseTime");
				String orderId = jElement.optString("orderId", "");
				String notifyId = null;
				if (jElement.has("notificationId")) {
					notifyId = jElement.getString("notificationId");
				}
				String developerPayload = jElement.optString("developerPayload", null);

				// If the purchase state is PURCHASED, then we require a
				// verified nonce.
				if (purchaseState == PurchaseState.PURCHASED && !verified) {
					continue;
				}
				purchases.add(new VerifiedPurchase(purchaseState, notifyId, productId, orderId,
						purchaseTime, developerPayload));
			}
		} catch (JSONException e) {
			Log.e(TAG, "JSON exception: ", e);
			return null;
		}
		removeNonce(nonce);
		return purchases;
	}

	/**
	 * Generates a PublicKey instance from a string containing the
	 * Base64-encoded public key.
	 * 
	 * @param encodedPublicKey
	 *            Base64-encoded public key
	 * @throws IllegalArgumentException
	 *             if encodedPublicKey is invalid
	 */
	public static PublicKey generatePublicKey(String encodedPublicKey) {
		try {
			byte[] decodedKey = Base64.decode(encodedPublicKey);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
			return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (InvalidKeySpecException e) {
			Log.e(TAG, "Invalid key specification.");
			throw new IllegalArgumentException(e);
		} catch (Base64DecoderException e) {
			Log.e(TAG, "Base64 decoding failed.");
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Verifies that the signature from the server matches the computed
	 * signature on the data. Returns true if the data is correctly signed.
	 * 
	 * @param publicKey
	 *            public key associated with the developer account
	 * @param signedData
	 *            signed data from server
	 * @param signature
	 *            server signature
	 * @return true if the data and signature match
	 */
	public static boolean verify(PublicKey publicKey, String signedData, String signature) {
		Log.v(TAG, "signature: " + signature);
		Signature sig;
		try {
			sig = Signature.getInstance(SIGNATURE_ALGORITHM);
			sig.initVerify(publicKey);
			sig.update(signedData.getBytes());
			if (!sig.verify(Base64.decode(signature))) {
				Log.e(TAG, "Signature verification failed.");
				return false;
			}
			return true;
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "NoSuchAlgorithmException.");
		} catch (InvalidKeyException e) {
			Log.e(TAG, "Invalid key specification.");
		} catch (SignatureException e) {
			Log.e(TAG, "Signature exception.");
		} catch (Base64DecoderException e) {
			Log.e(TAG, "Base64 decoding failed.");
		}
		return false;
	}

	private static String decryptKey(String encryptedKey, long scramble)
			throws Base64DecoderException {
		byte[] encryptedBytes = Base64.decode(encryptedKey);

		byte[] keyBytes = new byte[encryptedBytes.length];

		int lengthBytes = encryptedBytes.length;
		for (int i = 0; i < lengthBytes; i++) {
			byte temp = (byte) (scramble >> (i % Long.SIZE));
			keyBytes[i] = (byte) (encryptedBytes[i] ^ temp);
		}

		return Base64.encode(keyBytes);
	}

	public static String encryptKey(String key, long scramble) throws Base64DecoderException {

		byte[] keyBytes = Base64.decode(key);

		byte[] encryptedBytes = new byte[keyBytes.length];

		int lengthBytes = keyBytes.length;
		for (int i = 0; i < lengthBytes; i++) {
			byte temp = (byte) (scramble >> (i % Long.SIZE));
			encryptedBytes[i] = (byte) (keyBytes[i] ^ temp);
		}

		return Base64.encode(encryptedBytes);
	} 
}
