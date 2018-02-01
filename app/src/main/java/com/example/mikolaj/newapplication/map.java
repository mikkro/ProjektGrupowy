/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mikolaj.newapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.app.PendingIntent.getActivity;

//import android.location.LocationListener;
//import com.google.android.gms.location.LocationListener;

/**
 * This shows how to create a simple activity with a map and a marker on the map.
 */
public class map extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private ArrayList<Polygon> polygonList = new ArrayList<>();
    private GoogleMap mMap;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private Marker curreLocationMarker;
    public static final int REQUEST_LOCATION_CODE = 99;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.setThreadPolicy((new StrictMode.ThreadPolicy.Builder().permitNetwork().build()));

        DownloadDataBase.getData1(DownloadDataBase.address);
        DownloadDataBase.getData1(DownloadDataBase.URLDistriction);
        DownloadDataBase.getData1(DownloadDataBase.URLborderPoints);
        DownloadDataBase.splitOffenseData(DownloadDataBase.offenses);

        setContentView(R.layout.activity_map); // ustawienie widoku na jakiś taki z xml

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
        {
            checkLocationPermission();
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map); // pobranie tej klasy w widoku
        mapFragment.getMapAsync(this); // pobranie async tej mapy
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_LOCATION_CODE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    //permission granted
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    ==PackageManager.PERMISSION_GRANTED)
                    {
                        if(client==null)
                        {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else    //permission denied
                {
                    Toast.makeText(this, "Permission Denied!",Toast.LENGTH_LONG)
                            .show();
                }
        }

    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we
     * just add a marker near Africa.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //mMap.setOnMapClickListener(this);
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                ==PackageManager.PERMISSION_GRANTED)
        {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
            @Override
            public void onPolygonClick(Polygon polygon) {
                // Flip the red, green and blue components of the polygon's stroke color.
//                polygon.setStrokeColor(polygon.getStrokeColor() ^ 0x00ffffff);
//                polygon.setFillColor(polygon.getFillColor()^ 0x00ffffff);
//                polygon.setStrokeColor(0x800000FF);
//                polygon.setFillColor(0x800000FF);
                String description=null;
                int counter=0;
                for (int i = 0; i < DownloadDataBase.districts.size(); i++) {
                        if (DownloadDataBase.districts.get(i).getPolygon().equals(polygon)) {
                            description = DownloadDataBase.districts.get(i).getDistrictName();
                        }
                    }
                for (int i = 0; i < DownloadDataBase.offenses.size(); i++) {
                    if (DownloadDataBase.offenses.get(i).getDistrict().equals(description)) {
                        counter++;
                    }
                }
                    android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(
                            map.this);
                    adb.setTitle(description);
                    adb.setMessage("Zanotowano "+ counter + " wykroczeń" + " w dzielnicy " + description);
                    adb.setPositiveButton("Ok", null);
                    adb.show();


            }

        });
        //googleMap.setOnMapClickListener(this);


    }

    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.B_search:
                {
                EditText tf_location = (EditText)findViewById(R.id.TF_location);
                String location = tf_location.getText().toString();
                List<Address> addressList = null;
                MarkerOptions mo = new MarkerOptions();

                if(!location.equals(""))
                {
                    Geocoder geocoder = new Geocoder(this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 5);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    for(int i=0; i<addressList.size();i++)
                    {
                        Address myAddress = addressList.get(i);
                        LatLng latlng = new LatLng(myAddress.getLatitude(), myAddress.getLongitude());
                        mo.position(latlng);
                        mo.title("Your search result");
                        mMap.addMarker(mo);
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(latlng));
                    }

                }
                break;
                }
        case R.id.B_clear:
                {
                mMap.clear();
                break;
                }

        case R.id.B_morderstwa:
                {
                for (offense sthHappened: DownloadDataBase.zabojstwo) {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(sthHappened.getPosition_longitude(),sthHappened.getPosition_latitude() ))
                            .title(sthHappened.getDescription()));
                }
                break;
                }

        case R.id.B_porwania:
                {
                for (offense sthHappened: DownloadDataBase.porwanie) {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(sthHappened.getPosition_longitude(),sthHappened.getPosition_latitude() ))
                            .title(sthHappened.getDescription()));
                }
                break;
                }
        case R.id.B_przeklinanie:
                {
                for (offense sthHappened: DownloadDataBase.glosne_przeklinanie) {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(sthHappened.getPosition_longitude(),sthHappened.getPosition_latitude() ))
                            .title(sthHappened.getDescription()));
                }
                break;
                }
        case R.id.B_przemoc:
                {
                    for (offense sthHappened: DownloadDataBase.przemoc_domowa) {
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(sthHappened.getPosition_longitude(),sthHappened.getPosition_latitude() ))
                                .title(sthHappened.getDescription()));
                    }
                    break;
                }
        case R.id.B_showD:
                {
                    final int COLOR_GREEN = 0x80009900;
                    final int COLOR_YELLOW = 0x80FFFF00;
                    final int COLOR_ORANGE = 0x80FFF800;
                    final int COLOR_RED = 0x80CC0000;
                    int color = 0;
                    String dName=null;
                    int dCounter;
                    for(int i=0; i<DownloadDataBase.districts.size(); i++) {
                        PolygonOptions opts = new PolygonOptions();
                        //Polygon polygon = mMap.addPolygon(opts);

                        for (LatLng location : DownloadDataBase.districts.get(i).getList())
                        {
                            opts.add(location);
                        }

                        dName = DownloadDataBase.districts.get(i).getDistrictName();
                        dCounter = 0;
                        for (int j = 0; j < DownloadDataBase.offenses.size(); j++) {
                            if (DownloadDataBase.offenses.get(j).getDistrict().equals(dName)) {
                                dCounter++;
                            }

                        }
                        if(dCounter==0){
                            color = COLOR_GREEN;
                        }else if(dCounter>=1 && dCounter<=3){
                            color = COLOR_YELLOW;
                        }else if(dCounter>3 && dCounter<=6){
                            color = COLOR_ORANGE;
                        }else if(dCounter>6){
                            color = COLOR_RED;
                        }
                        Polygon polygon = mMap.addPolygon(opts
                                .strokeColor(Color.RED)
                                .fillColor(color)     //przezroczystosc trza ustawic!
                                .strokeWidth(1)
                        );
                        DownloadDataBase.districts.get(i).addPolygon(polygon);
                        polygon.setClickable(true);
                        polygonList.add(polygon);
                    }
                break;
                }
        }

//        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
//            public void onPolygonClick(Polygon polygon) {
//
//                //mClusterManager = new ClusterManager<MyItem>(getApplicationContext(), map());
//                //map().setOnCameraChangeListener(mClusterManager);
//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mMap.getCameraPosition().target, mMap.getCameraPosition().zoom));
//
//
//
//            }
//        });
    }
        /*////////////////////////do usuniecia
        LatLng gdansk = new LatLng(54.22,18.38);

        mMap.addMarker(new MarkerOptions().position(gdansk).title("Marker")); // dodanie nowego markera
        // tutaj tak samo mozna dodawać listenery do mapy
        mMap.moveCamera(CameraUpdateFactory.newLatLng(gdansk));
        ////////////////////////az dotad.**/


    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        client.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;

        if(curreLocationMarker!=null)
        {
            curreLocationMarker.remove();
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Location");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        curreLocationMarker = mMap.addMarker(markerOptions);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(10));

        if(client!=null)
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
        }

    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();

        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
        }
    }

    public boolean checkLocationPermission()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                !=PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION))
            {
                ActivityCompat.requestPermissions(this, new String[]
                        {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            }
            else
            {
                ActivityCompat.requestPermissions(this, new String[]
                        {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            }
            return false;
        }
        return true;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


}
