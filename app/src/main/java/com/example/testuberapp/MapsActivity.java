package com.example.testuberapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.testuberapp.receiver.CheckEnableLocationReceiver;
import com.example.testuberapp.receiver.ConnectionReceiver;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.CancelableCallback,
        ConnectionReceiver.ConnectionReceiverListener,
        CheckEnableLocationReceiver.LocationReceiverListener {

    private static final String TAG = "MapsActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final float MIN_ZOOM = 9.5f;
    private static final float MAX_ZOOM = 17f;

    private Boolean mLocationPermissionGranted = false;
    private Boolean locationEnable = false;
    private Boolean networkEnable = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private ViewGroup viewGroup;
    private LocationCallback locationDeviceCallback;
    private HandlerThread handlerThread;

    //widgets
    private ImageView mGps;
    private DrawerLayout drawerLayout;
    private ImageView mBtnNavDrawer;
    private Snackbar snackbarNetwork;
    private Snackbar snackbarLocation;
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        init();
        getLocationPermission();
        checkConnection(ConnectionReceiver.isConnected(getApplicationContext()));
        checkEnableLocation(CheckEnableLocationReceiver.checkLocationEnable(getApplicationContext()));

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConnection(ConnectionReceiver.isConnected(getApplicationContext()));
        checkEnableLocation(CheckEnableLocationReceiver.checkLocationEnable(getApplicationContext()));
        getDeviceLocation();
    }

    private void init() {
        mGps = (ImageView) findViewById(R.id.ic_gps);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mBtnNavDrawer = (ImageView) findViewById(R.id.btn_nav_drawer);
        viewGroup = findViewById(android.R.id.content);

        snackbarNetwork = Snackbar.make(viewGroup, "Network is disconnection", Snackbar.LENGTH_INDEFINITE)
                .setBackgroundTint(Color.RED)
                .setAction("Action", null);
        View view = snackbarNetwork.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);

        snackbarLocation = Snackbar.make(viewGroup, "Location is disable", Snackbar.LENGTH_INDEFINITE)
                .setBackgroundTint(Color.GRAY)
                .setAction("Action", null);

        ConnectionReceiver.setConnectionReceiverListener(this);
        CheckEnableLocationReceiver.setLocationReceiverListener(this);

        handlerThread = new HandlerThread("Location");

        locationDeviceCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.d(TAG, "onLocationResult: return location if have location");
                Location currentLocation = (Location) locationResult.getLastLocation();
                if (currentLocation != null) {
                    moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                            DEFAULT_ZOOM);
                }
            }
        };

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });
        mGps.setVisibility(locationEnable ? View.VISIBLE : View.INVISIBLE);


        mBtnNavDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked button navigation drawer");
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        });

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        try {
            if (mLocationPermissionGranted) {
                mFusedLocationProviderClient.requestLocationUpdates(LocationRequest.create(),
                        locationDeviceCallback,
                        handlerThread.getLooper());
            }

        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }


    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng:" + latLng.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), this);
    }

    private void initMap() {
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }


    private void getLocationPermission() {
        String[] permissions = new String[]{FINE_LOCATION, COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(getApplicationContext(), FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false;
                            return;
                        }
                    }
                    mLocationPermissionGranted = true;
                    initMap();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        initMapUiSetting();
        if (mLocationPermissionGranted) {
            getDeviceLocation();
            if (ContextCompat.checkSelfPermission(this, FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
    }


    private void initMapUiSetting() {
        mMap.setMaxZoomPreference(MAX_ZOOM);
        mMap.setMinZoomPreference(MIN_ZOOM);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                if (locationEnable)
                    mGps.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onFinish() {
        mGps.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCancel() {
        if (locationEnable)
            mGps.setVisibility(View.VISIBLE);
    }


    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        checkConnection(isConnected);
    }

    private void checkConnection(boolean isConnected) {
        if (!isConnected) {
            networkEnable = false;
            snackbarNetwork.show();
        } else {
            networkEnable = true;
            snackbarNetwork.dismiss();
        }
    }


    @Override
    public void onLocationEnableChange(boolean isEnable) {
        checkEnableLocation(isEnable);
    }

    private void checkEnableLocation(boolean isEnable) {
        if (!isEnable) {
            locationEnable = false;
            snackbarLocation.show();
            showMap();
        } else {
            locationEnable = true;
            snackbarLocation.dismiss();
            showMap();
        }
    }

    private void showMap() {
        if (!locationEnable && !networkEnable) {
            getSupportFragmentManager().beginTransaction().hide(mapFragment).commit();
            mGps.setVisibility(View.INVISIBLE);
        } else {
            getSupportFragmentManager().beginTransaction().show(mapFragment).commit();
            getDeviceLocation();
            checkConnection(ConnectionReceiver.isConnected(getApplicationContext()));
        }
    }
}
