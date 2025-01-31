package de.baumann.browser.activity;

import static android.content.ContentValues.*;
import static android.webkit.WebView.HitTestResult.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import de.baumann.browser.R;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserContainer;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.GridAdapter;
import de.baumann.browser.view.GridItem;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;
import de.baumann.browser.view.SwipeTouchListener;

public class BrowserActivity extends AppCompatActivity implements BrowserController {

	// Menus
	private static final int INPUT_FILE_REQUEST_CODE = 1;
	private RelativeLayout omniBox;
	private ImageButton omniBox_overview;
	private int duration;

	// Views
	private TextInputEditText omniBox_text;
	private EditText searchBox;
	private RelativeLayout bottomSheetDialog_OverView;
	private NinjaWebView ninjaWebView;
	private View customView;
	private VideoView videoView;
	private FloatingActionButton omniBox_tab;
	private KeyListener listener;
	private BadgeDrawable badgeDrawable;
	private BadgeDrawable badgeTab;

	// Layouts
	private CircularProgressIndicator progressBar;
	private RelativeLayout searchPanel;
	private FrameLayout contentFrame;
	private LinearLayout tab_container;
	private FrameLayout fullscreenHolder;
	private ListView list_search;

	// Others
	private Button omnibox_close;
	private BottomNavigationView bottom_navigation;
	private BottomAppBar bottomAppBar;
	private String overViewTab;
	private BroadcastReceiver downloadReceiver;
	private Activity activity;
	private Context context;
	private SharedPreferences sp;
	private ObjectAnimator animation;
	private boolean orientationChanged;
	private boolean searchOnSite;
	private ValueCallback<Uri[]> filePathCallback = null;
	private AlbumController currentAlbumController = null;
	private ValueCallback<Uri[]> mFilePathCallback;

	private AlbumController nextAlbumController(boolean next) {
		if (BrowserContainer.size() <= 1)
			return currentAlbumController;
		List<AlbumController> list = BrowserContainer.list();
		int index = list.indexOf(currentAlbumController);
		if (next) {
			index++;
			if (index >= list.size())
				index = 0;
		} else {
			index--;
			if (index < 0)
				index = list.size() - 1;
		}
		return list.get(index);
	}

	@Override
	public void onPause() {
		//Save open Tabs in shared preferences
		saveOpenedTabs();
		super.onPause();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = BrowserActivity.this;
		context = BrowserActivity.this;
		sp = PreferenceManager.getDefaultSharedPreferences(context);
		duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);

		if (getSupportActionBar() != null)
			getSupportActionBar().hide();
		Window window = this.getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		window.setStatusBarColor(ContextCompat.getColor(this, R.color.md_theme_light_onBackground));

		if (sp.getBoolean("sp_screenOn", false))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		HelperUnit.initTheme(activity);

		OrientationEventListener mOrientationListener = new OrientationEventListener(getApplicationContext()) {
			@Override
			public void onOrientationChanged(int orientation) {
				orientationChanged = true;
			}
		};
		if (mOrientationListener.canDetectOrientation())
			mOrientationListener.enable();

		sp.edit()
			.putInt("restart_changed", 0)
			.putString("profile", sp.getString("profile_toStart", "profileStandard")).apply();

		switch (Objects.requireNonNull(sp.getString("start_tab", "3"))) {
			case "3":
				overViewTab = getString(R.string.album_title_bookmarks);
				break;
			case "4":
				overViewTab = getString(R.string.album_title_history);
				break;
			default:
				overViewTab = getString(R.string.album_title_home);
				break;
		}
		setContentView(R.layout.activity_main);
		contentFrame = findViewById(R.id.main_content);

		// Calculate ActionBar height
		TypedValue tv = new TypedValue();
		if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true) && !sp.getBoolean("hideToolbar",
			true)) {
			int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
			contentFrame.setPadding(0, 0, 0, actionBarHeight);
		}

		downloadReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
				builder.setTitle(R.string.menu_download);
				builder.setIcon(R.drawable.icon_download);
				builder.setMessage(R.string.toast_downloadComplete);
				builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> startActivity(
					Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null)));
				builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
				Dialog dialog = builder.create();
				dialog.show();
				HelperUnit.setupDialog(context, dialog);
			}
		};

		IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		registerReceiver(downloadReceiver, filter);

		initOmniBox();
		initSearchPanel();
		initOverview();

		dispatchIntent(getIntent());

		//restore open Tabs from shared preferences if app got killed
		if (sp.getBoolean("sp_restoreTabs", false)
			|| sp.getBoolean("sp_reloadTabs", false)
			|| sp.getBoolean("restoreOnRestart", false)) {
			String saveDefaultProfile = sp.getString("profile", "profileStandard");
			ArrayList<String> openTabs;
			ArrayList<String> openTabsProfile;
			openTabs = new ArrayList<>(Arrays.asList(TextUtils.split(sp.getString("openTabs", ""), "‚‗‚")));
			openTabsProfile =
				new ArrayList<>(Arrays.asList(TextUtils.split(sp.getString("openTabsProfile", ""), "‚‗‚")));
			if (openTabs.size() > 0) {
				for (int counter = 0; counter < openTabs.size(); counter++) {
					addAlbum(getString(R.string.app_name), openTabs.get(counter), BrowserContainer.size() < 1, false,
						openTabsProfile.get(counter), null);
				}
			}
			sp.edit().putString("profile", saveDefaultProfile).apply();
			sp.edit().putBoolean("restoreOnRestart", false).apply();
		}

		//if still no open Tab open default page
		if (BrowserContainer.size() < 1) {
			if (sp.getBoolean("start_tabStart", false))
				showOverview();
			addAlbum(getString(R.string.app_name), "", true, false, "", null);
			ninjaWebView.loadUrl(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser/wiki"));
		}
	}

	// Overrides

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}
		Uri[] results = null;
		// Check that the response is a good one
		if (resultCode == Activity.RESULT_OK) {
			if (data != null) {
				// If there is not data, then we may have taken a photo
				String dataString = data.getDataString();
				if (dataString != null)
					results = new Uri[] {Uri.parse(dataString)};
			}
		}
		mFilePathCallback.onReceiveValue(results);
		mFilePathCallback = null;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (sp.getInt("restart_changed", 1) == 1) {
			saveOpenedTabs();
			HelperUnit.triggerRebirth(context);
		}
		dispatchIntent(getIntent());
	}

	@Override
	public void onDestroy() {

		NotificationManager notificationManager =
			(NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(2);
		notificationManager.cancel(1);

		if (sp.getBoolean("sp_clear_quit", true)) {
			boolean clearCache = sp.getBoolean("sp_clear_cache", true);
			boolean clearCookie = sp.getBoolean("sp_clear_cookie", false);
			boolean clearHistory = sp.getBoolean("sp_clear_history", false);
			boolean clearIndexedDB = sp.getBoolean("sp_clearIndexedDB", true);
			if (clearCache)
				BrowserUnit.clearCache(this);
			if (clearCookie)
				BrowserUnit.clearCookie();
			if (clearIndexedDB) {
				BrowserUnit.clearIndexedDB(this);
				WebStorage.getInstance().deleteAllData();
			}
		}

		BrowserContainer.clear();

		if (!sp.getBoolean("sp_reloadTabs", false) || sp.getInt("restart_changed", 1) == 1) {
			sp.edit().putString("openTabs", "").apply();   //clear open tabs in preferences
			sp.edit().putString("openTabsProfile", "").apply();
		}

		unregisterReceiver(downloadReceiver);
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
				showOverflow();
			case KeyEvent.KEYCODE_BACK:
				if (bottomAppBar.getVisibility() == View.GONE)
					hideOverview();
				else if (fullscreenHolder != null || customView != null || videoView != null)
					Log.v(TAG, "FOSS Browser in fullscreen mode");
				else if (list_search.getVisibility() == View.VISIBLE)
					omniBox_text.clearFocus();
				else if (searchPanel.getVisibility() == View.VISIBLE) {
					searchOnSite = false;
					searchBox.setText("");
					searchPanel.setVisibility(View.GONE);
					omniBox.setVisibility(View.VISIBLE);
				} else if (ninjaWebView.canGoBack()) {
					WebBackForwardList mWebBackForwardList = ninjaWebView.copyBackForwardList();
					String historyUrl =
						mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex() - 1).getUrl();
					goBack_skipRedirects(historyUrl);
				} else
					removeAlbum(currentAlbumController);
				return true;
		}
		return false;
	}

	@Override
	public synchronized void showAlbum(AlbumController controller) {
		View av = (View)controller;
		if (currentAlbumController != null)
			currentAlbumController.deactivate();
		currentAlbumController = controller;
		currentAlbumController.activate();
		contentFrame.removeAllViews();
		contentFrame.addView(av);
		if (searchPanel.getVisibility() == View.VISIBLE) {
			searchOnSite = false;
			searchBox.setText("");
			searchPanel.setVisibility(View.GONE);
			omniBox.setVisibility(View.VISIBLE);
		}
		updateOmniBox();
		if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
			WebSettings s = ninjaWebView.getSettings();
			boolean allowed = sp.getBoolean("setAlgorithmicDarkeningAllowed", true);
			WebSettingsCompat.setAlgorithmicDarkeningAllowed(s, allowed);
		}
	}

	@Override
	public synchronized void removeAlbum(final AlbumController controller) {

		if (BrowserContainer.size() <= 1) {
			if (!sp.getBoolean("sp_reopenLastTab", false))
				doubleTapsQuit();
			else {
				ninjaWebView.loadUrl(
					Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser/wiki")));
				hideOverview();
			}
		} else {
			closeTabConfirmation(() -> {
				AlbumController predecessor;
				if (controller == currentAlbumController)
					predecessor = ((NinjaWebView)controller).getPredecessor();
				else
					predecessor = currentAlbumController;
				//if not the current TAB is being closed return to current TAB
				tab_container.removeView(controller.getAlbumView());
				int index = BrowserContainer.indexOf(controller);
				BrowserContainer.remove(controller);
				if ((predecessor != null) && (BrowserContainer.indexOf(predecessor) != -1)) {
					//if predecessor is stored and has not been closed in the meantime
					showAlbum(predecessor);
				} else {
					if (index >= BrowserContainer.size())
						index = BrowserContainer.size() - 1;
					showAlbum(BrowserContainer.get(index));
				}
			});
		}
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (!orientationChanged) {
			saveOpenedTabs();
			HelperUnit.triggerRebirth(context);
		} else
			orientationChanged = false;
	}

	@Override
	public synchronized void updateProgress(int progress) {
		progressBar.setProgressCompat(progress, true);
		if (progress != BrowserUnit.LOADING_STOPPED) {
			updateOmniBox();
		}
		if (progress < BrowserUnit.PROGRESS_MAX)
			progressBar.setVisibility(View.VISIBLE);
	}

	@Override
	public void showFileChooser(ValueCallback<Uri[]> filePathCallback) {
		if (mFilePathCallback != null)
			mFilePathCallback.onReceiveValue(null);
		mFilePathCallback = filePathCallback;

		Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
		contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
		contentSelectionIntent.setType("*/*");
		Intent[] intentArray;
		intentArray = new Intent[0];

		Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
		chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
		chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
		//noinspection deprecation
		startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
	}

	@Override
	public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
		if (view == null)
			return;
		if (customView != null && callback != null) {
			callback.onCustomViewHidden();
			return;
		}

		customView = view;
		fullscreenHolder = new FrameLayout(context);
		fullscreenHolder.addView(
			customView,
			new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.MATCH_PARENT
			));

		FrameLayout decorView = (FrameLayout)getWindow().getDecorView();
		decorView.addView(
			fullscreenHolder,
			new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.MATCH_PARENT
			));

		customView.setKeepScreenOn(true);
		((View)currentAlbumController).setVisibility(View.GONE);
		setCustomFullscreen(true);

		if (view instanceof FrameLayout) {
			if (((FrameLayout)view).getFocusedChild() instanceof VideoView) {
				videoView = (VideoView)((FrameLayout)view).getFocusedChild();
				videoView.setOnErrorListener(new VideoCompletionListener());
				videoView.setOnCompletionListener(new VideoCompletionListener());
			}
		}
	}

	@Override
	public void onHideCustomView() {
		FrameLayout decorView = (FrameLayout)getWindow().getDecorView();
		decorView.removeView(fullscreenHolder);
		customView.setKeepScreenOn(false);
		((View)currentAlbumController).setVisibility(View.VISIBLE);
		setCustomFullscreen(false);
		fullscreenHolder = null;
		customView = null;
		if (videoView != null) {
			videoView.setOnErrorListener(null);
			videoView.setOnCompletionListener(null);
			videoView = null;
		}
		contentFrame.requestFocus();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		WebView.HitTestResult result = ninjaWebView.getHitTestResult();
		if (result.getExtra() != null) {
			if (result.getType() == SRC_ANCHOR_TYPE)
				showContextMenuLink("", result.getExtra(), SRC_ANCHOR_TYPE, false);
			else if (result.getType() == SRC_IMAGE_ANCHOR_TYPE) {
				// Create a background thread that has a Looper
				HandlerThread handlerThread = new HandlerThread("HandlerThread");
				handlerThread.start();
				// Create a handler to execute tasks in the background thread.
				Handler backgroundHandler = new Handler(handlerThread.getLooper());
				Message msg = backgroundHandler.obtainMessage();
				ninjaWebView.requestFocusNodeHref(msg);
				String url = (String)msg.getData().get("url");
				showContextMenuLink("", url, SRC_ANCHOR_TYPE, false);
			} else if (result.getType() == IMAGE_TYPE) {
				showContextMenuLink("", result.getExtra(), IMAGE_TYPE, false);
			} else
				showContextMenuLink("", result.getExtra(), 0, false);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private void initOverview() {
		bottomSheetDialog_OverView = findViewById(R.id.bottomSheetDialog_OverView);
		ListView listView = bottomSheetDialog_OverView.findViewById(R.id.list_overView);
		tab_container = bottomSheetDialog_OverView.findViewById(R.id.listOpenedTabs);

		AtomicInteger intPage = new AtomicInteger();

		NavigationBarView.OnItemSelectedListener navListener = menuItem -> {
			intPage.set(R.id.page_0);
			tab_container.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
			return true;
		};

		bottom_navigation = bottomSheetDialog_OverView.findViewById(R.id.bottom_navigation);
		bottom_navigation.setOnItemSelectedListener(navListener);
		bottom_navigation.findViewById(R.id.page_2).setOnLongClickListener(v -> {
			showDialogFilter();
			return true;
		});

		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
		int colorSecondary = typedValue.data;

		badgeTab = bottom_navigation.getOrCreateBadge(R.id.page_0);
		badgeTab.setBackgroundColor(colorSecondary);
		badgeTab.setHorizontalOffset(10);
		badgeTab.setVerticalOffset(10);
		setSelectedTab();
	}

	// Views

	@SuppressLint({"ClickableViewAccessibility", "UnsafeOptInUsageError"})
	private void initOmniBox() {

		omniBox = findViewById(R.id.omniBox);
		omniBox_text = findViewById(R.id.omniBox_input);
		listener = omniBox_text.getKeyListener(); // Save the default KeyListener!!!
		omniBox_text.setKeyListener(null); // Disable input
		omniBox_text.setEllipsize(TextUtils.TruncateAt.END);
		omniBox_tab = findViewById(R.id.omniBox_tab);
		omniBox_tab.setOnClickListener(v -> showTabView());
		omniBox_tab.setOnLongClickListener(view -> {
			performGesture("setting_gesture_tabButton");
			return true;
		});
		omniBox_overview = findViewById(R.id.omnibox_overview);

		list_search = findViewById(R.id.list_search);
		omnibox_close = findViewById(R.id.omnibox_close);
		omnibox_close.setOnClickListener(view -> {
			if (Objects.requireNonNull(omniBox_text.getText()).length() > 0)
				omniBox_text.setText("");
			else
				omniBox_text.clearFocus();
		});

		progressBar = findViewById(R.id.main_progress_bar);
		progressBar.setOnClickListener(v -> {
			ninjaWebView.stopLoading();
			progressBar.setVisibility(View.GONE);
		});
		bottomAppBar = findViewById(R.id.bottomAppBar);

		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
		int colorSecondary = typedValue.data;

		badgeDrawable = BadgeDrawable.create(context);
		badgeDrawable.setBackgroundColor(colorSecondary);

		Button omnibox_overflow = findViewById(R.id.omnibox_overflow);
		omnibox_overflow.setOnClickListener(v -> showOverflow());

		omniBox_overview.setOnTouchListener(new SwipeTouchListener(context) {
			public void onSwipeTop() {
				performGesture("setting_gesture_tb_up");
			}

			public void onSwipeBottom() {
				performGesture("setting_gesture_tb_down");
			}

			public void onSwipeRight() {
				performGesture("setting_gesture_tb_right");
			}

			public void onSwipeLeft() {
				performGesture("setting_gesture_tb_left");
			}
		});
		omniBox_tab.setOnTouchListener(new SwipeTouchListener(context) {
			public void onSwipeTop() {
				performGesture("setting_gesture_nav_up");
			}

			public void onSwipeBottom() {
				performGesture("setting_gesture_nav_down");
			}

			public void onSwipeRight() {
				performGesture("setting_gesture_nav_right");
			}

			public void onSwipeLeft() {
				performGesture("setting_gesture_nav_left");
			}
		});

		omniBox_text.setOnEditorActionListener((v, actionId, event) -> {
			String query = Objects.requireNonNull(omniBox_text.getText()).toString().trim();
			ninjaWebView.loadUrl(query);
			return false;
		});
		omniBox_text.setOnFocusChangeListener((v, hasFocus) -> {
			if (omniBox_text.hasFocus()) {
				omnibox_close.setVisibility(View.VISIBLE);
				list_search.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				omnibox_overflow.setVisibility(View.GONE);
				omniBox_overview.setVisibility(View.GONE);
				omniBox_tab.setVisibility(View.GONE);
				String url = ninjaWebView.getUrl();
				ninjaWebView.stopLoading();
				omniBox_text.setKeyListener(listener);
				if (url == null || url.isEmpty())
					omniBox_text.setText("");
				else
					omniBox_text.setText(url);
				initSearch();
				omniBox_text.selectAll();
			} else {
				HelperUnit.hideSoftKeyboard(omniBox_text, context);
				omnibox_close.setVisibility(View.GONE);
				list_search.setVisibility(View.GONE);
				omnibox_overflow.setVisibility(View.VISIBLE);
				omniBox_overview.setVisibility(View.VISIBLE);
				omniBox_tab.setVisibility(View.VISIBLE);
				omniBox_text.setKeyListener(null);
				omniBox_text.setEllipsize(TextUtils.TruncateAt.END);
				omniBox_text.setText(ninjaWebView.getTitle());
				updateOmniBox();
			}
		});
		omniBox_overview.setOnClickListener(v -> showOverview());
		omniBox_overview.setOnLongClickListener(v -> {
			performGesture("setting_gesture_overViewButton");
			return true;
		});
	}

	@SuppressLint({"UnsafeOptInUsageError"})
	private void updateOmniBox() {

		badgeDrawable.setNumber(BrowserContainer.size());
		badgeTab.setNumber(BrowserContainer.size());
		BadgeUtils.attachBadgeDrawable(badgeDrawable, omniBox_tab, findViewById(R.id.layout));
		omniBox_text.clearFocus();
		ninjaWebView = (NinjaWebView)currentAlbumController;
		String url = ninjaWebView.getUrl();

		if (url != null) {

			progressBar.setVisibility(View.GONE);

			if (Objects.requireNonNull(ninjaWebView.getTitle()).isEmpty())
				omniBox_text.setText(url);
			else
				omniBox_text.setText(ninjaWebView.getTitle());
		}
	}

	private void initSearchPanel() {
		searchPanel = findViewById(R.id.searchBox);
		searchBox = findViewById(R.id.searchBox_input);
		Button searchUp = findViewById(R.id.searchBox_up);
		Button searchDown = findViewById(R.id.searchBox_down);
		Button searchCancel = findViewById(R.id.searchBox_cancel);
		searchBox.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (currentAlbumController != null)
					((NinjaWebView)currentAlbumController).findAllAsync(s.toString());
			}
		});
		searchUp.setOnClickListener(v -> ((NinjaWebView)currentAlbumController).findNext(false));
		searchDown.setOnClickListener(v -> ((NinjaWebView)currentAlbumController).findNext(true));
		searchCancel.setOnClickListener(v -> {
			if (searchBox.getText().length() > 0)
				searchBox.setText("");
			else {
				searchOnSite = false;
				HelperUnit.hideSoftKeyboard(searchBox, context);
				searchPanel.setVisibility(View.GONE);
				omniBox.setVisibility(View.VISIBLE);
			}
		});
	}

	public void initSearch() {
		list_search.setTextFilterEnabled(true);
		list_search.setOnItemClickListener((parent, view, position, id) -> {
			omniBox_text.clearFocus();
			String url = ((TextView)view.findViewById(R.id.dateView)).getText().toString();
			ninjaWebView.loadUrl(url);
		});
		list_search.setOnItemLongClickListener((adapterView, view, i, l) -> {
			String title = ((TextView)view.findViewById(R.id.titleView)).getText().toString();
			String url = ((TextView)view.findViewById(R.id.dateView)).getText().toString();
			showContextMenuLink(title, url, SRC_ANCHOR_TYPE, true);
			return true;
		});
		omniBox_text.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}
		});
	}

	private void showOverview() {
		setSelectedTab();
		bottomSheetDialog_OverView.setVisibility(View.VISIBLE);
		ObjectAnimator animation = ObjectAnimator.ofFloat(bottomSheetDialog_OverView, "translationY", 0);
		animation.setDuration(duration);
		animation.start();
		bottomAppBar.setVisibility(View.GONE);
	}

	public void hideOverview() {
		bottomSheetDialog_OverView.setVisibility(View.GONE);
		ObjectAnimator animation =
			ObjectAnimator.ofFloat(bottomSheetDialog_OverView, "translationY", bottomSheetDialog_OverView.getHeight());
		animation.setDuration(duration);
		animation.start();
		bottomAppBar.setVisibility(View.VISIBLE);
	}

	public void showTabView() {
		bottom_navigation.setSelectedItemId(R.id.page_0);
		bottomSheetDialog_OverView.setVisibility(View.VISIBLE);
		ObjectAnimator animation = ObjectAnimator.ofFloat(bottomSheetDialog_OverView, "translationY", 0);
		animation.setDuration(duration);
		animation.start();
		bottomAppBar.setVisibility(View.GONE);
	}

	private void setSelectedTab() {
		if (overViewTab.equals(getString(R.string.album_title_home)))
			bottom_navigation.setSelectedItemId(R.id.page_1);
		else if (overViewTab.equals(getString(R.string.album_title_bookmarks)))
			bottom_navigation.setSelectedItemId(R.id.page_2);
		else if (overViewTab.equals(getString(R.string.album_title_history)))
			bottom_navigation.setSelectedItemId(R.id.page_3);
	}

	@SuppressLint("ClickableViewAccessibility")
	private void showOverflow() {

		HelperUnit.hideSoftKeyboard(omniBox_text, context);

		final String url = ninjaWebView.getUrl();
		final String title = ninjaWebView.getTitle();

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
		View dialogView = View.inflate(context, R.layout.dialog_menu_overflow, null);

		builder.setView(dialogView);
		AlertDialog dialog_overflow = builder.create();
		dialog_overflow.show();
		HelperUnit.setupDialog(context, dialog_overflow);

		LinearLayout textGroup = dialogView.findViewById(R.id.textGroup);
		TextView overflowURL = dialogView.findViewById(R.id.overflowURL);
		overflowURL.setText(url);
		overflowURL.setEllipsize(TextUtils.TruncateAt.MARQUEE);
		overflowURL.setSingleLine(true);
		overflowURL.setMarqueeRepeatLimit(1);
		overflowURL.setSelected(true);
		textGroup.setOnClickListener(v -> {
			overflowURL.setEllipsize(TextUtils.TruncateAt.MARQUEE);
			overflowURL.setSingleLine(true);
			overflowURL.setMarqueeRepeatLimit(1);
			overflowURL.setSelected(true);
		});
		TextView overflowTitle = dialogView.findViewById(R.id.overflowTitle);
		overflowTitle.setText(title);

		final GridView menu_grid_tab = dialogView.findViewById(R.id.overflow_tab);
		final GridView menu_grid_share = dialogView.findViewById(R.id.overflow_share);
		final GridView menu_grid_save = dialogView.findViewById(R.id.overflow_save);
		final GridView menu_grid_other = dialogView.findViewById(R.id.overflow_other);

		menu_grid_tab.setVisibility(View.VISIBLE);
		menu_grid_share.setVisibility(View.GONE);
		menu_grid_save.setVisibility(View.GONE);
		menu_grid_other.setVisibility(View.GONE);

		// Tab

		GridItem item_01 = new GridItem(getString(R.string.menu_openFav), R.drawable.icon_star);
		GridItem item_02 = new GridItem(getString(R.string.main_menu_new_tabOpen), R.drawable.icon_tab_plus);
		GridItem item_03 = new GridItem(getString(R.string.main_menu_new_tabProfile), R.drawable.icon_profile_trusted);
		GridItem item_04 = new GridItem(getString(R.string.menu_reload), R.drawable.icon_refresh);
		GridItem item_05 = new GridItem(getString(R.string.menu_closeTab), R.drawable.icon_tab_remove);
		GridItem item_06 = new GridItem(getString(R.string.menu_quit), R.drawable.icon_close);

		final List<GridItem> gridList_tab = new LinkedList<>();

		gridList_tab.add(gridList_tab.size(), item_01);
		gridList_tab.add(gridList_tab.size(), item_02);
		gridList_tab.add(gridList_tab.size(), item_05);
		gridList_tab.add(gridList_tab.size(), item_03);
		gridList_tab.add(gridList_tab.size(), item_04);
		gridList_tab.add(gridList_tab.size(), item_06);

		GridAdapter gridAdapter_tab = new GridAdapter(context, gridList_tab);
		menu_grid_tab.setAdapter(gridAdapter_tab);
		gridAdapter_tab.notifyDataSetChanged();

		menu_grid_tab.setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
			if (position == 0)
				NinjaToast.show(context, item_01.getTitle());
			else if (position == 1)
				NinjaToast.show(context, item_02.getTitle());
			else if (position == 2)
				NinjaToast.show(context, item_03.getTitle());
			else if (position == 3)
				NinjaToast.show(context, item_04.getTitle());
			else if (position == 4)
				NinjaToast.show(context, item_05.getTitle());
			else if (position == 5)
				NinjaToast.show(context, item_06.getTitle());
			return true;
		});

		menu_grid_tab.setOnItemClickListener((parent, view14, position, id) -> {
			String favURL =
				Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser/wiki"));
			if (position == 0) {
				ninjaWebView.loadUrl(favURL);
				dialog_overflow.cancel();
			} else if (position == 1) {
				addAlbum(getString(R.string.app_name), favURL, true, false, "", dialog_overflow);
				dialog_overflow.cancel();
			} else if (position == 3) {
				addAlbum(HelperUnit.domain(favURL), favURL, true, true, "", dialog_overflow);
			} else if (position == 4) {
				ninjaWebView.reload();
				dialog_overflow.cancel();
			} else if (position == 2) {
				removeAlbum(currentAlbumController);
				dialog_overflow.cancel();
			} else if (position == 5) {
				doubleTapsQuit();
				dialog_overflow.cancel();
			}
		});

		TabLayout tabLayout = dialogView.findViewById(R.id.tabLayout);
		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				if (tab.getPosition() == 0) {
					menu_grid_tab.setVisibility(View.VISIBLE);
					menu_grid_share.setVisibility(View.GONE);
					menu_grid_save.setVisibility(View.GONE);
					menu_grid_other.setVisibility(View.GONE);
				} else if (tab.getPosition() == 1) {
					menu_grid_tab.setVisibility(View.GONE);
					menu_grid_share.setVisibility(View.VISIBLE);
					menu_grid_save.setVisibility(View.GONE);
					menu_grid_other.setVisibility(View.GONE);
				} else if (tab.getPosition() == 2) {
					menu_grid_tab.setVisibility(View.GONE);
					menu_grid_share.setVisibility(View.GONE);
					menu_grid_save.setVisibility(View.VISIBLE);
					menu_grid_other.setVisibility(View.GONE);
				} else if (tab.getPosition() == 3) {
					menu_grid_tab.setVisibility(View.GONE);
					menu_grid_share.setVisibility(View.GONE);
					menu_grid_save.setVisibility(View.GONE);
					menu_grid_other.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {
			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
			}
		});

		menu_grid_tab.setOnTouchListener(new SwipeTouchListener(context) {
			public void onSwipeRight() {
				Objects.requireNonNull(tabLayout.getTabAt(3)).select();
			}

			public void onSwipeLeft() {
				Objects.requireNonNull(tabLayout.getTabAt(1)).select();
			}
		});

		HelperUnit.setupDialog(context, dialog_overflow);
	}

	// OverflowMenu

	public void showContextMenuLink(String title, final String url, int type, boolean showAll) {

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
		View dialogView = View.inflate(context, R.layout.dialog_menu, null);

		if (title.isEmpty()) {
			title = HelperUnit.domain(url);
		}
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
		menuTitle.setText(title);
		ImageView menu_icon = dialogView.findViewById(R.id.menu_icon);

		if (type == SRC_ANCHOR_TYPE) {
			menu_icon.setImageResource(R.drawable.icon_link);
		} else if (type == IMAGE_TYPE)
			menu_icon.setImageResource(R.drawable.icon_image);
		else
			menu_icon.setImageResource(R.drawable.icon_link);

		builder.setView(dialogView);
		AlertDialog dialog = builder.create();
		dialog.show();
		HelperUnit.setupDialog(context, dialog);

		GridItem item_01 = new GridItem(getString(R.string.main_menu_new_tabOpen), R.drawable.icon_tab_plus);
		GridItem item_02 = new GridItem(getString(R.string.main_menu_new_tab), R.drawable.icon_tab_background);

		final List<GridItem> gridList = new LinkedList<>();

		gridList.add(gridList.size(), item_01);
		gridList.add(gridList.size(), item_02);

		GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
		GridAdapter gridAdapter = new GridAdapter(context, gridList);
		menu_grid.setAdapter(gridAdapter);
		gridAdapter.notifyDataSetChanged();
		String finalTitle = title;
		menu_grid.setOnItemClickListener((parent, view, position, id) -> {
			switch (position) {
				case 0:
					addAlbum(finalTitle, url, true, false, "", dialog);
					dialog.cancel();
					break;
				case 1:
					addAlbum(finalTitle, url, false, false, "", dialog);
					dialog.cancel();
					break;
			}
		});
	}

	// Menus

	private void showDialogFastToggle() {
		ninjaWebView = (NinjaWebView)currentAlbumController;
		String url = ninjaWebView.getUrl();

		if (url != null) {

			MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
			View dialogView = View.inflate(context, R.layout.dialog_toggle, null);
			builder.setView(dialogView);

			TextView dialog_warning = dialogView.findViewById(R.id.dialog_titleDomain);
			dialog_warning.setText(HelperUnit.domain(url));
			dialog_warning.setEllipsize(TextUtils.TruncateAt.END);

			LinearLayout textGroup = dialogView.findViewById(R.id.textGroup);
			TextView overflowURL = dialogView.findViewById(R.id.overflowURL);
			overflowURL.setText(url);
			overflowURL.setEllipsize(TextUtils.TruncateAt.MARQUEE);
			overflowURL.setSingleLine(true);
			overflowURL.setMarqueeRepeatLimit(1);
			overflowURL.setSelected(true);
			textGroup.setOnClickListener(v -> {
				overflowURL.setEllipsize(TextUtils.TruncateAt.MARQUEE);
				overflowURL.setSingleLine(true);
				overflowURL.setMarqueeRepeatLimit(1);
				overflowURL.setSelected(true);
			});
			TextView overflowTitle = dialogView.findViewById(R.id.overflowTitle);
			overflowTitle.setText(ninjaWebView.getTitle());

			AlertDialog dialog = builder.create();
			dialog.show();
			HelperUnit.setupDialog(context, dialog);

			Chip chip_toggleNightView = dialogView.findViewById(R.id.chip_toggleNightView);
			int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
			if ((nightModeFlags == Configuration.UI_MODE_NIGHT_YES) && !sp.getString("sp_theme", "1").equals("2")) {
				chip_toggleNightView.setVisibility(View.VISIBLE);
			} else {
				chip_toggleNightView.setVisibility(View.GONE);
			}
			if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
				chip_toggleNightView.setChecked(sp.getBoolean("setAlgorithmicDarkeningAllowed", true));
				chip_toggleNightView.setOnLongClickListener(view -> {
					Toast.makeText(context, getString(R.string.menu_nightView), Toast.LENGTH_SHORT).show();
					return true;
				});
				chip_toggleNightView.setOnClickListener(v -> {
					ninjaWebView.toggleNightMode();
					dialog.cancel();
				});
			}

			Chip chip_toggleDesktop = dialogView.findViewById(R.id.chip_toggleDesktop);
			chip_toggleDesktop.setChecked(ninjaWebView.isDesktopMode());
			chip_toggleDesktop.setOnLongClickListener(view -> {
				Toast.makeText(context, getString(R.string.menu_desktopView), Toast.LENGTH_SHORT).show();
				return true;
			});
			chip_toggleDesktop.setOnClickListener(v -> {
				ninjaWebView.toggleDesktopMode(true);
				dialog.cancel();
			});

			Chip chip_toggleScreenOn = dialogView.findViewById(R.id.chip_toggleScreenOn);
			chip_toggleScreenOn.setChecked(sp.getBoolean("sp_screenOn", false));
			chip_toggleScreenOn.setOnLongClickListener(view -> {
				Toast.makeText(context, getString(R.string.setting_title_screenOn), Toast.LENGTH_SHORT).show();
				return true;
			});
			chip_toggleScreenOn.setOnClickListener(v -> {
				sp.edit().putBoolean("sp_screenOn", !sp.getBoolean("sp_screenOn", false)).apply();
				saveOpenedTabs();
				HelperUnit.triggerRebirth(context);
				dialog.cancel();
			});

			Chip chip_toggleAudioBackground = dialogView.findViewById(R.id.chip_toggleAudioBackground);
			chip_toggleAudioBackground.setChecked(sp.getBoolean("sp_audioBackground", false));
			chip_toggleAudioBackground.setOnLongClickListener(view -> {
				Toast.makeText(context, getString(R.string.setting_title_audioBackground), Toast.LENGTH_SHORT).show();
				return true;
			});
			chip_toggleAudioBackground.setOnClickListener(v -> {
				sp.edit().putBoolean("sp_audioBackground", !sp.getBoolean("sp_audioBackground", false)).apply();
				dialog.cancel();
			});

			Button ib_reload = dialogView.findViewById(R.id.ib_reload);
			ib_reload.setOnClickListener(view -> {
				if (ninjaWebView != null) {
					dialog.cancel();
					ninjaWebView.reload();
				}
			});

			Button button_help = dialogView.findViewById(R.id.button_help);
			button_help.setOnClickListener(view -> {
				dialog.cancel();
				Uri webpage = Uri.parse("https://github.com/scoute-dich/browser/wiki/Fast-Toggle-Dialog");
				BrowserUnit.intentURL(this, webpage);
			});
		} else {
			NinjaToast.show(context, getString(R.string.app_error));
		}
	}

	// Dialogs

	private void showDialogFilter() {

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
		View dialogView = View.inflate(context, R.layout.dialog_menu, null);
		builder.setView(dialogView);
		builder.setTitle(R.string.menu_filter);
		AlertDialog dialog = builder.create();
		dialog.show();
		HelperUnit.setupDialog(context, dialog);
		CardView cardView = dialogView.findViewById(R.id.albumCardView);
		cardView.setVisibility(View.GONE);

		GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
		final List<GridItem> gridList = new LinkedList<>();
		HelperUnit.addFilterItems(activity, gridList);

		GridAdapter gridAdapter = new GridAdapter(context, gridList);
		menu_grid.setNumColumns(2);
		menu_grid.setHorizontalSpacing(20);
		menu_grid.setVerticalSpacing(20);
		menu_grid.setAdapter(gridAdapter);

		if (menu_grid.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
			ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams)menu_grid.getLayoutParams();
			p.setMargins(56, 20, 56, 20);
			menu_grid.requestLayout();
		}

		gridAdapter.notifyDataSetChanged();
	}

	private void doubleTapsQuit() {
		if (!sp.getBoolean("sp_close_browser_confirm", true))
			finishAndRemoveTask();
		else {
			MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
			builder.setTitle(R.string.setting_title_confirm_exit);
			builder.setIcon(R.drawable.icon_alert);
			builder.setMessage(R.string.toast_quit);
			builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> finishAndRemoveTask());
			builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
			AlertDialog dialog = builder.create();
			dialog.show();
			HelperUnit.setupDialog(context, dialog);
		}
	}

	// Voids

	private void saveOpenedTabs() {
		ArrayList<String> openTabs = new ArrayList<>();
		for (int i = 0; i < BrowserContainer.size(); i++) {
			if (currentAlbumController == BrowserContainer.get(i))
				openTabs.add(0, ((NinjaWebView)(BrowserContainer.get(i))).getUrl());
			else
				openTabs.add(((NinjaWebView)(BrowserContainer.get(i))).getUrl());
		}
		sp.edit().putString("openTabs", TextUtils.join("‚‗‚", openTabs)).apply();
		//Save profile of open Tabs in shared preferences
		ArrayList<String> openTabsProfile = new ArrayList<>();
		for (int i = 0; i < BrowserContainer.size(); i++) {
			if (currentAlbumController == BrowserContainer.get(i))
				openTabsProfile.add(0, ((NinjaWebView)(BrowserContainer.get(i))).getProfile());
			else
				openTabsProfile.add(((NinjaWebView)(BrowserContainer.get(i))).getProfile());
		}
		sp.edit().putString("openTabsProfile", TextUtils.join("‚‗‚", openTabsProfile)).apply();
	}

	private void setCustomFullscreen(boolean fullscreen) {
		if (fullscreen) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				final WindowInsetsController insetsController = getWindow().getInsetsController();
				if (insetsController != null) {
					insetsController.hide(WindowInsets.Type.statusBars());
					insetsController.setSystemBarsBehavior(
						WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
				}
			} else
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				final WindowInsetsController insetsController = getWindow().getInsetsController();
				if (insetsController != null) {
					insetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
					insetsController.setSystemBarsBehavior(
						WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
				}
			} else
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	}

	public void goBack_skipRedirects(String historyUrl) {
		if (ninjaWebView.canGoBack()) {
			ninjaWebView.setIsBackPressed(true);
			ninjaWebView.initPreferences(historyUrl);
			ninjaWebView.goBack();
		}
	}

	private void copyLink(String url) {
		ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("text", url);
		Objects.requireNonNull(clipboard).setPrimaryClip(clip);
		String text = getString(R.string.toast_copy_successful) + ": " + url;
		NinjaToast.show(this, text);
	}

	public void shareLink(String title, String url) {
		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
		sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
		context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link))));
	}

	private void postLink(String data, Dialog dialogParent) {
		String urlForPosting = sp.getString("urlForPosting", "");
		String message = getString(R.string.menu_shareClipboard) + ": " + data;

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
		View dialogViewSubMenu = View.inflate(context, R.layout.dialog_edit, null);

		CardView cardView = dialogViewSubMenu.findViewById(R.id.albumCardView);
		cardView.setVisibility(View.GONE);
		TextInputLayout editTopLayout = dialogViewSubMenu.findViewById(R.id.editTopLayout);
		editTopLayout.setVisibility(View.GONE);
		TextInputLayout editBottomLayout = dialogViewSubMenu.findViewById(R.id.editBottomLayout);
		editBottomLayout.setHint(getString(R.string.dialog_URL_hint));
		editBottomLayout.setHelperText(getString(R.string.dialog_postOnWebsiteHint));

		EditText editTop = dialogViewSubMenu.findViewById(R.id.editBottom);
		if (urlForPosting.isEmpty())
			editTop.setText("");
		else
			editTop.setText(urlForPosting);
		editTop.setHint(getString(R.string.dialog_URL_hint));

		builder.setView(dialogViewSubMenu);
		builder.setTitle(getString(R.string.dialog_postOnWebsite));
		builder.setMessage(message);
		builder.setIcon(R.drawable.icon_post);

		Dialog dialog = builder.create();
		dialog.show();
		HelperUnit.setupDialog(context, dialog);

		Button ib_cancel = dialogViewSubMenu.findViewById(R.id.editCancel);
		ib_cancel.setOnClickListener(v -> {
			HelperUnit.hideSoftKeyboard(editTop, context);
			dialog.cancel();
		});
		Button ib_ok = dialogViewSubMenu.findViewById(R.id.editOK);
		ib_ok.setOnClickListener(v -> {
			String shareTop = editTop.getText().toString().trim();
			sp.edit().putString("urlForPosting", shareTop).apply();
			ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("text", data);
			Objects.requireNonNull(clipboard).setPrimaryClip(clip);
			String text = getString(R.string.toast_copy_successful) + " -  " + data;
			NinjaToast.show(this, text);
			addAlbum("", shareTop, true, false, "", dialog);
			HelperUnit.hideSoftKeyboard(editTop, context);
			dialog.cancel();
			try {
				dialogParent.cancel();
			} catch (Exception e) {
				Log.i(TAG, "shouldOverrideUrlLoading Exception:" + e);
			}
		});
	}

	private void searchOnSite() {
		searchOnSite = true;
		omniBox.setVisibility(View.GONE);
		searchPanel.setVisibility(View.VISIBLE);
		HelperUnit.showSoftKeyboard(searchBox, activity);
	}

	private void performGesture(String gesture) {
		String gestureAction = Objects.requireNonNull(sp.getString(gesture, "0"));
		switch (gestureAction) {
			case "01":
				break;
			case "02":
				if (ninjaWebView.canGoForward()) {
					WebBackForwardList mWebBackForwardList = ninjaWebView.copyBackForwardList();
					String historyUrl =
						mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex() + 1).getUrl();
					ninjaWebView.initPreferences(historyUrl);
					ninjaWebView.goForward();
				} else
					NinjaToast.show(this, R.string.toast_webview_forward);
				break;
			case "03":
				if (ninjaWebView.canGoBack()) {
					WebBackForwardList mWebBackForwardList = ninjaWebView.copyBackForwardList();
					String historyUrl =
						mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex() - 1).getUrl();
					goBack_skipRedirects(historyUrl);
				} else
					removeAlbum(currentAlbumController);
				break;
			case "04":
				ninjaWebView.pageUp(true);
				break;
			case "05":
				ninjaWebView.pageDown(true);
				break;
			case "06":
				showAlbum(nextAlbumController(false));
				break;
			case "07":
				showAlbum(nextAlbumController(true));
				break;
			case "08":
				showOverview();
				break;
			case "09":
				addAlbum(getString(R.string.app_name),
					Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser/wiki")),
					true, false, "", null);
				break;
			case "10":
				removeAlbum(currentAlbumController);
				break;
			case "11":
				showTabView();
				break;
			case "12":
				shareLink(ninjaWebView.getTitle(), ninjaWebView.getUrl());
				break;
			case "13":
				searchOnSite();
				break;
			case "16":
				ninjaWebView.reload();
				break;
			case "17":
				ninjaWebView.loadUrl(
					Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser/wiki")));
				break;
			case "18":
				bottom_navigation.setSelectedItemId(R.id.page_2);
				showOverview();
				showDialogFilter();
				break;
			case "19":
				showDialogFastToggle();
				break;
			case "20":
				ninjaWebView.toggleNightMode();
				break;
			case "21":
				ninjaWebView.toggleDesktopMode(true);
				break;
			case "22":
				sp.edit().putBoolean("sp_screenOn", !sp.getBoolean("sp_screenOn", false)).apply();
				saveOpenedTabs();
				HelperUnit.triggerRebirth(context);
				break;
			case "23":
				sp.edit().putBoolean("sp_audioBackground", !sp.getBoolean("sp_audioBackground", false)).apply();
				break;
			case "24":
				copyLink(ninjaWebView.getUrl());
				break;
		}
	}

	private void closeTabConfirmation(final Runnable okAction) {
		if (!sp.getBoolean("sp_close_tab_confirm", false))
			okAction.run();
		else {
			MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
			builder.setTitle(R.string.menu_closeTab);
			builder.setIcon(R.drawable.icon_alert);
			builder.setMessage(R.string.toast_quit_TAB);
			builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> okAction.run());
			builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
			AlertDialog dialog = builder.create();
			dialog.show();
			HelperUnit.setupDialog(context, dialog);
		}
	}

	private void dispatchIntent(Intent intent) {

		String action = intent.getAction();
		String url = intent.getStringExtra(Intent.EXTRA_TEXT);
		String data = "";

		if ("".equals(action)) {
			Log.i(TAG, "resumed FOSS browser");
			return;
		} else if (filePathCallback != null) {
			filePathCallback = null;
			getIntent().setAction("");
			return;
		} else if ("postLink".equals(action)) {
			getIntent().setAction("");
			hideOverview();
			postLink(url, null);
			return;
		} else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_PROCESS_TEXT)) {
			CharSequence text = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
			assert text != null;
			data = text.toString();
		} else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_WEB_SEARCH)) {
			data = Objects.requireNonNull(intent.getStringExtra(SearchManager.QUERY));
		} else if (Intent.ACTION_VIEW.equals(action)) {
			data = Objects.requireNonNull(getIntent().getData()).toString();
		} else if (url != null && Intent.ACTION_SEND.equals(action)) {
			data = url;
		}

		if (!data.isEmpty()) {
			addAlbum(null, data, true, false, "", null);
			getIntent().setAction("");
			hideOverview();
			BrowserUnit.openInBackground(activity, intent, data);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private void setWebView(String title, final String url, final boolean foreground) {
		ninjaWebView = new NinjaWebView(context);

		if (Objects.requireNonNull(sp.getString("saved_key_ok", "no")).equals("no")) {
			sp.edit().putString("saved_key_ok", "yes")
				.putString("setting_gesture_tb_up", "04")
				.putString("setting_gesture_tb_down", "05")
				.putString("setting_gesture_tb_left", "03")
				.putString("setting_gesture_tb_right", "02")
				.putString("setting_gesture_nav_up", "16")
				.putString("setting_gesture_nav_down", "10")
				.putString("setting_gesture_nav_left", "07")
				.putString("setting_gesture_nav_right", "06")
				.putString("setting_gesture_tabButton", "19")
				.putString("setting_gesture_overViewButton", "18")
				.putBoolean("sp_autofill", true)
				.apply();
			ninjaWebView.setProfileDefaultValues();
		}

		ninjaWebView.setBrowserController(this);
		ninjaWebView.setAlbumTitle(title, url);
		activity.registerForContextMenu(ninjaWebView);

		SwipeTouchListener swipeTouchListener;
		swipeTouchListener = new SwipeTouchListener(context) {
			public void onSwipeBottom() {
				if (sp.getBoolean("hideToolbar", true)) {
					if (animation == null || !animation.isRunning()) {
						animation = ObjectAnimator.ofFloat(bottomAppBar, "translationY", 0);
						animation.setDuration(duration);
						animation.start();
					}
				}
			}

			public void onSwipeTop() {
				if (!ninjaWebView.canScrollVertically(0) && sp.getBoolean("hideToolbar", true)) {
					if (animation == null || !animation.isRunning()) {
						animation = ObjectAnimator.ofFloat(bottomAppBar, "translationY", bottomAppBar.getHeight());
						animation.setDuration(duration);
						animation.start();
					}
				}
			}
		};

		ninjaWebView.setOnTouchListener(swipeTouchListener);
		ninjaWebView.setOnScrollChangeListener((scrollY, oldScrollY) -> {
			if (!searchOnSite) {
				if (sp.getBoolean("hideToolbar", true)) {
					if (scrollY > oldScrollY) {
						if (animation == null || !animation.isRunning()) {
							animation = ObjectAnimator.ofFloat(bottomAppBar, "translationY", bottomAppBar.getHeight());
							animation.setDuration(duration);
							animation.start();
						}
					} else if (scrollY < oldScrollY) {
						if (animation == null || !animation.isRunning()) {
							animation = ObjectAnimator.ofFloat(bottomAppBar, "translationY", 0);
							animation.setDuration(duration);
							animation.start();
						}
					}
				}
			}
			if (scrollY == 0)
				ninjaWebView.setOnTouchListener(swipeTouchListener);
			else
				ninjaWebView.setOnTouchListener(null);
		});

		if (url.isEmpty())
			ninjaWebView.loadUrl("about:blank");
		else
			ninjaWebView.loadUrl(url);

		if (currentAlbumController != null) {
			ninjaWebView.setPredecessor(currentAlbumController);
			//save currentAlbumController and use when TAB is closed via Back button
			int index = BrowserContainer.indexOf(currentAlbumController) + 1;
			BrowserContainer.add(ninjaWebView, index);
		} else
			BrowserContainer.add(ninjaWebView);

		if (!foreground)
			ninjaWebView.deactivate();
		else {
			ninjaWebView.setBrowserController(this);
			ninjaWebView.activate();
			showAlbum(ninjaWebView);
		}

		View albumView = ninjaWebView.getAlbumView();
		tab_container.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT,
			LinearLayout.LayoutParams.WRAP_CONTENT);

		albumView.setOnLongClickListener(v -> {

			TextView textViewTitle = albumView.findViewById(R.id.titleView);
			TextView textViewUrl = albumView.findViewById(R.id.dateView);
			String titleDialog = textViewTitle.getText().toString();
			String urlDialog = textViewUrl.getText().toString();

			MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
			View dialogView = View.inflate(context, R.layout.dialog_menu, null);

			LinearLayout textGroup = dialogView.findViewById(R.id.textGroup);
			TextView menuURL = dialogView.findViewById(R.id.menuURL);
			menuURL.setText(urlDialog);
			menuURL.setEllipsize(TextUtils.TruncateAt.MARQUEE);
			menuURL.setSingleLine(true);
			menuURL.setMarqueeRepeatLimit(1);
			menuURL.setSelected(true);
			textGroup.setOnClickListener(v2 -> {
				menuURL.setEllipsize(TextUtils.TruncateAt.MARQUEE);
				menuURL.setSingleLine(true);
				menuURL.setMarqueeRepeatLimit(1);
				menuURL.setSelected(true);
			});
			TextView menuTitle = dialogView.findViewById(R.id.menuTitle);
			menuTitle.setText(titleDialog);

			builder.setView(dialogView);
			AlertDialog dialog = builder.create();
			dialog.show();
			HelperUnit.setupDialog(context, dialog);

			GridItem item_01 = new GridItem(context.getString(R.string.menu_share_link), R.drawable.icon_link);
			GridItem item_02 = new GridItem(context.getString(R.string.menu_closeTab), R.drawable.icon_tab_remove);

			final List<GridItem> gridList = new LinkedList<>();
			gridList.add(gridList.size(), item_02);
			gridList.add(gridList.size(), item_01);

			GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
			GridAdapter gridAdapter = new GridAdapter(context, gridList);
			menu_grid.setAdapter(gridAdapter);
			gridAdapter.notifyDataSetChanged();
			menu_grid.setOnItemClickListener((parent, view, position, id) -> {
				dialog.cancel();
				switch (position) {
					case 1:
						Intent sharingIntent = new Intent(Intent.ACTION_SEND);
						sharingIntent.setType("text/plain");
						sharingIntent.putExtra(Intent.EXTRA_SUBJECT, titleDialog);
						sharingIntent.putExtra(Intent.EXTRA_TEXT, urlDialog);
						context.startActivity(
							Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link))));
						break;
					case 0:
						removeAlbum(currentAlbumController);
						if (BrowserContainer.size() < 2) {
							hideOverview();
						}
						break;
				}
			});
			return false;
		});
		updateOmniBox();
	}

	private synchronized void addAlbum(String title, final String url, final boolean foreground,
		final boolean profileDialog, String profile, Dialog dialogParent) {

		//restoreProfile from shared preferences if app got killed
		if (!profile.equals(""))
			sp.edit().putString("profile", profile).apply();
		if (profileDialog) {
			GridItem item_01 =
				new GridItem(getString(R.string.setting_title_profiles_trusted), R.drawable.icon_profile_trusted);
			GridItem item_02 =
				new GridItem(getString(R.string.setting_title_profiles_standard), R.drawable.icon_profile_standard);
			GridItem item_03 =
				new GridItem(getString(R.string.setting_title_profiles_protected), R.drawable.icon_profile_protected);
			GridItem item_04 =
				new GridItem(getString(R.string.setting_title_profiles_changed), R.drawable.icon_profile_changed);

			MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
			View dialogView = View.inflate(context, R.layout.dialog_menu, null);
			builder.setView(dialogView);
			AlertDialog dialog = builder.create();

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
			menuTitle.setText(title);
			dialog.show();
			HelperUnit.setupDialog(context, dialog);

			GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
			final List<GridItem> gridList = new LinkedList<>();
			gridList.add(gridList.size(), item_01);
			gridList.add(gridList.size(), item_02);
			gridList.add(gridList.size(), item_03);
			gridList.add(gridList.size(), item_04);
			GridAdapter gridAdapter = new GridAdapter(context, gridList);
			menu_grid.setAdapter(gridAdapter);
			gridAdapter.notifyDataSetChanged();
			menu_grid.setOnItemClickListener((parent, view, position, id) -> {
				dialogParent.cancel();
				switch (position) {
					case 0:
						sp.edit().putString("profile", "profileTrusted").apply();
						break;
					case 1:
						sp.edit().putString("profile", "profileStandard").apply();
						break;
					case 2:
						sp.edit().putString("profile", "profileProtected").apply();
						break;
					case 3:
						sp.edit().putString("profile", "profileChanged").apply();
						break;
				}
				dialog.cancel();
				hideOverview();
				setWebView(title, url, foreground);
			});
		} else
			setWebView(title, url, foreground);
	}

	private class VideoCompletionListener implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			return false;
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			onHideCustomView();
		}
	}
}
