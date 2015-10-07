package org.cardioart.gateway.api.helper.gps;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by jirawat on 30/09/2015.
 */
public class GPSHelper extends GPSConnection implements
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Location currentLocation;
    private LocationRequest mLocationRequest;
    private boolean isRequestingLocationUpdates = false;
    private boolean isLocationUpdated = false;
    private String mStrError = "";

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (!isRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mStrError +=  " connection_suspended_" + i;
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        isLocationUpdated = true;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mStrError += " connection_failed_" + connectionResult.toString();
    }

    protected synchronized void buildGoogleApiClient(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    protected synchronized void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(2500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    // use android main activity as main context parameters
    public GPSHelper(Context mainContext) {
        // register Google API Client and Location service
        buildGoogleApiClient(mainContext);
        createLocationRequest();
    }

    public void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        isRequestingLocationUpdates = true;
    }

    public void stopLocationUpdates() {
        if (isRequestingLocationUpdates) {
            isRequestingLocationUpdates = false;
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    public String getError() {return mStrError;}
    public Location getGPSLocation() {
        isLocationUpdated = false;
        return currentLocation;
    }
    public boolean isLocationUpdated() {return isLocationUpdated;}
    public boolean isConnected() {return mGoogleApiClient.isConnected();}
    public boolean isLocationRequested() {return isRequestingLocationUpdates;}
}
