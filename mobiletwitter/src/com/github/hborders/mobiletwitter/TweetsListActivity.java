package com.github.hborders.mobiletwitter;

import static com.github.hborders.mobiletwitter.MobileTwitterApplication.getMobileTwitterApplication;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class TweetsListActivity extends ListActivity {

	private BroadcastReceiver loginBroadcastReceiver;
	private ArrayAdapter<Tweet> tweetArrayAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		tweetArrayAdapter = new ArrayAdapter<Tweet>(this,
				R.layout.tweets_list_item, new ArrayList<Tweet>());
		setListAdapter(tweetArrayAdapter);

		loginBroadcastReceiver = new ActivityFinishedBroadcastReceiver(this);
	}

	@Override
	protected void onStart() {
		super.onStart();

		registerReceiver(loginBroadcastReceiver, new IntentFilter(
				MobileTwitterApplication.LOGIN_ACTION));

		refreshTweets();
	}

	@Override
	protected void onStop() {
		tweetArrayAdapter.clear();

		unregisterReceiver(loginBroadcastReceiver);

		super.onStop();
	}

	private void refreshTweets() {
		List<Tweet> tweets = getMobileTwitterApplication(this).extractTweets();
		tweetArrayAdapter.clear();
		for (Tweet tweet : tweets) {
			tweetArrayAdapter.add(tweet);
		}
		
		tweetArrayAdapter.notifyDataSetChanged();
	}
}
