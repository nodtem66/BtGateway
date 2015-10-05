package org.cardioart.gateway.api.helper.gps;

import android.location.Location;

/**
 * Created by jirawat on 30/09/2015.
 */
public abstract class GPSConnection {
    abstract public void startLocationUpdates();
    abstract public void stopLocationUpdates();
    abstract public String getError();
    abstract public boolean isLocationUpdated();
    abstract public boolean isLocationRequested();
    abstract public boolean isConnected();
    abstract public Location getGPSLocation();
}
