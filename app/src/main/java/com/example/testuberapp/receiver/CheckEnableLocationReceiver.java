package com.example.testuberapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

public class CheckEnableLocationReceiver extends BroadcastReceiver {


    private static LocationManager locManager;

    public static LocationReceiverListener locationReceiverListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isEnable = checkLocationEnable(context);

        if (locationReceiverListener != null) {
            locationReceiverListener.onLocationEnableChange(isEnable);
        }
    }

    public static boolean checkLocationEnable(Context context) {
        locManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return true;
        } else {
            return false;
        }
    }

    public static void setLocationReceiverListener(LocationReceiverListener listener){
        locationReceiverListener = listener;
    }

    public interface LocationReceiverListener {
        void onLocationEnableChange(boolean isEnable);
    }
}
