package com.google.zxing.client.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.decode.BeepManager;
import com.google.zxing.client.android.decode.CaptureActivityHandler;
import com.google.zxing.client.android.decode.DecodeCallback;
import com.google.zxing.client.android.decode.InactivityTimer;
import com.google.zxing.client.android.decode.ViewfinderView;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * 店铺扫码支付-二维码扫描页
 */
public final class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback, OnClickListener {
    private static final int RESULT_LOAD_IMAGE = 0;
    private static final float QRCODE_MIN_SIZE = 400f;
    private static DecodeCallback callback;

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private Result savedResultToShow;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private ImageView opreateView;

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public static void setCallback(DecodeCallback callback) {
        CaptureActivity.callback = callback;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(QrCodeUtils.TAG, getClass().getName() + ".onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//需要阻止黑屏
        setContentView(R.layout.activity_capture);
        findViewById(R.id.qrcode_back).setOnClickListener(this);
        findViewById(R.id.qrcode_open_photo).setOnClickListener(this);

        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        opreateView = (ImageView) findViewById(R.id.qrcode_flashlight);

        opreateView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (cameraManager != null) {
                    CaptureConfig.KEY_FRONT_LIGHT = !CaptureConfig.KEY_FRONT_LIGHT;
                    if (CaptureConfig.KEY_FRONT_LIGHT) {
                        opreateView.setImageResource(R.drawable.ic_camera_close);
                    } else {
                        opreateView.setImageResource(R.drawable.ic_camera_open);
                    }
                    cameraManager.getConfigManager().initializeTorch(
                            cameraManager.getCamera().getParameters(), false);
                    onPause();
                    onResume();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(QrCodeUtils.TAG, getClass().getName() + ".onResume");

        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());

        viewfinderView = (ViewfinderView) findViewById(R.id.qrcode_viewfinder);
        viewfinderView.setCameraManager(cameraManager);

        handler = null;
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.qrcode_preview);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        beepManager.tePrefs();

        inactivityTimer.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(QrCodeUtils.TAG, getClass().getName() + ".onPause");
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.qrcode_preview);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(QrCodeUtils.TAG, getClass().getName() + ".onDestroy");
        inactivityTimer.shutdown();
        callback = null;
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(QrCodeUtils.TAG, getClass().getName() + ".onKeyDown");
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // restartPreviewAfterDelay(0L);
                return super.onKeyDown(keyCode, event);
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void decodeOrStoreSavedBitmap(Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.qrcode_decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(QrCodeUtils.TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
            return;
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult The contents of the barcode.
     * @param barcode   A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode) {
        inactivityTimer.onActivity();

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            // Then not from history, so beep/vibrate and we have an image to
            // draw on
            beepManager.playBeepSoundAndVibrate();
            //drawResultPoints(barcode, rawResult);
            viewfinderView.drawResultBitmap(barcode);
        }

        if (callback != null) {
            callback.onDecoded(rawResult, barcode);
        }

        restartPreviewAfterDelay(3000);
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of
     * the barcode.
     *
     * @param barcode   A bitmap of the captured image.
     * @param rawResult The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1]);
            } else if (points.length == 4
                    && (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A || rawResult
                    .getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1]);
                drawLine(canvas, paint, points[2], points[3]);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    canvas.drawPoint(point.getX(), point.getY(), paint);
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b) {
        canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            Log.w(QrCodeUtils.TAG, "No SurfaceHolder provided");
            return;
        }
        if (cameraManager.isOpen()) {
            Log.w(QrCodeUtils.TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, viewfinderView, null, null, cameraManager);
            }
            decodeOrStoreSavedBitmap(null);
        } catch (IOException ioe) {
            Log.w(QrCodeUtils.TAG, ioe);
            showToast(getString(R.string.qrcode_camera_problem));
            finish();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety...
            Log.w(QrCodeUtils.TAG, e);
            showToast(getString(R.string.qrcode_framwork_problem));
            finish();
        }
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.qrcode_restart_preview, delayMS);
        }
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.qrcode_back) {
            finish();
        } else if (id == R.id.qrcode_open_photo) {
            showToast("请选择含二维码的图片");
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, RESULT_LOAD_IMAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        Log.d(QrCodeUtils.TAG, getClass().getName() + ".onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == RESULT_LOAD_IMAGE && null != data) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    Uri uri = data.getData();
                    if (uri == null) {
                        showToastInWorkThread("二维码选择失败");
                        return;
                    }
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
                    if (cursor == null) {
                        showToastInWorkThread("二维码选择失败");
                        return;
                    }
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String picturePath = cursor.getString(columnIndex);
                    Log.i(QrCodeUtils.TAG, "path=" + picturePath);
                    cursor.close();
                    Bitmap bitmap = decodeBitmapFromPath(picturePath);
                    if (bitmap == null) {
                        showToastInWorkThread("二维码选择失败");
                        return;
                    }
                    Result result = QrCodeUtils.scanQrCode(bitmap);
                    if (result == null) {
                        showToastInWorkThread("二维码识别失败");
                    } else {
                        Log.d(QrCodeUtils.TAG, "二维码识别成功：" + result.getText());
                        decodeOrStoreSavedBitmap(result);
                    }
                }
            });
        }
    }

    private Bitmap decodeBitmapFromPath(String picturePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picturePath, options);
        options.inJustDecodeBounds = false;
        int sampleSize = (int) (options.outHeight / QRCODE_MIN_SIZE);
        if (sampleSize <= 0) {
            sampleSize = 1;
        }
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(picturePath, options);
    }

    private void showToast(CharSequence text) {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void showToastInWorkThread(CharSequence text) {
        Looper.prepare();
        showToast(text);
        Looper.loop();
    }

}
