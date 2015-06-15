package org.capsterx.mmbeer.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ProgressBar;

import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;



public class Get_ID extends AsyncTask<Void, Integer, String> {
    String user_;
    String pass_;
    Context context_;

    public Get_ID(Context context, String user, String pass) {
        context_ = context;
        user_ = user;
        pass_ = pass;
        Log.d("MMBeer", "Get ID Construction");
    }

    protected String doInBackground(Void... params) {

        try {
            Log.d("MMBeer", "Get ID Logging in");
            publishProgress(1, 4);

            Connection.Response res = Jsoup.connect("http://www.mmbeerclub.com/home?destination=node/3")
                    .data("name", user_, "pass", pass_, "form_id", "user_login_block", "op", "Log in")
                    .method(Connection.Method.POST)
                    .timeout(0)
                    .execute();
            Map<String, String> cookies = res.cookies();
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                Log.d("MMBeer", entry.getKey() + "=" + entry.getValue());
            }
            publishProgress(2, 4);

            Document doc = Jsoup.connect("http://www.mmbeerclub.com/node/2")
                    .cookies(cookies)
                    .timeout(0)
                    .get();
            String href = doc.select("#your-settings").select("a").first().attr("href");
            href = href.split("=")[1];
            href = "http://www.mmbeerclub.com/" + href;
            Log.d("MMBeer", "ID Setting href = " + href);
            publishProgress(3, 4);

            doc = Jsoup.connect(href)
                    .cookies(cookies)
                    .timeout(0)
                    .get();

            String id = doc.select("#edit-field-card-number-und-0-value").attr("value");
            Log.d("MMBeer", "id=" + id);
            publishProgress(4, 4);

            return id;
        } catch (IOException e) {
            Log.d("MMBeer", "Get ID Failed " + e.getMessage());
        }
        return "";
    }

    protected void onProgressUpdate(Integer... progress) {
        Log.d("MMBeer", "Count: " + progress[0] + " of " + progress[1]);
    }

    protected void onPostExecute(String id) {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(context_);
        sharedPrefs.edit().putString("club_id", id).apply();
    }
}