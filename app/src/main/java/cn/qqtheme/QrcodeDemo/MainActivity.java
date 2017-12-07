package cn.qqtheme.QrcodeDemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.QrCodeUtils;
import com.google.zxing.client.android.decode.DecodeCallback;

public class MainActivity extends Activity {
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.qrcode_preview);
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //final Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) imageView.getDrawable()).getBitmap();
                final Bitmap bitmap = toBitmap((View) imageView.getParent());
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setItems(new String[]{"保存二维码", "识别二维码", "取消"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (which == 0) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            ImageView iv = new ImageView(MainActivity.this);
                            iv.setImageBitmap(bitmap);
                            builder.setView(iv);
                            builder.setNeutralButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            builder.show();
                        } else if (which == 1) {
                            Result result = QrCodeUtils.scanQrCode(bitmap);
                            if (result == null) {
                                Toast.makeText(MainActivity.this, "二维码识别失败", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this, result.getText(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
                builder.show();
                return false;
            }
        });
    }

    public void onQrcodeScan(View view) {
        QrCodeUtils.launchCaptureActivity(this, new DecodeCallback() {
            @Override
            public void onDecoded(Result result, Bitmap barcode) {
                Toast.makeText(MainActivity.this, result.getText(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void onQrcodeCreate(View view) {
        try {
            Bitmap bitmap = QrCodeUtils.createQrCode("李玉江@贵州穿青人");
            imageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private Bitmap toBitmap(View v) {
        v.clearFocus();
        v.setPressed(false);
        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);
        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);
        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);
        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);
        return bitmap;
    }

}
