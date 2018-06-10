package com.example.abanoub.facebookloginmapslocationdirectionsexample;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    ImageView profile_imageView;
    TextView fullName_textView;
    EditText latitude_ED, longitude_ED;
    Button locate_Btn;
    //Maps
    GoogleMap mMap;
    private Marker marker;
    //Location
    FusedLocationProviderClient fusedLocationProviderClient;
    //Directions
    LatLng origin;
    Polyline polyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        String userID=getIntent().getExtras().getString("userID");
        String fullName=getIntent().getExtras().getString("fullName");

        profile_imageView=findViewById(R.id.imageView);
        fullName_textView=findViewById(R.id.textView);
        latitude_ED = findViewById(R.id.lat);
        longitude_ED = findViewById(R.id.lng);
        locate_Btn = findViewById(R.id.locate);

        Toast.makeText(this, "Login with facebook Succeed", Toast.LENGTH_LONG).show();
        fullName_textView.setText(fullName);
        Glide.with(this)
                .load("https://graph.facebook.com/" + userID+ "/picture?type=large")
                .into(profile_imageView);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getCurrentLocation();

        locate_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (TextUtils.isEmpty(latitude_ED.getText()) || TextUtils.isEmpty(longitude_ED.getText()))
                    Toast.makeText(MapsActivity.this, "Enter Lat & Lng first", Toast.LENGTH_SHORT).show();
                else {
                    if (marker != null)
                        marker.remove();
                    if (polyline != null)
                        polyline.remove();

                    double latitude;
                    double longitude;
                    try {
                        latitude = Double.parseDouble(latitude_ED.getText().toString());
                        longitude = Double.parseDouble(longitude_ED.getText().toString());

                        // Close keyboard if no view has focus:
                        View v = MapsActivity.this.getCurrentFocus();
                        if (v != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }

                        LatLng latLng = new LatLng(latitude, longitude);
                        marker = mMap.addMarker(new MarkerOptions().position(latLng).title("Marker (Lat: " + latitude
                                + ", Long: " + longitude + ")"));

                        //Directions
                        LatLng dest = new LatLng(latitude, longitude);
                        flyWithCameraTo(dest);

                        // Getting URL to the Google Directions API
                        String url = getUrl(origin, dest);
                        Log.d("url", url.toString());
                        FetchUrlTask FetchUrlTask = new FetchUrlTask();

                        // Start downloading json data from Google Directions API
                        FetchUrlTask.execute(url);
                    } catch (Exception e) {
                        Toast.makeText(MapsActivity.this, "Error! Enter valid location", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void flyWithCameraTo(LatLng target) {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(target, 8, 30, 0))
                , 3000, null);
    }

    private String getUrl(LatLng origin, LatLng dest) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin.latitude + "," + origin.longitude
                + "&destination=" + dest.latitude + "," + dest.longitude + "&key=AIzaSyAAA4CLh0umAUUB_IuQUlUoFTbd0kNf5Ts";
        return url;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true); //set mark at your location on the map
    }

    private void getCurrentLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //checking location settings on device
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(new LocationRequest());

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...

                if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(MapsActivity.this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Logic to handle location object
                            Toast.makeText(MapsActivity.this, "Current location:\nLat=" + location.getLatitude()
                                    + "\nLong=" + location.getLongitude(), Toast.LENGTH_LONG).show();
                            origin = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(origin));
                        }
                    }
                });
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        Toast.makeText(MapsActivity.this, "task.onfailure()", Toast.LENGTH_SHORT).show();

                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MapsActivity.this, 1);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Fetches data from url passed
    class FetchUrlTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... param) {

            URL url=null;
            HttpURLConnection connection =null;
            InputStream inputstream=null;
            InputStreamReader inputstreamreader = null;
            BufferedReader bufferedreader = null;
            String finalJson=null;

            try {
                url=new URL(param[0]);
                connection=(HttpURLConnection) url.openConnection();
                connection.connect();
                inputstream=connection.getInputStream();
                bufferedreader = new BufferedReader(new InputStreamReader(inputstream));
                StringBuffer buffer = new StringBuffer();
                String line ="";
                while ((line = bufferedreader.readLine()) != null){
                    buffer.append(line);
                }
                finalJson = buffer.toString();
                Log.i("doInBackground:" , finalJson);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return finalJson;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (!isNetworkAvailable())
                Toast.makeText(MapsActivity.this, "Enable network connection to get directions",
                        Toast.LENGTH_LONG).show();

            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    // Parsing fetched data
    class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask", jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask", "Executing routes");
                Log.d("ParserTask", routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            if (result != null) {
                ArrayList<LatLng> points;
                PolylineOptions lineOptions = null;

                // Traversing through all the routes
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList<>();
                    lineOptions = new PolylineOptions();

                    // Fetching i-th route
                    List<HashMap<String, String>> path = result.get(i);

                    // Fetching all the points in i-th route
                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);

                        points.add(position);
                    }

                    // Adding all the points in the route to LineOptions
                    lineOptions.addAll(points);
                    lineOptions.width(8);
                    lineOptions.color(Color.RED);

                    Log.d("onPostExecute", "onPostExecute lineoptions decoded");
                }

                // Drawing polyline in the Google Map for the i-th route
                if (lineOptions != null) {
                    polyline = mMap.addPolyline(lineOptions);
                } else {
                    Log.d("onPostExecute", "without Polylines drawn");
                }
            }
        }
    }
}

