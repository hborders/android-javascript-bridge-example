package com.github.hborders.mobiletwitter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MobileTwitterApplication extends Application {
	public static final String LOGIN_ACTION = MobileTwitterApplication.class
			.getName() + ".broadcast.Login";
	public static final String TWEETS_LIST_ACTION = MobileTwitterApplication.class
			.getName() + ".broadcast.TweetsList";

	private static final String TAG = MobileTwitterApplication.class.getName()
			+ ".tag";

	private static final String MAYBE_TWEETS_LIST_URL = "https://mobile.twitter.com/";
	private static final String LOGIN_URL = "https://mobile.twitter.com/session/new";
	private static final String LOGOUT_URL = "https://mobile.twitter.com/session/destroy";

	private final Map<Integer, BlockingQueue<String>> callIdsToJavascriptReturnValueBlockingQueues = new ConcurrentHashMap<Integer, BlockingQueue<String>>();
	private final AtomicInteger callIdAtomicInteger = new AtomicInteger(1);

	private WebView webView;
	private MobileTwitterWebViewClient mobileTwitterWebViewClient;

	public MobileTwitterApplication() {
	}

	public static MobileTwitterApplication getMobileTwitterApplication(
			Activity activity) {
		return (MobileTwitterApplication) activity.getApplication();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();

		if (webView != null) {
			webView.freeMemory();
		}
	}

	private WebView getWebView() {
		if (webView == null) {
			webView = new WebView(this);
			mobileTwitterWebViewClient = new MobileTwitterWebViewClient();
			webView.getSettings().setJavaScriptEnabled(true);
			webView.getSettings().setSavePassword(false);
			webView.setWebViewClient(mobileTwitterWebViewClient);
			webView.setWebChromeClient(new WebChromeClient() {
				@Override
				public void onConsoleMessage(String message, int lineNumber,
						String sourceID) {
					Log.v(TAG, "ConsoleMessage:" + message);
				}
			});

			webView.addJavascriptInterface(new Object() {
				@SuppressWarnings("unused")
				public void returnStringValueForCallId(String value, int callId) {
					BlockingQueue<String> javascriptReturnValueBlockingQueue = callIdsToJavascriptReturnValueBlockingQueues
							.get(callId);
					if (javascriptReturnValueBlockingQueue != null) {
						javascriptReturnValueBlockingQueue.add(value);
					}
				}

				@SuppressWarnings("unused")
				public String javascriptFromAsset(String asset) {
					String javascript = null;
					try {
						InputStream inputStream = getAssets().open(asset,
								AssetManager.ACCESS_STREAMING);
						InputStreamReader inputStreamReader = new InputStreamReader(
								inputStream);
						char[] buffer = new char[1024];
						StringWriter stringWriter = new StringWriter(1024);
						for (int charsRead = inputStreamReader.read(buffer); charsRead != -1; charsRead = inputStreamReader
								.read(buffer)) {
							stringWriter.write(buffer, 0, charsRead);
						}
						javascript = stringWriter.toString();
						inputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return javascript;
				}
			}, "mobileTwitterJavaApi");
		}

		return webView;
	}

	private MobileTwitterWebViewClient getMobileTwitterWebViewClient() {
		if (mobileTwitterWebViewClient == null) {
			getWebView();
		}
		return mobileTwitterWebViewClient;
	}

	public void load() {
		getMobileTwitterWebViewClient().loadUrl(MAYBE_TWEETS_LIST_URL);
	}

	public void login(String username, String password) {
		executeAsynchronousJavascriptOnWebView("mobiletwitter.login(\""
				+ username + "\", \"" + password + "\")");
	}

	public void logout() {
		getWebView().loadUrl(LOGOUT_URL);
		executeAsynchronousJavascriptOnWebView("mobiletwitter.logout()");
	}

	public List<Tweet> extractTweets() {
		String tweetsJson = executeSynchronousJavascriptOnWebView("mobiletwitter.tweetsJson()");
		List<Tweet> tweets;
		try {
			JSONArray tweetJsonArray = (JSONArray) new JSONTokener(tweetsJson)
					.nextValue();
			tweets = new ArrayList<Tweet>(tweetJsonArray.length());
			for (int i = 0, l = tweetJsonArray.length(); i < l; i++) {
				JSONObject tweetJsonObject = tweetJsonArray.getJSONObject(i);
				tweets.add(new Tweet(tweetJsonObject
						.getString("userscreenname"), tweetJsonObject
						.getString("text")));
			}
		} catch (JSONException e) {
			e.printStackTrace();
			tweets = Collections.emptyList();
		}

		return tweets;
	}

	private void executeAsynchronousJavascriptOnWebView(String javascript) {
		String javascriptUrl = "javascript:" + javascript + ";";
		getWebView().loadUrl(javascriptUrl);
	}

	private String executeSynchronousJavascriptOnWebView(String javascript) {
		int callId = callIdAtomicInteger.incrementAndGet();
		BlockingQueue<String> javascriptReturnValueBlockingQueue = new LinkedBlockingQueue<String>(
				1);
		callIdsToJavascriptReturnValueBlockingQueues.put(callId,
				javascriptReturnValueBlockingQueue);

		String javascriptUrl = "javascript:mobileTwitterJavaApi.returnStringValueForCallId("
				+ javascript + ", " + callId + ")";
		getWebView().loadUrl(javascriptUrl);

		String javascriptReturnValue = null;
		try {
			javascriptReturnValue = javascriptReturnValueBlockingQueue.poll(3,
					TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			callIdsToJavascriptReturnValueBlockingQueues.remove(callId);
		}

		return javascriptReturnValue;
	}

	private void loadJavascriptFromAsset(String asset) {
		String javascript = "(function() { var javascript = mobileTwitterJavaApi.javascriptFromAsset(\""
				+ asset + "\");  eval(javascript + \"\"); })()";
		executeSynchronousJavascriptOnWebView(javascript);
	}

	private void startActivity(Class<? extends Activity> activityClass) {
		Intent intent = new Intent(MobileTwitterApplication.this, activityClass);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	private class MobileTwitterWebViewClient extends WebViewClient {
		private String redirectUrl;
		private boolean expectOnPageFinished;

		public void loadUrl(String url) {
			getWebView().loadUrl(url);
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			if (expectOnPageFinished) {
				redirectUrl = url;
			}
			expectOnPageFinished = true;
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView webView, String url) {
			if (!url.equals(redirectUrl)) {
				redirectUrl = null;
			}
			loadUrl(url);
			return true;
		}

		@Override
		public void onPageFinished(WebView webView, String url) {
			if ((redirectUrl == null) || !url.equals(redirectUrl)) {
				executeSynchronousJavascriptOnWebView("(function(){ window.jsxpath = { useNative: false }; })()");
				loadJavascriptFromAsset("javascript-xpath-latest.js");
				loadJavascriptFromAsset("mobiletwitter.js");

				if (MAYBE_TWEETS_LIST_URL.equals(url)) {
					if (Boolean
							.valueOf(executeSynchronousJavascriptOnWebView("mobiletwitter.isLocatedOnTweetsList()"))) {
						sendOrderedBroadcast(new Intent(TWEETS_LIST_ACTION),
								null, new BroadcastReceiver() {
									@Override
									public void onReceive(Context context,
											Intent intent) {
										startActivity(TweetsListActivity.class);
									}
								}, null, Activity.RESULT_OK, null, null);
					} else {
						getWebView().loadUrl(LOGIN_URL);
					}
				} else if (LOGIN_URL.equals(url)) {
					sendOrderedBroadcast(new Intent(LOGIN_ACTION), null,
							new BroadcastReceiver() {

								@Override
								public void onReceive(Context context,
										Intent intent) {
									startActivity(LoginActivity.class);
								}
							}, null, Activity.RESULT_OK, null, null);
				}
			}
			expectOnPageFinished = false;
			redirectUrl = null;
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			Log.v(TAG, "onReceivedError: " + description);
		}
	}
}
