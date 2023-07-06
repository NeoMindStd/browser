package de.baumann.browser.browser;

import java.util.Objects;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.baumann.browser.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaWebView;

public class NinjaWebChromeClient extends WebChromeClient {

	private final NinjaWebView ninjaWebView;

	public NinjaWebChromeClient(NinjaWebView ninjaWebView) {
		super();
		this.ninjaWebView = ninjaWebView;
	}

	@Override
	public void onProgressChanged(WebView view, int progress) {
		super.onProgressChanged(view, progress);
		ninjaWebView.updateTitle(progress);
		ninjaWebView.updateFavicon(view.getUrl());
		if (Objects.requireNonNull(view.getTitle()).isEmpty())
			ninjaWebView.updateTitle(view.getUrl(), view.getUrl());
		else
			ninjaWebView.updateTitle(view.getTitle(), view.getUrl());
	}

	@Override
	public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg) {
		Context context = view.getContext();
		NinjaWebView newWebView = new NinjaWebView(context);
		view.addView(newWebView);
		WebView.WebViewTransport transport = (WebView.WebViewTransport)resultMsg.obj;
		transport.setWebView(newWebView);
		resultMsg.sendToTarget();
		newWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				BrowserUnit.intentURL(context, request.getUrl());
				return true;
			}
		});
		return true;
	}

	@Override
	public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
		NinjaWebView.getBrowserController().onShowCustomView(view, callback);
		super.onShowCustomView(view, callback);
	}

	@Override
	public void onHideCustomView() {
		NinjaWebView.getBrowserController().onHideCustomView();
		super.onHideCustomView();
	}

	@Override
	public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
		WebChromeClient.FileChooserParams fileChooserParams) {
		NinjaWebView.getBrowserController().showFileChooser(filePathCallback);
		return true;
	}

	@Override
	public void onReceivedIcon(WebView view, Bitmap icon) {
		ninjaWebView.setFavicon(icon);
		super.onReceivedIcon(view, icon);
	}

	@Override
	public void onReceivedTitle(WebView view, String sTitle) {
		super.onReceivedTitle(view, sTitle);
	}
}
