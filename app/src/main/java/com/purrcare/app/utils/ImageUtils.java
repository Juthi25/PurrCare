package com.purrcare.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Photos are shrunk and saved as a Base64 text string inside the Realtime Database.
 * (Firebase Storage normally needs a paid plan now, so this keeps the project free and simple.)
 */
public final class ImageUtils {
    private ImageUtils() { }

    /** Reads the picked/captured image, fixes rotation, shrinks it and returns Base64 text (or null on error). */
    public static String uriToBase64(Context context, Uri uri, int maxSize) {
        try {
            // 1) read only the size of the image
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            while (bounds.outWidth / sample > maxSize * 2 || bounds.outHeight / sample > maxSize * 2) {
                sample *= 2;
            }

            // 2) decode a smaller version
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bitmap;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bitmap == null) return null;

            // 3) rotate if the camera saved it sideways
            int rotation = 0;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in != null) {
                    ExifInterface exif = new ExifInterface(in);
                    int o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    if (o == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                    else if (o == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                    else if (o == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
                }
            } catch (Exception ignored) {
                // no EXIF data - keep rotation 0
            }

            // 4) scale down so the longest side is maxSize
            float scale = Math.min(1f, (float) maxSize / Math.max(bitmap.getWidth(), bitmap.getHeight()));
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            matrix.postRotate(rotation);
            Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // 5) JPEG -> Base64 text
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            result.compress(Bitmap.CompressFormat.JPEG, 75, out);
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    /** Shows the saved photo, or the cute cat placeholder when there is none. */
    public static void setImage(ImageView view, String base64, int placeholderRes) {
        Bitmap bitmap = base64ToBitmap(base64);
        if (bitmap != null) view.setImageBitmap(bitmap);
        else view.setImageResource(placeholderRes);
    }
}
