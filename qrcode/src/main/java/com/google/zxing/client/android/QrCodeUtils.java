package com.google.zxing.client.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.decode.DecodeCallback;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.Hashtable;

/**
 * 二维码工具类
 * Created by liyujiang on 2017/6/7.
 */
public class QrCodeUtils {
    public static final String TAG = "lyj-qrcode";
    public static final String CHARACTER_SET = "utf-8";
    public static final int MIN_SIZE = 400;

    public static void launchCaptureActivity(Context context, DecodeCallback callback) {
        CaptureActivity.setCallback(callback);
        Intent intent = new Intent(context, CaptureActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static Bitmap createQrCode(String str) throws WriterException {
        return createQrCode(str, MIN_SIZE, MIN_SIZE);
    }

    /**
     * 生成二维码
     */
    public static Bitmap createQrCode(String str, int w, int h) throws WriterException {
        Hashtable<EncodeHintType, String> hints = new Hashtable<>();
        hints.put(EncodeHintType.CHARACTER_SET, CHARACTER_SET);
        BitMatrix matrix = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, w, h, hints);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        //画黑点
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    pixels[y * width + x] = Color.BLACK;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, w, h);
        return bitmap;
    }

    public static Result scanQrCode(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Hashtable<DecodeHintType, String> hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, CHARACTER_SET);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        QRCodeReader reader = new QRCodeReader();
        try {
            return reader.decode(binaryBitmap, hints);
        } catch (NotFoundException e) {
            Log.w(TAG, e);
        } catch (ChecksumException e) {
            Log.w(TAG, e);
        } catch (FormatException e) {
            Log.w(TAG, e);
        }
        return null;
    }

}
