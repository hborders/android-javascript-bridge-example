package com.github.hborders.mobiletwitter;

public class Tweet {
	public final String userscreenname;
	public final String text;

	public Tweet(String userscreenname, String text) {
		super();
		this.userscreenname = userscreenname;
		this.text = text;
	}

	@Override
	public String toString() {
		return userscreenname + ": " + text;
	}
}
