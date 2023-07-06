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

import java.util.List;
import java.util.Objects;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.baumann.browser.R;
import de.baumann.browser.view.GridItem;

public class HelperUnit {
	private static SharedPreferences sp;

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
}
