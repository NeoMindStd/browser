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

import static android.Manifest.permission.*;
import static android.os.Build.VERSION.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.baumann.browser.R;

public class BackupUnit {

	public static final int PERMISSION_REQUEST_CODE = 123;

	public static boolean checkPermissionStorage(Context context) {
		if (SDK_INT >= Build.VERSION_CODES.R)
			return Environment.isExternalStorageManager();
		else {
			int result = ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE);
			int result1 = ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE);
			return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
		}
	}

	public static void requestPermission(Activity activity) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
		builder.setIcon(R.drawable.icon_alert);
		builder.setTitle(R.string.app_warning);
		builder.setMessage(R.string.app_permission);
		builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
			dialog.cancel();
			if (SDK_INT >= Build.VERSION_CODES.R) {
				try {
					Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
					intent.addCategory("android.intent.category.DEFAULT");
					intent.setData(Uri.parse(String.format("package:%s", activity.getPackageName())));
					activity.startActivity(intent);
				} catch (Exception e) {
					Intent intent = new Intent();
					intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
					activity.startActivity(intent);
				}
			} else {
				//below android 11
				ActivityCompat.requestPermissions(activity, new String[] {WRITE_EXTERNAL_STORAGE},
					PERMISSION_REQUEST_CODE);
			}
		});
		builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
		AlertDialog dialog = builder.create();
		dialog.show();
		HelperUnit.setupDialog(activity, dialog);
	}
}
