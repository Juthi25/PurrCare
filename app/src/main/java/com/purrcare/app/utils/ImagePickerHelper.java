package com.purrcare.app.utils;

import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

/**
 * CAMERA / GALLERY INTENT: lets the user take a photo or choose one from the gallery.
 * Create it in onCreate() of an Activity, then call showChooser() when a button is tapped.
 * The picked image comes back (already shrunk, as Base64 text) in Callback.onImagePicked().
 * No runtime permission is needed: the system camera/gallery apps do the work for us.
 */
public class ImagePickerHelper {

    public interface Callback {
        void onImagePicked(String base64);
    }

    private final AppCompatActivity activity;
    private final int maxSize;
    private final Callback callback;
    private final ActivityResultLauncher<String> galleryLauncher;
    private final ActivityResultLauncher<Uri> cameraLauncher;
    private Uri cameraUri;

    public ImagePickerHelper(AppCompatActivity activity, int maxSize, Callback callback) {
        this.activity = activity;
        this.maxSize = maxSize;
        this.callback = callback;

        // Gallery: system picker for any image
        galleryLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) deliver(uri);
                });

        // Camera: the camera app saves the full photo to the Uri we give it
        cameraLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (Boolean.TRUE.equals(success) && cameraUri != null) deliver(cameraUri);
                });
    }

    public void showChooser() {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Add a photo")
                .setItems(new String[]{"\uD83D\uDCF7  Take a photo", "\uD83D\uDDBC\uFE0F  Choose from gallery"}, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else galleryLauncher.launch("image/*");
                })
                .show();
    }

    private void openCamera() {
        try {
            File dir = new File(activity.getCacheDir(), "images");
            if (!dir.exists() && !dir.mkdirs()) {
                UiUtils.toast(activity, "Could not prepare the camera");
                return;
            }
            File file = new File(dir, "capture_" + System.currentTimeMillis() + ".jpg");
            cameraUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
            cameraLauncher.launch(cameraUri);
        } catch (Exception e) {
            UiUtils.toast(activity, "No camera available on this device");
        }
    }

    private void deliver(Uri uri) {
        String base64 = ImageUtils.uriToBase64(activity, uri, maxSize);
        if (base64 == null) UiUtils.toast(activity, "Could not read that photo, please try another one");
        else callback.onImagePicked(base64);
    }
}
