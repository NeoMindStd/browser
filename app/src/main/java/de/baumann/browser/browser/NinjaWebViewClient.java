package de.baumann.browser.browser;

import static androidx.constraintlayout.motion.utils.Oscillator.*;

import java.util.Objects;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import de.baumann.browser.R;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaWebView;

public class NinjaWebViewClient extends WebViewClient {

	private final NinjaWebView ninjaWebView;
	private final Context context;
	private final SharedPreferences sp;

	public NinjaWebViewClient(NinjaWebView ninjaWebView) {
		super();
		this.ninjaWebView = ninjaWebView;
		this.context = ninjaWebView.getContext();
		this.sp = PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		super.onPageFinished(view, url);
		ninjaWebView.isBackPressed = false;

		if (ninjaWebView.isForeground())
			ninjaWebView.invalidate();
		else
			ninjaWebView.postInvalidate();

		if (sp.getBoolean("onPageFinished", false))
			view.evaluateJavascript(Objects.requireNonNull(sp.getString("sp_onPageFinished", "")), null);
	}

	@Override
	public void onReceivedError(WebView webview, WebResourceRequest request, WebResourceError error) {
		Context context = webview.getContext();
		String description = error.getDescription().toString();
		String failingUrl = request.getUrl().toString();
		String urlToLoad = sp.getString("urlToLoad", "");
		String htmlData = NinjaWebView.getErrorHTML(context, description, urlToLoad);
		if (urlToLoad.equals(failingUrl)) {
			webview.loadDataWithBaseURL(null, htmlData, "", "", failingUrl);
			webview.invalidate();
		}
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {

		ninjaWebView.setStopped(false);

		super.onPageStarted(view, url, favicon);

		if (sp.getBoolean("onPageStarted", false))
			view.evaluateJavascript(Objects.requireNonNull(sp.getString("sp_onPageStarted", "")), null);
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
		final Uri uri = request.getUrl();
		String url = uri.toString();

		if (ninjaWebView.isBackPressed)
			return false;
		else {
			// handle the url by implementing your logic
			if (url.startsWith("http://") || url.startsWith("https://")) {
				this.ninjaWebView.initPreferences(url);
				ninjaWebView.loadUrl(url);
				return false;
			} else {
				try {
					Intent intent;
					if (url.startsWith("intent:")) {
						intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
						intent.addCategory("android.intent.category.BROWSABLE");
						intent.setComponent(null);
						intent.setSelector(null);
					} else {
						intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					}
					view.getContext().startActivity(intent);
					return true;
				} catch (Exception e) {
					Log.i(TAG, "shouldOverrideUrlLoading Exception:" + e);
					return true;
				}
			}
		}
	}

	@Override
	public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
		return super.shouldInterceptRequest(view, request);
	}

	@Override
	public void onFormResubmission(WebView view, @NonNull final Message doNotResend, final Message resend) {

		View dialogView = View.inflate(context, R.layout.dialog_menu, null);
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);

		LinearLayout textGroup = dialogView.findViewById(R.id.textGroup);
		TextView menuURL = dialogView.findViewById(R.id.menuURL);
		menuURL.setText(view.getUrl());
		menuURL.setEllipsize(TextUtils.TruncateAt.MARQUEE);
		menuURL.setSingleLine(true);
		menuURL.setMarqueeRepeatLimit(1);
		menuURL.setSelected(true);
		textGroup.setOnClickListener(v -> {
			menuURL.setEllipsize(TextUtils.TruncateAt.MARQUEE);
			menuURL.setSingleLine(true);
			menuURL.setMarqueeRepeatLimit(1);
			menuURL.setSelected(true);
		});
		TextView menuTitle = dialogView.findViewById(R.id.menuTitle);
		menuTitle.setText(HelperUnit.domain(view.getUrl()));
		TextView messageView = dialogView.findViewById(R.id.message);
		messageView.setVisibility(View.VISIBLE);
		messageView.setText(R.string.dialog_content_resubmission);
		builder.setView(dialogView);
		builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> resend.sendToTarget());
		builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> doNotResend.sendToTarget());
		AlertDialog dialog = builder.create();
		dialog.show();
		dialog.setOnCancelListener(d -> doNotResend.sendToTarget());
		HelperUnit.setupDialog(context, dialog);
	}

	@SuppressLint("WebViewClientOnReceivedSslError")
	@Override
	public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
		handler.proceed();
	}

	@Override
	public void onReceivedHttpAuthRequest(WebView view, @NonNull final HttpAuthHandler handler, String host,
		String realm) {

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
		View dialogView = View.inflate(context, R.layout.dialog_edit, null);

		TextInputLayout editTopLayout = dialogView.findViewById(R.id.editTopLayout);
		editTopLayout.setHint(this.context.getString(R.string.dialog_sign_in_username));
		TextInputLayout editBottomLayout = dialogView.findViewById(R.id.editBottomLayout);
		editBottomLayout.setHint(this.context.getString(R.string.dialog_sign_in_password));
		TextInputEditText editTop = dialogView.findViewById(R.id.editTop);
		TextInputEditText editBottom = dialogView.findViewById(R.id.editBottom);
		editTop.setText("");
		editTop.setHint(this.context.getString(R.string.dialog_sign_in_username));
		editBottom.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		editBottom.setText("");
		editBottom.setHint(this.context.getString(R.string.dialog_sign_in_password));

		builder.setView(dialogView);

		LinearLayout textGroupEdit = dialogView.findViewById(R.id.textGroupEdit);
		TextView menuURLEdit = dialogView.findViewById(R.id.menuURLEdit);
		menuURLEdit.setText(view.getUrl());
		menuURLEdit.setEllipsize(TextUtils.TruncateAt.MARQUEE);
		menuURLEdit.setSingleLine(true);
		menuURLEdit.setMarqueeRepeatLimit(1);
		menuURLEdit.setSelected(true);
		textGroupEdit.setOnClickListener(v -> {
			menuURLEdit.setEllipsize(TextUtils.TruncateAt.MARQUEE);
			menuURLEdit.setSingleLine(true);
			menuURLEdit.setMarqueeRepeatLimit(1);
			menuURLEdit.setSelected(true);
		});
		TextView menuTitleEdit = dialogView.findViewById(R.id.menuTitleEdit);
		menuTitleEdit.setText(view.getTitle());

		AlertDialog dialog = builder.create();
		dialog.show();
		HelperUnit.setupDialog(context, dialog);
		dialog.setOnCancelListener(dialog1 -> {
			handler.cancel();
			dialog1.cancel();
		});

		Button ib_cancel = dialogView.findViewById(R.id.editCancel);
		ib_cancel.setOnClickListener(v -> {
			HelperUnit.hideSoftKeyboard(editBottom, context);
			dialog.cancel();
		});
		Button ib_ok = dialogView.findViewById(R.id.editOK);
		ib_ok.setOnClickListener(v -> {
			HelperUnit.hideSoftKeyboard(editBottom, context);
			String user = Objects.requireNonNull(editTop.getText()).toString().trim();
			String pass = Objects.requireNonNull(editBottom.getText()).toString().trim();
			handler.proceed(user, pass);
			dialog.cancel();
		});
	}
}
