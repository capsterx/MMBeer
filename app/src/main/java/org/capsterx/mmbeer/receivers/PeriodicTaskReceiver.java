package org.capsterx.mmbeer.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import org.capsterx.mmbeer.Constants;

public class PeriodicTaskReceiver extends BroadcastReceiver {

    private static final String TAG = "PeriodicTaskReceiver";
    private static final String INTENT_ACTION = "com.example.app.PERIODIC_TASK_HEART_BEAT";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MMBeer", "Alarm rececieved: " + intent);
        if (intent.getAction() != null && !intent.getAction().isEmpty()) {
            Log.d("MMBeer", context.getClass().toString());
            //MainActivity myApplication = (MainActivity) context.getApplicationContext();
            SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(context);

            if (intent.getAction().equals("android.intent.action.BATTERY_LOW")) {
                sharedPreferences.edit().putBoolean(Constants.BACKGROUND_SERVICE_BATTERY_CONTROL, false).apply();
                stopPeriodicTaskHeartBeat(context);
            } else if (intent.getAction().equals("android.intent.action.BATTERY_OKAY")) {
                sharedPreferences.edit().putBoolean(Constants.BACKGROUND_SERVICE_BATTERY_CONTROL, true).apply();
                restartPeriodicTaskHeartBeat(context);
            } else if (intent.getAction().equals(INTENT_ACTION)) {
                doPeriodicTask(context);
            }
            else {
                Log.d("MMBeer", "Alarm ignored, unknown action: " + intent.getAction());
            }
        }
    }

    private void doPeriodicTask(Context context) {
        Log.d("MMBeer", "Perodic task");
        try {
            DB snappydb = DBFactory.open(context);
            String[] beers  =  snappydb.getArray("beers", String.class);// get array of string
            for (String beer : beers)
            {
Log.d("MMBeer - perodic", beer);
            }
            snappydb.close();
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
    }

    public void restartPeriodicTaskHeartBeat(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        boolean isBatteryOk = sharedPreferences.getBoolean(Constants.BACKGROUND_SERVICE_BATTERY_CONTROL, true);
        Log.d("MMBeer", "Battery ok: " + isBatteryOk);
        Intent alarmIntent = new Intent(context, PeriodicTaskReceiver.class);
        boolean isAlarmUp = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_NO_CREATE) != null;
        Log.d("MMBeer", "Alarm ok: " + isAlarmUp);

        if (isBatteryOk && !isAlarmUp) {
            Log.d("MMBeer", "Starting alarm service");
            //AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            //alarmIntent.setAction(INTENT_ACTION);
            //PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
            //alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 30000, pendingIntent);
        }
    }

    public void stopPeriodicTaskHeartBeat(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, PeriodicTaskReceiver.class);
        alarmIntent.setAction(INTENT_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        alarmManager.cancel(pendingIntent);
    }
}