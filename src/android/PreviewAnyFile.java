package com.mostafa.previewanyfile;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.net.URLEncoder;

import android.os.Build;
import java.lang.reflect.Method;
import android.os.StrictMode;

import androidx.core.content.FileProvider;

public class PreviewAnyFile extends CordovaPlugin {

  private CallbackContext callbackContext; // The callback context from which we were invoked.
  private Context context;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.callbackContext = callbackContext;
    this.context = this.cordova.getContext();
    // this.executeArgs = args;
    if (action.equals("preview")) {
      String url = args.getString(0);
      this.viewFile(url, callbackContext);
    }

    return true;
  }

  private void presentFile(Intent intent, Uri uri, String type) {
    // https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
    // https://inthecheesefactory.com/blog/how-to-share-access-to-file-with-fileprovider-on-android-nougat/en
    File myFile = new File(String.valueOf(uri).replace("file://", ""));
    Uri myUri = FileProvider.getUriForFile(this.context, this.context.getApplicationContext().getPackageName() + ".provider", myFile);
    intent.setDataAndType(myUri, type);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    this.cordova.getActivity().startActivityForResult(intent, 1);
  }

  private void viewFile(String url, CallbackContext callbackContext) {

    if (Build.VERSION.SDK_INT >= 24) {
      try {
        Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
        m.invoke(null);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    boolean file_presented = false;
    String error = null;

    Uri uri = Uri.parse(url);
    Intent intent = new Intent(Intent.ACTION_VIEW);
    String safeURl = url.toLowerCase();
    String extension = MimeTypeMap.getFileExtensionFromUrl(safeURl);
    if (extension == "") {
      extension = safeURl.substring(safeURl.lastIndexOf(".") + 1);
    }
    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    if (mimeType == null) {
      try {
        presentFile(intent, uri, "application/*");
        file_presented = true;
      } catch (ActivityNotFoundException t) {
        error = t.getLocalizedMessage();
        file_presented = false;
      }
    } else {
      try {
        presentFile(intent, uri, mimeType);
        file_presented = true;
      } catch (ActivityNotFoundException e) {
        try {
          presentFile(intent, uri, "application/*");
          file_presented = true;
        } catch (ActivityNotFoundException t) {
          error = t.getLocalizedMessage();
          file_presented = false;
        }
      }
    }

    if (file_presented) {
      callbackContext.success("SUCCESS");
    } else {
      callbackContext.error(error);
    }
  }

}
