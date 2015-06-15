package org.capsterx.mmbeer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.capsterx.mmbeer.services.BackgroundService;
import org.capsterx.mmbeer.services.BeerSpiceService;
import org.capsterx.mmbeer.tasks.Download_Beers;
import org.capsterx.mmbeer.tasks.Get_ID;
import org.capsterx.mmbeer.types.BeerNameList;

import java.util.*;

import android.util.Log;
import android.view.Window;
import android.widget.*;

import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.UncachedSpiceService;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.PendingRequestListener;
import com.octo.android.robospice.request.listener.RequestListener;
import com.octo.android.robospice.request.listener.RequestProgress;
import com.octo.android.robospice.request.listener.RequestProgressListener;
import com.snappydb.DBFactory;
import com.snappydb.DB;
import com.snappydb.SnappyDB;
import com.snappydb.SnappydbException;

import java.util.concurrent.Callable;


public class MainActivity extends ActionBarActivity {
    ArrayAdapter<String> myAdapter;
    Map<String, Beer> beers_ = new HashMap<String, Beer>();
    private MenuItem refresh_;
    boolean refresh_enabled_=true;
    SharedPreferences.OnSharedPreferenceChangeListener prefListener_;
    Integer beer_count_;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    protected SpiceManager spiceManager = new SpiceManager(BeerSpiceService.class);

    private class BeerUpdater implements RequestProgressListener, PendingRequestListener<BeerNameList>
    {
        @Override
        public void onRequestNotFound()
        {
            setRefreshable(true);
        }

        @Override
        public void onRequestProgressUpdate(RequestProgress progress)
        {
            ProgressBar b = (ProgressBar) findViewById(R.id.progressBar);
            Log.d("MMBeer", "got progress: " + progress.getProgress());
            b.setMax(100);
            b.setProgress((int)(progress.getProgress() * 100));
        }

        @Override
        public void onRequestFailure(SpiceException spiceException)
        {
            Log.d("MMBeer", "Got expcetion from downlader " + spiceException.getMessage());
        }

        @Override
        public void onRequestSuccess(BeerNameList beer_names)
        {
            for (String beer : beer_names)
            {
                Log.d("MMBeer", beer);
            }
            myAdapter.clear();
            List<String> dedupeBeers =
                    new ArrayList<String>(new LinkedHashSet<String>(beer_names));
            Log.d("MMBeer", "Found: " + beer_names.size() + " deduped size: " + dedupeBeers.size());

            for (String name : beer_names) {
                if (!beers_.containsKey(name)) {
                    Beer beer = new Beer(name);
                    beers_.put(name, beer);
                }
            }
            beer_count_ = beer_names.size();
            saveBeers();
            set_title();

            myAdapter.addAll(dedupeBeers);
            myAdapter.sort(new Comparator<String>() {
                public int compare(String lhs, String rhs) {
                    return lhs.compareTo(rhs);
                }
            });
            myAdapter.notifyDataSetChanged();
            setRefreshable(true);
            saveLoaded();
        }
    }


    private java.util.Date lastLoaded() {
        try {
            DB snappyDB = new SnappyDB.Builder(getApplicationContext())
                    .registerSerializers(java.util.Date.class, new DefaultSerializers.DateSerializer())
                    .build();
            return  snappyDB.get("last_loaded", java.util.Date.class);// get array of string
        } catch (SnappydbException e) {
            Log.d("MMBeer", "lastLoaded exception: " + e.getMessage());
        }
        return new java.util.Date(0);
    }

    private void saveLoaded() {
        try {
            DB snappyDB = new SnappyDB.Builder(getApplicationContext())
                    .registerSerializers(java.util.Date.class, new DefaultSerializers.DateSerializer())
                    .build();
            snappyDB.put("last_loaded", new java.util.Date());
            TextView lastUpdated = (TextView) findViewById(R.id.lastUpdated);

            lastUpdated.setText("Last Updated: " + lastLoaded());

        } catch (SnappydbException e) {
            Log.d("MMBeer", "saveLoaded exception: " + e.getMessage());
        }
    }

    private void loadBeers()
    {
        try {
            DB snappydb = DBFactory.open(getApplicationContext());
            String[] beers  =  snappydb.getArray("beers", String.class);// get array of string
            for (String beer : beers)
            {
                beers_.put(beer, new Beer(beer));
            }
            beer_count_ = snappydb.getInt("beer_count");
            snappydb.close();
        }
        catch (SnappydbException e)
        {
            Log.d("MMBeer", "loadBeers exception: " + e.getMessage());
        }
    }

    private void saveBeers()
    {
        try {
            DB snappydb = DBFactory.open(getApplicationContext());
            List<String> beer_names = new ArrayList<String>();
            for (Map.Entry<String, Beer> entry : beers_.entrySet()) {
                beer_names.add(entry.getKey());
            }
            String[] array = beer_names.toArray(new String[beer_names.size()]);

            snappydb.put("beers", array);
            snappydb.putInt("beer_count", beer_count_);
            snappydb.close();
        }
        catch (SnappydbException e)
        {
            Log.d("MMBeer", "saveBeers exception: " + e.getMessage());
        }
    }



    private void refresh() {
        if  (isRefreshable())
        {
            TextView lastUpdated = (TextView) findViewById(R.id.lastUpdated);
            lastUpdated.setText("Updating...");

            Log.d("MMBeer", "Refreshing...");
            List<String> beer_names = new ArrayList<>();
            //Display_Beers display = new Display_Beers(beer_names);
            SharedPreferences sharedPrefs = PreferenceManager
                    .getDefaultSharedPreferences(this);
            String user = sharedPrefs.getString("username", "NULL");
            String pass = sharedPrefs.getString("password", "NULL");
            if (!user.equals("NULL") && !pass.equals("NULL")) {
                MainActivity.this.setProgressBarIndeterminateVisibility(true);
                Download_Beers request = new Download_Beers(user, pass);
                spiceManager.execute(request, "get_beer_list", DurationInMillis.ALWAYS_EXPIRED, new BeerUpdater());
                //new Download_Beers(display, beer_names, (ProgressBar) findViewById(R.id.progressBar), user, pass).execute();
            }
            setRefreshable(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        Log.d("MMBeer", "onCreate");
        super.onCreate(savedInstanceState);
        Log.d("MMBeer", "super onCreate");


        setContentView(R.layout.activity_main);


        Intent startServiceIntent = new Intent(getApplicationContext(), BackgroundService.class);
        startService(startServiceIntent);

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
         prefListener_ =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences prefs,
                                                          String key) {
                        if (key.equals("username") || key.equals("password")) {
                            SharedPreferences.Editor edit = prefs.edit();
                            edit.putString("club_id", "none");
                            edit.commit();
                            get_id();
                            refresh();
                        }
                    }
                };
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener_);
        get_id();
        Log.d("MMBeer", "end onCreate");

    }

    private void get_id()
    {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        String id = sharedPrefs.getString("club_id", "none");
        String user = sharedPrefs.getString("username", "NULL");
        String pass = sharedPrefs.getString("password", "NULL");
        if (id.equals("none") && !user.equals("NULL") && !pass.equals("NULL"))
        {
            new Get_ID(this, user, pass).execute();
        }
    }

    void set_title() {
        getSupportActionBar().setTitle("Beers " + beer_count_ + "(" + beers_.size() + ")");
    }

    @Override
    protected void onStop() {
        spiceManager.shouldStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        Log.d("MMBeer", "onStart");

        super.onStart();
        Log.d("MMBeer", "super onStart done");

        spiceManager.start(this);

        spiceManager.addListenerIfPending(BeerNameList.class, "get_beer_list", new BeerUpdater());
        setRefreshable(false);


        loadBeers();
        {
            List<String> beer_names = new ArrayList<String>();
            for (Map.Entry<String, Beer> entry : beers_.entrySet()) {
                beer_names.add(entry.getKey());
            }
            Collections.sort(beer_names);

            myAdapter = new ArrayAdapter<String>(
                    MainActivity.this,
                    android.R.layout.simple_list_item_1,
                    beer_names);
            ListView myList =
                    (ListView) findViewById(R.id.listView);
            myList.setAdapter(myAdapter);
        }

        set_title();

        EditText inputSearch = (EditText) findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user changed the Text
                MainActivity.this.myAdapter.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });
        Log.d("MMBeer", "onStart done");

    }

    void setRefreshable(boolean value)
    {
        refresh_enabled_ = value;
        if (refresh_ != null)
        {
            refresh_.setEnabled(value);
        }
    }

    boolean isRefreshable()
    {
        return refresh_enabled_;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("MMBeer", "Calling on create menu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        refresh_ = menu.getItem(2);
        refresh_.setEnabled(refresh_enabled_);

        java.util.Date now = new java.util.Date();
        Log.d("MMBeer", "Last loaded: " + lastLoaded());
        Log.d("MMBeer", "days = " + ((now.getTime() - lastLoaded().getTime()) / (86400 * 1000)));
        TextView lastUpdated = (TextView) findViewById(R.id.lastUpdated);
        lastUpdated.setText("Last Updated: " + lastLoaded());


        if (beers_.size() == 0 || ((now.getTime() - lastLoaded().getTime()) / (86400 * 1000)) > 1)
        {
            refresh();
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings: {
                Intent i = new Intent(this, Preferences.class);
                startActivityForResult(i, 1);
                break;
            }
            case R.id.beer_id: {
                Intent i = new Intent(this, ClubID.class);
                startActivityForResult(i, 1);

                //Dialog dialog=new Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                //dialog.setContentView(R.layout.club_id);
                //dialog.show();
                break;
            }

            case R.id.refresh_id: {
                refresh();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d("MMBeer", "Saving state");
        super.onSaveInstanceState(outState);
    }
}
