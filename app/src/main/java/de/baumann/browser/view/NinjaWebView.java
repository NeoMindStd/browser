package de.baumann.browser.view;

import static android.content.ContentValues.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.baumann.browser.R;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.browser.NinjaWebChromeClient;
import de.baumann.browser.browser.NinjaWebViewClient;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;

public class NinjaWebView extends WebView implements AlbumController {

	private static BrowserController browserController = null;
	public boolean fingerPrintProtection;
	public boolean isBackPressed;
	private OnScrollChangeListener onScrollChangeListener;
	private Context context;
	private boolean desktopMode;
	private boolean stopped;
	private AdapterTabs album;
	private AlbumController predecessor = null;
	private NinjaWebViewClient webViewClient;
	private NinjaWebChromeClient webChromeClient;
	private String profile;
	private SharedPreferences sp;
	private boolean foreground;

	public NinjaWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NinjaWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public NinjaWebView(Context context) {
		super(context);
		sp = PreferenceManager.getDefaultSharedPreferences(context);
		String profile = sp.getString("profile", "standard");
		this.context = context;
		this.foreground = false;
		this.desktopMode = false;
		this.isBackPressed = false;
		this.fingerPrintProtection = sp.getBoolean(profile + "_fingerPrintProtection", true);

		this.stopped = false;
		this.album = new AdapterTabs(this.context, this, browserController);
		this.webViewClient = new NinjaWebViewClient(this) {
			@Override
			public void onReceivedError(WebView webview, WebResourceRequest request, WebResourceError error) {
				Context context = webview.getContext();
				String description = error.getDescription().toString();
				String failingUrl = request.getUrl().toString();
				String urlToLoad = sp.getString("urlToLoad", "");
				String htmlData = getErrorHTML(context, description, urlToLoad);
				if (failingUrl.contains(urlToLoad)) {
					webview.loadDataWithBaseURL(urlToLoad, htmlData, "text/html", "UTF-8", urlToLoad);
					webview.invalidate();
				}
			}
		};
		this.webChromeClient = new NinjaWebChromeClient(this);

		initWebView();
		initAlbum();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static String getErrorHTML(Context context, String description, String failingUrl) {
		int primary = MaterialColors.getColor(context, R.attr.colorPrimary, Color.GREEN);
		int background = MaterialColors.getColor(context, android.R.attr.colorBackground, Color.BLACK);
		String primaryHex = String.format("#%06X", (0xFFFFFF & primary));
		String backgroundHex = String.format("#%06X", (0xFFFFFF & background));
		String errorSvgPath = "";
		try {
			InputStream inputStream = context.getResources().openRawResource(R.raw.error);
			byte[] b = new byte[inputStream.available()];
			inputStream.read(b);
			errorSvgPath = new String(b);
		} catch (Exception ignored) {
		}

		String s = context.getString(R.string.app_error) + ": " + failingUrl;
		return "<html><body>" +
			errorSvgPath +
			"<div align=\"center\">" +
			description +
			"<hr style=\"height: 1rem; visibility:hidden;\" />" +
			s +
			"\n</div>" +
			"<a href=\"" + failingUrl + "\">" + context.getString(R.string.menu_reload) + "</a>" +
			"</body></html>" +
			"<style>" +
			"html { background: " + backgroundHex + ";" + "color: " + primaryHex + "; }" +
			"body { min-height: 100vh; display: flex; flex-direction: column; justify-content: center; align-items: center }"
			+
			"svg { transform: scale(3); margin-bottom: 4rem; fill: " + primaryHex + "; }" +
			"a { margin-top: 1rem; text-decoration: none; padding: 0.7rem 1rem; border-radius: 1rem; background: "
			+ primaryHex + ";" + "color: " + backgroundHex + "; }" +
			"p { line-height: 150%; }" +
			"</style>";
	}

	public static BrowserController getBrowserController() {
		return browserController;
	}

	public void setBrowserController(BrowserController browserController) {
		NinjaWebView.browserController = browserController;
		this.album.setBrowserController(browserController);
	}

	@Override
	public void onScrollChanged(int l, int t, int old_l, int old_t) {
		super.onScrollChanged(l, t, old_l, old_t);
		if (onScrollChangeListener != null)
			onScrollChangeListener.onScrollChange(t, old_t);
	}

	public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
		this.onScrollChangeListener = onScrollChangeListener;
	}

	public void setIsBackPressed(Boolean isBackPressed) {
		this.isBackPressed = isBackPressed;
	}

	public boolean isForeground() {
		return foreground;
	}

	private synchronized void initWebView() {
		setWebViewClient(webViewClient);
		setWebChromeClient(webChromeClient);
	}

	@SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
	@TargetApi(Build.VERSION_CODES.O)
	public synchronized void initPreferences(String url) {

		sp = PreferenceManager.getDefaultSharedPreferences(context);
		profile = sp.getString("profile", "profileStandard");
		String profileOriginal = profile;
		WebSettings webSettings = getSettings();

		int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		if ((nightModeFlags == Configuration.UI_MODE_NIGHT_YES) || sp.getString("sp_theme", "1").equals("3")) {
			if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
				boolean allowed = sp.getBoolean("setAlgorithmicDarkeningAllowed", true);
				if (!allowed) {
					WebSettingsCompat.setAlgorithmicDarkeningAllowed(webSettings, false);
					sp.edit().putBoolean("setAlgorithmicDarkeningAllowed", false).apply();
				} else {
					WebSettingsCompat.setAlgorithmicDarkeningAllowed(webSettings, true);
					sp.edit().putBoolean("setAlgorithmicDarkeningAllowed", true).apply();
				}
			}
		}

		String userAgent = getUserAgent(desktopMode);
		webSettings.setUserAgentString(userAgent);
		if (android.os.Build.VERSION.SDK_INT >= 26)
			webSettings.setSafeBrowsingEnabled(true);
		webSettings.setSupportZoom(true);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setDisplayZoomControls(false);
		webSettings.setSupportMultipleWindows(true);
		webSettings.setTextZoom(Integer.parseInt(Objects.requireNonNull(sp.getString("sp_fontSize", "100"))));

		if (sp.getBoolean("sp_autofill", true)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				this.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
			else
				webSettings.setSaveFormData(true);
		} else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				this.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
			else
				webSettings.setSaveFormData(false);
		}

		webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
		webSettings.setMediaPlaybackRequiresUserGesture(sp.getBoolean(profile + "_saveData", true));
		webSettings.setBlockNetworkImage(!sp.getBoolean(profile + "_images", true));
		webSettings.setJavaScriptEnabled(sp.getBoolean(profile + "_javascript", true));
		webSettings.setJavaScriptCanOpenWindowsAutomatically(sp.getBoolean(profile + "_javascriptPopUp", false));
		webSettings.setDomStorageEnabled(sp.getBoolean(profile + "_dom", false));

		webSettings.setAllowFileAccess(true);
		webSettings.setAllowFileAccessFromFileURLs(true);
		webSettings.setAllowUniversalAccessFromFileURLs(true);

		fingerPrintProtection = sp.getBoolean(profile + "_fingerPrintProtection", true);

		CookieManager manager = CookieManager.getInstance();
		if (sp.getBoolean(profile + "_cookies", false)) {
			manager.setAcceptCookie(true);
			manager.getCookie(url);
		} else
			manager.setAcceptCookie(false);

		profile = profileOriginal;
	}

	public void setProfileDefaultValues() {
		sp.edit()
			.putBoolean("profileTrusted_saveData", true)
			.putBoolean("profileTrusted_images", true)
			.putBoolean("profileTrusted_fingerPrintProtection", false)
			.putBoolean("profileTrusted_cookies", true)
			.putBoolean("profileTrusted_javascript", true)
			.putBoolean("profileTrusted_javascriptPopUp", true)
			.putBoolean("profileTrusted_saveHistory", true)
			.putBoolean("profileTrusted_dom", true)

			.putBoolean("profileStandard_saveData", true)
			.putBoolean("profileStandard_images", true)
			.putBoolean("profileStandard_fingerPrintProtection", true)
			.putBoolean("profileStandard_cookies", false)
			.putBoolean("profileStandard_javascript", true)
			.putBoolean("profileStandard_javascriptPopUp", false)
			.putBoolean("profileStandard_saveHistory", true)
			.putBoolean("profileStandard_dom", false)

			.putBoolean("profileProtected_saveData", true)
			.putBoolean("profileProtected_images", true)
			.putBoolean("profileProtected_fingerPrintProtection", true)
			.putBoolean("profileProtected_cookies", false)
			.putBoolean("profileProtected_javascript", false)
			.putBoolean("profileProtected_javascriptPopUp", false)
			.putBoolean("profileProtected_saveHistory", true)
			.putBoolean("profileProtected_dom", false).apply();
	}

	private synchronized void initAlbum() {
		album.setBrowserController(browserController);
	}

	public synchronized HashMap<String, String> getRequestHeaders() {
		HashMap<String, String> requestHeaders = new HashMap<>();
		requestHeaders.put("DNT", "1");
		//  Server-side detection for GlobalPrivacyControl
		requestHeaders.put("Sec-GPC", "1");
		requestHeaders.put("X-Requested-With", "com.duckduckgo.mobile.android");

		profile = sp.getString("profile", "profileStandard");
		if (sp.getBoolean(profile + "_saveData", false))
			requestHeaders.put("Save-Data", "on");
		return requestHeaders;
	}

	@Override
	public synchronized void stopLoading() {
		stopped = true;
		super.stopLoading();
	}

	@Override
	public synchronized void reload() {
		stopped = false;
		this.initPreferences(this.getUrl());
		try {
			this.loadUrl(Objects.requireNonNull(this.getUrl()));
			super.reload();
		} catch (Exception e) {
			Log.i(TAG, "shouldOverrideUrlLoading Exception:" + e);
			NinjaToast.show(context, R.string.app_error);
		}
	}

	@Override
	public synchronized void loadUrl(@NonNull String url) {
		InputMethodManager imm = (InputMethodManager)this.context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
		stopped = false;

		if (url.startsWith("http://")) {

			GridItem item_01 = new GridItem("https://", R.drawable.icon_https);
			GridItem item_02 = new GridItem("http://", R.drawable.icon_http);
			GridItem item_03 = new GridItem(context.getString(R.string.app_cancel), R.drawable.icon_close);

			View dialogView = View.inflate(context, R.layout.dialog_menu, null);
			MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);

			LinearLayout textGroup = dialogView.findViewById(R.id.textGroup);
			TextView menuURL = dialogView.findViewById(R.id.menuURL);
			menuURL.setText(url);
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
			menuTitle.setText(HelperUnit.domain(url));
			TextView message = dialogView.findViewById(R.id.message);
			message.setVisibility(View.VISIBLE);
			message.setText(R.string.toast_unsecured);
			builder.setView(dialogView);

			AlertDialog dialog = builder.create();
			dialog.show();
			HelperUnit.setupDialog(context, dialog);

			GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
			final List<GridItem> gridList = new LinkedList<>();
			gridList.add(gridList.size(), item_01);
			gridList.add(gridList.size(), item_02);
			gridList.add(gridList.size(), item_03);
			GridAdapter gridAdapter = new GridAdapter(context, gridList);
			menu_grid.setAdapter(gridAdapter);
			gridAdapter.notifyDataSetChanged();
			menu_grid.setOnItemClickListener((parent, view, position, id) -> {
				switch (position) {
					case 0:
						dialog.cancel();
						String finalURL = url.replace("http://", "https://");
						sp.edit().putString("urlToLoad", finalURL).apply();
						initPreferences(BrowserUnit.queryWrapper(context, finalURL));
						super.loadUrl(BrowserUnit.queryWrapper(context, finalURL), getRequestHeaders());
						break;
					case 1:
						dialog.cancel();
						sp.edit().putString("urlToLoad", url).apply();
						initPreferences(BrowserUnit.queryWrapper(context, url));
						super.loadUrl(BrowserUnit.queryWrapper(context, url), getRequestHeaders());
						break;
					case 2:
						dialog.cancel();
						super.loadUrl(BrowserUnit.queryWrapper(context, "about:blank"), getRequestHeaders());
						break;
				}
			});
		} else {
			sp.edit().putString("urlToLoad", url).apply();
			initPreferences(BrowserUnit.queryWrapper(context, url));
			super.loadUrl(BrowserUnit.queryWrapper(context, url), getRequestHeaders());
		}

	}

	@Override
	public View getAlbumView() {
		return album.getAlbumView();
	}

	public void setAlbumTitle(String title, String url) {
		album.setAlbumTitle(title, url);
	}

	@Override
	public synchronized void activate() {
		requestFocus();
		foreground = true;
		album.activate();
	}

	@Override
	public synchronized void deactivate() {
		clearFocus();
		foreground = false;
		album.deactivate();
	}

	public synchronized void updateTitle(int progress) {
		if (foreground && !stopped)
			browserController.updateProgress(progress);
		else if (foreground)
			browserController.updateProgress(BrowserUnit.LOADING_STOPPED);
	}

	public synchronized void updateTitle(String title, String url) {
		album.setAlbumTitle(title, url);
	}

	@Override
	public synchronized void destroy() {
		stopLoading();
		onPause();
		clearHistory();
		setVisibility(GONE);
		removeAllViews();
		super.destroy();
	}

	public boolean isDesktopMode() {
		return desktopMode;
	}

	public boolean isFingerPrintProtection() {
		return fingerPrintProtection;
	}

	public String getUserAgent(boolean desktopMode) {
		String mobilePrefix = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + ")";
		String desktopPrefix = "Mozilla/5.0 (X11; Linux " + System.getProperty("os.arch") + ")";

		String newUserAgent = WebSettings.getDefaultUserAgent(context);
		String prefix = newUserAgent.substring(0, newUserAgent.indexOf(")") + 1);

		if (desktopMode) {
			try {
				newUserAgent = newUserAgent.replace(prefix, desktopPrefix);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				newUserAgent = newUserAgent.replace(prefix, mobilePrefix);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//Override UserAgent if own UserAgent is defined
		if (!sp.contains(
			"userAgentSwitch")) {  //if new switch_text_preference has never been used initialize the switch
			if (Objects.requireNonNull(sp.getString("sp_userAgent", "")).equals("")) {
				sp.edit().putBoolean("userAgentSwitch", false).apply();
			} else
				sp.edit().putBoolean("userAgentSwitch", true).apply();
		}

		String ownUserAgent = sp.getString("sp_userAgent", "");
		if (!ownUserAgent.equals("") && (sp.getBoolean("userAgentSwitch", false)))
			newUserAgent = ownUserAgent;
		return newUserAgent;
	}

	public void toggleDesktopMode(boolean reload) {
		desktopMode = !desktopMode;
		String newUserAgent = getUserAgent(desktopMode);
		getSettings().setUserAgentString(newUserAgent);
		getSettings().setUseWideViewPort(desktopMode);
		getSettings().setLoadWithOverviewMode(desktopMode);
		if (reload)
			reload();
	}

	public void toggleNightMode() {
		if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
			WebSettings s = this.getSettings();
			boolean allowed = sp.getBoolean("setAlgorithmicDarkeningAllowed", true);
			if (allowed) {
				WebSettingsCompat.setAlgorithmicDarkeningAllowed(s, false);
				sp.edit().putBoolean("setAlgorithmicDarkeningAllowed", false).apply();
			} else {
				WebSettingsCompat.setAlgorithmicDarkeningAllowed(s, true);
				sp.edit().putBoolean("setAlgorithmicDarkeningAllowed", true).apply();
			}
		}
	}

	public void setStopped(boolean stopped) {
		this.stopped = stopped;
	}

	public String getProfile() {
		return profile;
	}

	public AlbumController getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(AlbumController predecessor) {
		this.predecessor = predecessor;
	}

	public interface OnScrollChangeListener {
		/**
		 * Called when the scroll position of a view changes.
		 *
		 * @param scrollY    Current vertical scroll origin.
		 * @param oldScrollY Previous vertical scroll origin.
		 */
		void onScrollChange(int scrollY, int oldScrollY);
	}
}
