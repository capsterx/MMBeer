package org.capsterx.mmbeer.tasks;
import android.content.Intent;
import android.os.*;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.capsterx.mmbeer.types.BeerNameArrayList;
import org.capsterx.mmbeer.types.BeerNameList;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.lang.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.octo.android.robospice.request.SpiceRequest;

import java.util.concurrent.Callable;




public class Download_Beers extends SpiceRequest<BeerNameList> {
    String user_;
    String pass_;

    public Download_Beers(String user, String pass) {
        super(BeerNameList.class);
        user_ = user;
        pass_ = pass;
        Log.d("MMBeer", "Construction");
    }

    public BeerNameList loadDataFromNetwork() {
        BeerNameList beers = new BeerNameArrayList();
        try {
            Log.d("MMBeer", "Logging in");
            publishProgress(0.01F);

            Connection.Response res = Jsoup.connect("http://www.mmbeerclub.com/home?destination=node/3")
                    .data("name", user_, "pass", pass_, "form_id", "user_login_block", "op", "Log in")
                    .method(Connection.Method.POST)
                    .timeout(0)
                    .execute();
            Map<String,String> cookies = res.cookies();
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                Log.d("MMBeer", entry.getKey() + "=" + entry.getValue());
            }
            publishProgress(0.02F);

            Integer page=new Integer(0);
            boolean finished = false;
            while (!finished) {
                Log.d("MMBeer", "Getting page: " + page);
                Document doc = Jsoup.connect("http://www.mmbeerclub.com/my-beers?page=" + page)
                        .cookies(cookies)
                        .timeout(0)
                        .get();
                String pager = doc.select("#beer-pager").select("span.beer-pager-mid").text();
                String[] pager_parts = pager.split(" ");
                Integer total_count = Integer.parseInt(pager_parts[pager_parts.length - 1]);
                Log.d("MMBeer downloader", "on page=" + page + " total=" + total_count + " progress=" + (float)(page + 3) / (float)(total_count + 2));
                publishProgress((float)(page + 3) / (float)(total_count + 2));
                ++page;
                for(Element row : doc.select("table").first().select("tr"))
                {
                    for (Element column : row.select("td"))
                    {
                        Log.d("MMBeer", "'" + column.text() + "'");
                        if (column.text().equals("Still more beers to drink!"))
                        {
                            finished = true;
                        }
                        else
                        {
                            beers.add(column.text());
                        }
                    }
                }
            }
            publishProgress(1.0F);

            Collections.sort(beers);


        } catch (IOException e)
        {
            Log.d("MMBeer", "Failed " + e.getMessage());
        }
        return beers;
    }

}