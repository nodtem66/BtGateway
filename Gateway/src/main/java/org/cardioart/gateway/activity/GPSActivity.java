package org.cardioart.gateway.activity;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.cardioart.gateway.R;

public class GPSActivity extends FragmentActivity implements
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    public static final String REQUEST_LOCATION_UPDATE_KEY = "REQUEST_LOCATION_UPDATE_KEY";
    public static final String LOCATION_KEY = "LOCATION_KEY";

    private String serverAddress;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Location mCurrentLocation;
    private LocationRequest mLocationRequest;
    private boolean isRequestingLocationUpdates = false;
    private TextView mTextViewLat;
    private TextView mTextViewLong;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);
        serverAddress = getIntent().getStringExtra("server_ip");

        //Toast.makeText(GPSActivity.this, serverAddress, Toast.LENGTH_SHORT).show();
        mTextViewLat = (TextView) findViewById(R.id.textViewLat);
        mTextViewLong = (TextView) findViewById(R.id.textViewLong);

        // register Google Api Client and Location API
        buildGoogleApiClient();
        createLocationRequest();

        // register listener to scanning button
        Button buttonScan = (Button) findViewById(R.id.buttonScan);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationUpdates();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && isRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {

        }
        if (!isRequestingLocationUpdates) {
            startLocationUpdates();
        }
        Toast.makeText(GPSActivity.this, "connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(GPSActivity.this, "fail " + connectionResult.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUEST_LOCATION_UPDATE_KEY, isRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        updateUI();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        googleMap.setMyLocationEnabled(true);
        googleMap.setMapType(googleMap.MAP_TYPE_NORMAL);

        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        Toast.makeText(GPSActivity.this, "connecting Goole API...", Toast.LENGTH_SHORT).show();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        isRequestingLocationUpdates = true;
        Toast.makeText(GPSActivity.this, "requesting Location...", Toast.LENGTH_SHORT).show();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void updateUI() {
        mTextViewLat.setText(String.valueOf(mCurrentLocation.getLatitude()));
        mTextViewLong.setText(String.valueOf(mCurrentLocation.getLongitude()));
        LatLng position = new LatLng(mCurrentLocation.getLatitude(),
                mCurrentLocation.getLongitude());
        // Move the camera to show the marker.
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 14));
    }
}
