package com.example.soonamiapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.EventLog;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.logging.SimpleFormatter;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();//Get Log Responses
    //URL to get the dataset query from the server of USGS
    public static final String USGS_REQUEST_URL =
            "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2012-01-01&endtime=2012-12-01&minmagnitude=6";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TsunamiAsyncTask task = new TsunamiAsyncTask();
        task.execute();

    }

    private void updateUI(Event earthquake) {
        TextView titleView = (TextView) findViewById(R.id.title);
        titleView.setText(earthquake.title);

        TextView dateView = (TextView) findViewById(R.id.date);
        titleView.setText(getDateString(earthquake.time));

        TextView tsunamiTextView = (TextView) findViewById(R.id.tsunami_alert);
        titleView.setText(getTsunamiAlertString(earthquake.tsunamiAlert));
    }

    private String getDateString(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MM yyyy 'at' HH:mm:ss z");
        return formatter.format(time);
    }

    private String getTsunamiAlertString(int tsunamiAlert) {
        switch(tsunamiAlert) {
            case 0:
                return getString(R.string.alert_no);
            case 1:
                return getString(R.string.alert_yes);
            default:
                return getString(R.string.alert_not_available);
        }
    }

    private class TsunamiAsyncTask extends AsyncTask<URL, Void, Event> {
        @Override
        protected Event doInBackground(URL... urls) {
            URL url = createURL(USGS_REQUEST_URL);
            String jsonResponse = "";

            try{
                jsonResponse = makeHttpRequest(url);
            } catch(IOException e){
                //handle
            }
            Event earthquake = extractFeatureFromJSON(jsonResponse);
            return earthquake;
        }

        @Override
        protected void onPostExecute(Event earthquake) {
            if(earthquake==null) return;
            updateUI(earthquake);
        }

        private URL createURL(String stringURL) {
            URL url = null;
            try{
                url = new URL(stringURL);
            } catch (MalformedURLException e){
                Log.e(LOG_TAG, "Error with creating url", e);
            }
            return url;
        }

        private String makeHttpRequest(URL url) throws IOException{
            String JSONResponse = "";
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;

            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.connect();

                if(urlConnection.getResponseCode()==200) {
                    inputStream = urlConnection.getInputStream();
                    JSONResponse = readFromInputStream(inputStream);
                }
            } catch (IOException e) {
                //handle
            }
            finally {
                if(urlConnection!=null) urlConnection.disconnect();
                if(inputStream!=null) inputStream.close();
            }
            return JSONResponse;
        }

        private String readFromInputStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if(inputStream!=null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = bufferedReader.readLine();
                while(line!=null) {
                    output.append(line);
                    line = bufferedReader.readLine();
                }
            }
            return output.toString();
        }

        private Event extractFeatureFromJSON(String earthquakeJSON) {
            try {
                JSONObject jsonObject = new JSONObject(earthquakeJSON);
                JSONArray featureArray = jsonObject.getJSONArray("features");

                if(featureArray.length()>0) {
                    JSONObject firstFeature = featureArray.getJSONObject(0);
                    JSONObject properties = firstFeature.getJSONObject("properties");

                    String title = properties.getString("title");
                    long time = properties.getLong("time");
                    int tsunmai_ALert = properties.getInt("tsunami");
                    return new Event(title, time, tsunmai_ALert);
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
            }
            return null;
        }
     }
}