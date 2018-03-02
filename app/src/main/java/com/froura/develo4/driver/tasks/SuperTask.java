package com.froura.develo4.driver.tasks;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.froura.develo4.driver.config.TaskConfig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

/**
 * Created by KendrickCosca on 11/27/2017.
 */

public class SuperTask extends AsyncTask<Void, Void, String> {
    private final Context context;
    private final String url;
    private ProgressDialog progressDialog;
    private String message;
    private int resultcode;
    private String id;

    public SuperTask(Context context, String url, String id, String message) {
        this.context = context;
        this.url = url;
        this.message = message;
        this.id = id;
        progressDialog = new ProgressDialog(context);
    }

    public static void execute(Context context, String url, String id, String message) {
        new SuperTask(context,url,id,message).execute();
    }

    public SuperTask(Context context, String url, String id) {
        this.context = context;
        this.url = url;
        this.id = id;
    }

    public static void execute(Context context, String url, String id) {
        new SuperTask(context,url,id).execute();
    }

    public interface TaskListener {
        void onTaskRespond(String json, String id, int resultcode);
        ContentValues setRequestValues(ContentValues contentValues, String id);
    }

    public static String createPostString(Set<Map.Entry<String, Object>> set) throws UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder();
        boolean flag = true;

        for (Map.Entry<String, Object> value : set) {
            stringBuilder.append( flag ? "" : "&" );
            flag = false;
            stringBuilder.append(URLEncoder.encode(value.getKey(), "UTF-8"));
            stringBuilder.append("=");
            stringBuilder.append(URLEncoder.encode(value.getValue().toString(), "UTF-8"));
        }

        return stringBuilder.toString();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(progressDialog != null) {
            progressDialog.setMessage(this.message);
            progressDialog.setIndeterminate(false);
            //progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            URL url = new URL(this.url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);

            OutputStream outputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            Log.d("Error Tag", ((AppCompatActivity)context).getPackageName());
            String postString = createPostString(((TaskListener)this.context).setRequestValues(new ContentValues(), id).valueSet());
            bufferedWriter.write(postString);

            // clear
            bufferedWriter.flush();
            bufferedWriter.close();
            outputStream.close();

            InputStream inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line = "";

            while ((line = bufferedReader.readLine()) != null){
                stringBuilder.append(line);
            }

            resultcode = httpURLConnection.getResponseCode();

            // clear
            bufferedReader.close();
            inputStream.close();

            httpURLConnection.disconnect();

            Log.d("This is the return: ", stringBuilder.toString());
            return stringBuilder.toString();
        } catch (Exception ignored) {
            Log.e("Error here: ", "The error is: ", ignored);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String json) {
        super.onPostExecute(json);
        ((TaskListener)this.context).onTaskRespond(json, id, resultcode);
        if(progressDialog != null)
            progressDialog.dismiss();
    }
}
