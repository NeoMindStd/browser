/*
    This file is part of the browser WebApp.

    browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.unit;

import static android.content.Context.*;
import static android.graphics.drawable.Icon.*;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import de.baumann.browser.R;
import de.baumann.browser.activity.BrowserActivity;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.browser.DataURIParser;
import de.baumann.browser.view.GridItem;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;

public class HelperUnit {
	private static SharedPreferences sp;

	public static void saveAs(final Activity activity, String titleMenu, final String url, final String name,
		Dialog dialogParent) {

		try {
			MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);

			View dialogView = View.inflate(activity, R.layout.dialog_edit, null);

			TextInputLayout editTopLayout = dialogView.findViewById(R.id.editTopLayout);
			editTopLayout.setHint(activity.getString(R.string.dialog_title_hint));
			TextInputLayout editBottomLayout = dialogView.findViewById(R.id.editBottomLayout);
			editBottomLayout.setHint(activity.getString(R.string.dialog_extension_hint));

			EditText editTop = dialogView.findViewById(R.id.editTop);
			EditText editBottom = dialogView.findViewById(R.id.editBottom);
			editTop.setHint(activity.getString(R.string.dialog_title_hint));
			editBottom.setHint(activity.getString(R.string.dialog_extension_hint));

			String filename = name != null ? name : URLUtil.guessFileName(url, null, null);
			String extension = filename.substring(filename.lastIndexOf("."));
			String prefix = filename.substring(0, filename.lastIndexOf("."));

			editTop.setText(prefix);
			if (extension.length() <= 8)
				editBottom.setText(extension);

			LinearLayout textGroupEdit = dialogView.findViewById(R.id.textGroupEdit);
			TextView menuURLEdit = dialogView.findViewById(R.id.menuURLEdit);
			menuURLEdit.setText(url);
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
			menuTitleEdit.setText(titleMenu);

			builder.setView(dialogView);

			AlertDialog dialog = builder.create();
			dialog.show();
			HelperUnit.setupDialog(activity, dialog);

			Button ib_cancel = dialogView.findViewById(R.id.editCancel);
			ib_cancel.setOnClickListener(view -> {
				hideSoftKeyboard(editBottom, activity);
				dialog.cancel();
			});
			Button ib_ok = dialogView.findViewById(R.id.editOK);
			ib_ok.setOnClickListener(view12 -> {

				String title = editTop.getText().toString().trim();
				String extension1 = editBottom.getText().toString().trim();
				String filename1 = title + extension1;

				if (title.isEmpty() || !extension1.startsWith(".")) {
					NinjaToast.show(activity, activity.getString(R.string.toast_input_empty));
				} else {
					if (BackupUnit.checkPermissionStorage(activity)) {
						try {
							Uri source = Uri.parse(url);
							DownloadManager.Request request = new DownloadManager.Request(source);
							String cookies = CookieManager.getInstance().getCookie(url);
							request.addRequestHeader("cookie", cookies);
							request.addRequestHeader("Accept", "text/html, application/xhtml+xml, *" + "/" + "*");
							request.addRequestHeader("Accept-Language", "en-US,en;q=0.7,he;q=0.3");
							request.addRequestHeader("Referer", url);
							request.setNotificationVisibility(
								DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
							request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename1);
							DownloadManager dm = (DownloadManager)activity.getSystemService(DOWNLOAD_SERVICE);
							assert dm != null;
							dm.enqueue(request);
						} catch (Exception e) {
							System.out.println("Error Downloading File: " + e);
							Toast.makeText(activity, activity.getString(R.string.app_error) + e.toString()
								.substring(e.toString().indexOf(":")), Toast.LENGTH_LONG).show();
							e.printStackTrace();
						}

					} else {
						BackupUnit.requestPermission(activity);
					}
					HelperUnit.hideSoftKeyboard(editBottom, activity);
					try {
						dialog.cancel();
					} catch (Exception e) {
						Log.i("FOSS Browser", "shouldOverrideUrlLoading Exception:" + e);
					}
					dialogParent.cancel();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String fileName(String url) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
		String currentTime = sdf.format(new Date());
		String domain = Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim();
		return domain.replace(".", "_").trim() + "_" + currentTime.trim();
	}

	public static String domain(String url) {
		if (url == null) {
			return "";
		} else {
			try {
				return Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim();
			} catch (Exception e) {
				return "";
			}
		}
	}

	public static void initTheme(Activity context) {
		sp = PreferenceManager.getDefaultSharedPreferences(context);

		if (sp.getBoolean("useDynamicColor", true)) {
			switch (Objects.requireNonNull(sp.getString("sp_theme", "1"))) {
				case "2":
					context.setTheme(R.style.AppTheme_wallpaper_day);
					break;
				case "3":
					context.setTheme(R.style.AppTheme_wallpaper_night);
					break;
				case "5":
					context.setTheme(R.style.AppTheme_OLED);
					break;
				default:
					context.setTheme(R.style.AppTheme_wallpaper);
					break;
			}
		} else {
			switch (Objects.requireNonNull(sp.getString("sp_theme", "1"))) {
				case "2":
					context.setTheme(R.style.AppTheme_day);
					break;
				case "3":
					context.setTheme(R.style.AppTheme_night);
					break;
				case "5":
					context.setTheme(R.style.AppTheme_OLED);
					break;
				default:
					context.setTheme(R.style.AppTheme);
					break;
			}
		}
	}

	public static void addFilterItems(Activity activity, List<GridItem> gridList) {
		GridItem item_01 =
			new GridItem(sp.getString("icon_01", activity.getResources().getString(R.string.color_red)), 11);
		GridItem item_02 =
			new GridItem(sp.getString("icon_02", activity.getResources().getString(R.string.color_pink)), 10);
		GridItem item_03 =
			new GridItem(sp.getString("icon_03", activity.getResources().getString(R.string.color_purple)), 9);
		GridItem item_04 =
			new GridItem(sp.getString("icon_04", activity.getResources().getString(R.string.color_blue)), 8);
		GridItem item_05 =
			new GridItem(sp.getString("icon_05", activity.getResources().getString(R.string.color_teal)), 7);
		GridItem item_06 =
			new GridItem(sp.getString("icon_06", activity.getResources().getString(R.string.color_green)), 6);
		GridItem item_07 =
			new GridItem(sp.getString("icon_07", activity.getResources().getString(R.string.color_lime)), 5);
		GridItem item_08 =
			new GridItem(sp.getString("icon_08", activity.getResources().getString(R.string.color_yellow)), 4);
		GridItem item_09 =
			new GridItem(sp.getString("icon_09", activity.getResources().getString(R.string.color_orange)), 3);
		GridItem item_10 =
			new GridItem(sp.getString("icon_10", activity.getResources().getString(R.string.color_brown)), 2);
		GridItem item_11 =
			new GridItem(sp.getString("icon_11", activity.getResources().getString(R.string.color_grey)), 1);
		GridItem item_12 =
			new GridItem(sp.getString("icon_12", activity.getResources().getString(R.string.setting_theme_system)), 0);

		if (sp.getBoolean("filter_01", true))
			gridList.add(gridList.size(), item_01);
		if (sp.getBoolean("filter_02", true))
			gridList.add(gridList.size(), item_02);
		if (sp.getBoolean("filter_03", true))
			gridList.add(gridList.size(), item_03);
		if (sp.getBoolean("filter_04", true))
			gridList.add(gridList.size(), item_04);
		if (sp.getBoolean("filter_05", true))
			gridList.add(gridList.size(), item_05);
		if (sp.getBoolean("filter_06", true))
			gridList.add(gridList.size(), item_06);
		if (sp.getBoolean("filter_07", true))
			gridList.add(gridList.size(), item_07);
		if (sp.getBoolean("filter_08", true))
			gridList.add(gridList.size(), item_08);
		if (sp.getBoolean("filter_09", true))
			gridList.add(gridList.size(), item_09);
		if (sp.getBoolean("filter_10", true))
			gridList.add(gridList.size(), item_10);
		if (sp.getBoolean("filter_11", true))
			gridList.add(gridList.size(), item_11);
		if (sp.getBoolean("filter_12", true))
			gridList.add(gridList.size(), item_12);
	}

	public static void showSoftKeyboard(View view, Activity context) {
		assert view != null;
		final Handler handler = new Handler();
		handler.postDelayed(() -> {
			if (view.requestFocus()) {
				InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}

	public static void hideSoftKeyboard(View view, Context context) {
		assert view != null;
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	public static void setupDialog(Context context, Dialog dialog) {
		TypedValue typedValue = new TypedValue();
		context.getTheme().resolveAttribute(R.attr.colorError, typedValue, true);
		int color = typedValue.data;
		ImageView imageView = dialog.findViewById(android.R.id.icon);
		if (imageView != null)
			imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
		if (sp.getString("sp_theme", "1").equals("5")) {
			dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
			dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_border);
		}
	}

	public static void triggerRebirth(Context context) {
		sp = PreferenceManager.getDefaultSharedPreferences(context);
		sp.edit().putInt("restart_changed", 0).apply();
		sp.edit().putBoolean("restoreOnRestart", true).apply();

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
		builder.setTitle(R.string.menu_restart);
		builder.setIcon(R.drawable.icon_alert);
		builder.setMessage(R.string.toast_restart);
		builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
			PackageManager packageManager = context.getPackageManager();
			Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
			assert intent != null;
			ComponentName componentName = intent.getComponent();
			Intent mainIntent = Intent.makeRestartActivityTask(componentName);
			context.startActivity(mainIntent);
			System.exit(0);
		});
		builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
		AlertDialog dialog = builder.create();
		dialog.show();
		HelperUnit.setupDialog(context, dialog);
	}

	/**
	 * This method converts dp unit to equivalent pixels, depending on device density.
	 *
	 * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
	 * @param context Context to get resources and device specific display metrics
	 * @return A float value to represent px equivalent to dp depending on device density
	 */
	public static int convertDpToPixel(float dp, Context context) {
		return Math.round(
			dp * ((float)context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
	}
}
