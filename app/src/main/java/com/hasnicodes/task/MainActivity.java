package com.hasnicodes.task;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MainActivity extends AppCompatActivity {
    String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    EditText link;
    static ProgressDialog mProgressDialog;
    static String error = null;
    RadioGroup chooser_radio;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Permissions.check(this, permissions, null, null, new PermissionHandler() {
            @Override
            public void onGranted() {
                Toast.makeText(MainActivity.this, "Granted", Toast.LENGTH_SHORT).show();
            }
        });
        initViews();

    }

    void initViews() {
        link = findViewById(R.id.link);
        chooser_radio = findViewById(R.id.chooser_radio);
        link.setText(AppConstant.dummyLink);
        chooser_radio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.pdf) {
                    link.setText(AppConstant.dummyLink);
                } else if (i == R.id.xls) {
                    link.setText(AppConstant.dummyLinkXSLS);
                }
            }
        });

    }


    public void startDownload(View view) {
        String downloadLink = link.getText().toString().trim();
        if (downloadLink.startsWith("http") || downloadLink.startsWith("https")) {
            new DownloadFile(MainActivity.this, downloadLink).execute();
        } else {
            Toast.makeText(this, "Invalid Link", Toast.LENGTH_SHORT).show();
            AppConstant.printError("In Valid");
        }
    }


    private static class DownloadFile extends AsyncTask<String, Integer, String> {
        Context context;
        String myUrl = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setMessage("Downloading..!!");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        public DownloadFile(Context context, String url) {
            this.context = context;
            this.myUrl = url;
        }

        @Override
        protected String doInBackground(String... strings) {
            InputStream input = null;
            OutputStream output = null;
            File file;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(myUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }
                int fileLength = connection.getContentLength();

                //file management
                String rootDic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                String fileName = myUrl.substring(myUrl.lastIndexOf('/') + 1);

                file = new File(rootDic, fileName);

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(file);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                error = e.getMessage();
                return null;
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {

                }

                if (connection != null)
                    connection.disconnect();
            }
            return file.toString();
        }


        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            AppConstant.printError("Progress " + progress[0]);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(final String result) {

            mProgressDialog.dismiss();
            AppConstant.printError(result);
            if (result == null) {
                Toast.makeText(context, "Download error: " + error, Toast.LENGTH_LONG).show();
                AppConstant.printError(error);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Completed");
                builder.setPositiveButton("View", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        File file = new File(result);
                        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
                        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_NEW_TASK);
                        Uri uri = FileProvider.getUriForFile(context,
                                context.getPackageName() + ".provider", file);
                        intent.setDataAndType(uri, mimeType);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            context.startActivity(Intent.createChooser(intent, "choseFile"));
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(context, "No Application available to view this file", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        }

    }

}