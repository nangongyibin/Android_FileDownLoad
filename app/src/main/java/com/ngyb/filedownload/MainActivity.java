package com.ngyb.filedownload;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private EditText mEt;
    private ProgressBar mPb;
    private int mMaxLength;
    private int mStartIndex;
    private int mCurrentIndex;
    private boolean isStop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPb = findViewById(R.id.pb);
        mEt = findViewById(R.id.et);
        init();
    }

    private void init() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 7219);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.MOUNT_FORMAT_FILESYSTEMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MOUNT_FORMAT_FILESYSTEMS}, 7219);
        }
    }

    public void play(View view) {
        download();
    }

    public void pause(View view) {
        isStop = true;
    }

    public void continues(View view) {
        if (mCurrentIndex == mMaxLength) {
            mStartIndex = 0;
        }
        isStop = false;
        download();
    }

    public void download() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = mEt.getText().toString().trim();
                if (TextUtils.isEmpty(url)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "请输入下载文件的地址", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    try {
                        URL url1 = new URL(url);
                        HttpURLConnection con = (HttpURLConnection) url1.openConnection();
                        con.setRequestMethod("GET");
                        con.setConnectTimeout(5000);
                        File file = new File(getFileName(url) + ".txt");
                        Log.e(TAG, "run: " + file.getPath());
                        Log.e(TAG, "run: " + file.exists());

                        if (file.exists() && file.length() > 0) {
                            FileInputStream fileInputStream = new FileInputStream(file);
                            BufferedReader bfr = new BufferedReader(new InputStreamReader(fileInputStream));
                            String line = bfr.readLine();
                            mStartIndex = Integer.parseInt(line);
                            con.setRequestProperty("range", "bytes=" + mStartIndex + "-");
                            bfr.close();
                            fileInputStream.close();
                        } else {
                            mStartIndex = 0;
                        }
                        int code = con.getResponseCode();
                        Log.e(TAG, "run: " + code);
                        if (code == 200) {
                            mMaxLength = con.getContentLength();
                        }
                        if (code == 200 || code == 206) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mPb.setMax(mMaxLength);
                                }
                            });
                            InputStream is = con.getInputStream();
                            RandomAccessFile raf = new RandomAccessFile(getFileName(url), "rw");
                            raf.seek(mStartIndex);
                            mCurrentIndex = mStartIndex;
                            int len = 0;
                            byte[] buf = new byte[1024];
                            while ((len = is.read(buf)) != -1) {
                                if (isStop) {
                                    RandomAccessFile tempFile = new RandomAccessFile(getFileName(url) + ".txt", "rw");
                                    tempFile.write(String.valueOf(mCurrentIndex).getBytes());
                                    tempFile.close();
                                    break;
                                }
                                raf.write(buf, 0, len);
                                mCurrentIndex += len;
                                Log.e(TAG, "run:currentindex= " + mCurrentIndex);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mPb.setProgress(mCurrentIndex);
                                    }
                                });
                            }
                            is.close();
                            raf.close();
                            if (mMaxLength-mCurrentIndex<1000000){
                                File deleteFile = new File(getFileName(url) + ".txt");
                                deleteFile.delete();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "下载成功", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i] == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "读写文件的权限获取成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "读写文件的权限获取失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public String getFileName(String url) {
        String[] filenames = url.split("/");
        int length = filenames.length;
        String path = getFilesDir().getPath();
        String localPath = path + "/" + filenames[length - 1];
        Log.e(TAG, "getFileName: " + localPath);
        return localPath;
    }
}
