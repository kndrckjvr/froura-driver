package com.froura.develo4.driver.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

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

/**
 * Created by KendrickCosca on 11/27/2017.
 */

public class CheckUserTasks extends AsyncTask<Void, Void, String> {
    public static void execute(Context context) {
        new CheckUserTasks(context).execute();
    }

    public interface OnLoginDriverTasksListener {
        void parseCheckUserJSONString(String jsonString);
        String createCheckUserPostString(ContentValues contentValues) throws UnsupportedEncodingException;
    }

    private final Context context;

    public CheckUserTasks(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            // set form information
            URL url = new URL(TaskConfig.CHECK_USER_URL);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);

            OutputStream outputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

            String postString = ((OnLoginDriverTasksListener)context).createCheckUserPostString(new ContentValues());
            bufferedWriter.write(postString);

            // clear
            bufferedWriter.flush();
            bufferedWriter.close();
            outputStream.close();

            InputStream inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder stringBuilder = new StringBuilder();
            String line = "";

            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line);

            // clear
            bufferedReader.close();
            inputStream.close();

            httpURLConnection.disconnect();

            return stringBuilder.toString();
        } catch (Exception e) {}
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }


    @Override
    protected void onPostExecute(String jsonString) {
        super.onPostExecute(jsonString);
        ((OnLoginDriverTasksListener)context).parseCheckUserJSONString(jsonString);
    }
}
