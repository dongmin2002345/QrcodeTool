package cn.qqtheme.QrcodeDemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
                Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                Result result = QrCodeUtils.scanQrCode(bitmap);
                if (result == null) {
                    Toast.makeText(MainActivity.this, "二维码识别失败", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, result.getText(), Toast.LENGTH_LONG).show();
                }
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

}
