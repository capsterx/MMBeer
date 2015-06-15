package org.capsterx.mmbeer.services;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;

import com.octo.android.robospice.UncachedSpiceService;
import com.octo.android.robospice.persistence.CacheManager;
import com.octo.android.robospice.persistence.exception.CacheCreationException;
import com.octo.android.robospice.persistence.exception.CacheSavingException;

/**
 * Created by jbrannan on 6/14/15.
 */
public class BeerSpiceService extends UncachedSpiceService {
    @Override
    public Notification createDefaultNotification() {
//        Notification notification = super.createDefaultNotification();
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
        //temporary fix https://github.com/octo-online/robospice/issues/200
//        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0);
//            notification.setLatestEventInfo(this, "", "", pendingIntent);
        //      }
//        return notification;
//    }
        return null;
    }



    //@Override
    //public CacheManager createCacheManager(Application application) {
    //    return new CacheManager() {
    //        @Override
    //        public <T> T saveDataToCacheAndReturnData(T data, Object cacheKey) throws CacheSavingException, CacheCreationException {
    //            return data;
    //        }
    //    };
    //}
}
