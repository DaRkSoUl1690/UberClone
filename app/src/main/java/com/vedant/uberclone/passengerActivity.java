package com.vedant.uberclone;

import androidx.annotation.NonNull;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.vedant.uberclone.databinding.ActivityPassengerBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class passengerActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;
    private ActivityPassengerBinding binding;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private boolean isCarReady = false;
    private Button btnRequestCar;
    private Button btnBeep;
    private boolean isUberCancelled = true;
    int backButtonCount = 0;
    private Timer t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPassengerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        btnRequestCar = findViewById(R.id.btnRequestCar);
        btnRequestCar.setOnClickListener(this);

        btnBeep = findViewById(R.id.btnBeepBeep);
        btnBeep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

           getDriverUpdates();
            }
        });

        ParseQuery<ParseObject> carRequestQuery = ParseQuery.getQuery("RequestCar");
        carRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        carRequestQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (objects.size() > 0 && e == null) {

                    isUberCancelled = false;
                    btnRequestCar.setText("Cancel your uber request!");

                    getDriverUpdates();
                }
            }
        });


        findViewById(R.id.btnLogoutFromPassengerActivity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ParseUser.logOutInBackground(new LogOutCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            finish();

                        }
                    }
                });
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                updateCameraPassengerLocation(location);


            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };


        if (Build.VERSION.SDK_INT < 23)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            } else if (Build.VERSION.SDK_INT >= 23) {

                if (ContextCompat.checkSelfPermission(passengerActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(passengerActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);


                } else {

                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);

                    Location currentPassengerLocation =
                            mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateCameraPassengerLocation(currentPassengerLocation);


                }
            }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(passengerActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);

                Location currentPassengerLocation =
                        mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateCameraPassengerLocation(currentPassengerLocation);

            }
        }

    }

    private void updateCameraPassengerLocation(Location pLocation) {


        if (!isCarReady) {
            LatLng passengerLocation = new LatLng(pLocation.getLatitude(), pLocation.getLongitude());
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(passengerLocation, 15));

            mMap.addMarker(new MarkerOptions().position(passengerLocation).title("You are here!!!").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }
    }

    @Override
    public void onClick(View view) {

        if (isUberCancelled) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
                Location passengerCurrentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (passengerCurrentLocation != null) {

                    ParseObject requestCar = new ParseObject("RequestCar");
                    requestCar.put("username", ParseUser.getCurrentUser().getUsername());

                    ParseGeoPoint userLocation = new ParseGeoPoint(passengerCurrentLocation.getLatitude(), passengerCurrentLocation.getLongitude());
                    requestCar.put("passengerLocation", userLocation);

                    requestCar.saveInBackground(e -> {

                        if (e == null) {

                            Toast.makeText(passengerActivity.this, "A car request is sent",
                                    Toast.LENGTH_SHORT).show();

                            btnRequestCar.setText("Cancel your uber order");
                            isUberCancelled = false;


                        }
                    });

                } else {

                    Toast.makeText(this, "Unknown Error. Something went wrong!!!", Toast.LENGTH_SHORT).show();

                }
            }
        } else {


            ParseQuery<ParseObject> carRequestQuery = ParseQuery.getQuery("RequestCar");
            carRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            carRequestQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> requestList, ParseException e) {

                    if (requestList.size() > 0 && e == null) {

                        isUberCancelled = true;
                        btnRequestCar.setText("Request a new uber");

                        for (ParseObject uberRequest : requestList) {

                            uberRequest.deleteInBackground(e1 -> {

                                if (e1 == null) {
                                    Toast.makeText(passengerActivity.this, "Request/s deleted", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    }
                }
            });

        }

    }


    private void getDriverUpdates() {

        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                ParseQuery<ParseObject> uberRequestQuery = ParseQuery.getQuery("RequestCar");
                uberRequestQuery.whereEqualTo("username",
                        ParseUser.getCurrentUser().getUsername());
                uberRequestQuery.whereEqualTo("requestAccepted", true);
                uberRequestQuery.whereExists("driverOfMe");

                uberRequestQuery.findInBackground((objects, e) -> {

                    if (objects.size() > 0 && e == null) {

                        isCarReady = true;
                        for (final ParseObject requestObject : objects) {

                            ParseQuery<ParseUser> driverQuery = ParseUser.getQuery();
                            driverQuery.whereEqualTo("username", requestObject.getString("driverOfMe"));
                            driverQuery.findInBackground((drivers, e1) -> {
                                if (drivers.size() > 0 && e1 == null) {

                                    for (ParseUser driverOfRequest : drivers) {

                                        ParseGeoPoint driverOfRequestLocation = driverOfRequest.getParseGeoPoint("driverLocation");
                                        if (ContextCompat.checkSelfPermission(passengerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                            Location passengerLocation =
                                                    mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                                            ParseGeoPoint pLocationAsParseGeoPoint = new ParseGeoPoint(passengerLocation.getLatitude(), passengerLocation.getLongitude());

                                            assert driverOfRequestLocation != null;
                                            double milesDistance = driverOfRequestLocation.distanceInMilesTo(pLocationAsParseGeoPoint);

                                            if (milesDistance < 0.3) {


                                                requestObject.deleteInBackground(e11 -> {
                                                    if (e11 == null) {

                                                        Toast.makeText(passengerActivity.this, "Your Uber is ready!", Toast.LENGTH_LONG).show();
                                                        isCarReady = false;
                                                        isUberCancelled = true;
                                                        btnRequestCar.setText("You can order a new uber now!");
                                                    }
                                                });

                                            } else {

                                                float roundedDistance = Math.round(milesDistance * 10) / 10;
                                                Toast.makeText(passengerActivity.this,
                                                        requestObject.getString("driverOfMe") + " is " + roundedDistance + " miles away from you!- Please wait!!!", Toast.LENGTH_LONG).show();


                                                LatLng dLocation = new LatLng(driverOfRequestLocation.getLatitude(),
                                                        driverOfRequestLocation.getLongitude());


                                                LatLng pLocation = new LatLng(pLocationAsParseGeoPoint.getLatitude(), pLocationAsParseGeoPoint.getLongitude());

                                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                                Marker driverMarker = mMap.addMarker(new MarkerOptions().position(dLocation).title("Driver Location"));
                                                Marker passengerMarker = mMap.addMarker(new MarkerOptions().position(pLocation));

                                                ArrayList<Marker> myMarkers = new ArrayList<>();
                                                myMarkers.add(driverMarker);
                                                myMarkers.add(passengerMarker);

                                                for (Marker marker : myMarkers) {

                                                    builder.include(marker.getPosition());

                                                }

                                                LatLngBounds bounds = builder.build();

                                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 80);
                                                mMap.animateCamera(cameraUpdate);
                                            }

                                        }

                                    }

                                }
                            });


                        }
                    } else {
                        isCarReady = false;
                    }
                });

            }

        }, 0, 3000);


    }

}
