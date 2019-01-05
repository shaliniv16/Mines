package com.example.shaliniverma.mines;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class Display extends AppCompatActivity {

    public static final String LOG_TAG = Display.class.getSimpleName();
    private static final String USGS_REQUEST_URL = "https://thingspeak.com/channels/651316/feed.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        Intent intent = getIntent();
        MinesAsyncTask task = new MinesAsyncTask();
        task.execute();
    }

    private void updateUi(Content content) {

        TextView tempTextView = (TextView) findViewById(R.id.temp);
        tempTextView.setText(content.temp);

        TextView humidityTextView = (TextView) findViewById(R.id.humidity);
        humidityTextView.setText(content.humidity);

    }


    private class MinesAsyncTask extends AsyncTask<URL, Void, Content> {

        @Override
        protected Content doInBackground(URL... urls) {

            URL url = createUrl(USGS_REQUEST_URL);

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                // TODO Handle the IOException
            }
            Content content = extractFeatureFromJson(jsonResponse);
            return content;
        }

        @Override

        protected void onPostExecute(Content content){

            if(content == null){
                return;
            }
            updateUi(content);
        }
        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }
        private String makeHttpRequest(URL url)throws IOException{
            String jsonResponse = "";
            if(url == null){
                return jsonResponse;
            }
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();
                if(urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                }
            } catch (IOException e) {
                // TODO: Handle the exception
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    inputStream.close();
                }
            }
            return jsonResponse;
        }
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }
        private Content extractFeatureFromJson(String minesJSON) {
            if(TextUtils.isEmpty(minesJSON)){
                return null;
            }
            try {
                JSONObject baseJsonResponse = new JSONObject(minesJSON);
                JSONArray featureArray = baseJsonResponse.getJSONArray("feeds");

                // If there are results in the features array
                if (featureArray.length() > 0) {
                    // Extract out the first feature (which is an earthquake)
                    JSONObject firstFeature = featureArray.getJSONObject(0);
                    //JSONObject properties = firstFeature.getJSONObject("feeds");

                    // Extract out the temperature and humidity
                    String temp = firstFeature.getString("field1");
                    String humidity = firstFeature.getString("field2");

                    // Create a new {@link Event} object

                    return new Content(temp, humidity);
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
            }
            return null;
        }
    }

}