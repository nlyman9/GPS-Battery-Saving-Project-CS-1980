package com.gps.gpsoptimizationproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapquest.navigation.NavigationManager;
import com.mapquest.navigation.dataclient.RouteService;
import com.mapquest.navigation.location.LocationProviderAdapter;
import com.mapquest.navigation.model.Shape;
import com.mapquest.navigation.model.location.Destination;

import com.mapquest.navigation.dataclient.listener.RoutesResponseListener;
import com.mapquest.navigation.model.RouteOptionType;
import com.mapquest.navigation.model.SystemOfMeasurement;
import com.mapquest.navigation.model.location.Coordinate;
import com.mapquest.navigation.model.Route;
import com.mapquest.navigation.model.RouteLeg;
import com.mapquest.navigation.model.RouteOptions;

import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    // Standard deviation of time for the GPS subsystem to startup and acquire a lock on a satellite.
    final float GPS_STARTUP_TIME = 36.59f;

    // Amount of time the GPS needs to be off before we save any power.
    final float POWER_SAVING_THRESHOLD_TIME = 10f;

    // Radius of how close is considered "arrived at" a location.
    final float ACCEPTABLE_DISTANCE_RADIUS = 80;

    // Permission magic number for location.
    private static final int LOCATION_REQUEST = 1400;

    // TODO: What does this do?
    private static final String TAG = "MyActivity";

    // Specifies who the route is going to be created for (Stephen, Matt, Mosse, driver, test).
    final String user = "matt";

    // Declaring variables for use down below.
    Location currentLocation = new Location("");
    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;

    // TextViews
    TextView velocityDisplay;
    TextView distanceDisplay;
    TextView timeDisplay;

    // Represents the current point in our navigation list.
    Location currentDestination;

    //MapQuest Variables
    LocationProviderAdapter mLocationProviderAdapter;
    RouteService mRouteService;
    NavigationManager mNavigationManager;
    List<Destination> dest = new ArrayList<>();

    // Variable used to calculate the distance from GPS turning on to the previous point.
    Location previous = new Location("");
    // Setting the below value to true indicates we are only logging or modulating.
    boolean logging = false;
    // ArrayLists to store our Location objects to describe the route we are navigating.
    ArrayList<Location> staticRoute;
    ArrayList<Location> dynamicRoute;

    int ListI = 0;

    // State variables and flags used for navigation.
    boolean isInNavigationMode = false;
    boolean shouldLogCurrentLocation = false;
    boolean shouldTestOvershot = false;
    boolean firstOvershot = false;
    Marker destMarker;
    File logfile = null;
    // FileOutputStream used for logging.
    FileOutputStream loggingFileOutputStream = null;
    String beforeEnable = "";
    Runnable GPSOnRunnable = null;
    Handler handler = new Handler();
    float firstDistance;
    // Object used to get battery level
    BatteryManager batteryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate(savedInstanceState);
        //check if we have location permissions
        if (!canAccessLocation()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        }
        //System.out.println("Entered Maps Activity Successfully");
        currentLocation.setLongitude(50);
        currentLocation.setLatitude(-80);

        setContentView(R.layout.activity_maps);
        //Declare the textboxs within the context of the code
        velocityDisplay = findViewById(R.id.VelocityView);
        distanceDisplay = findViewById(R.id.DistanceView);
        timeDisplay = findViewById(R.id.TimeView);

        try {
            logfile = new File(getApplicationContext().getFilesDir() + "/GPSLog.txt");
            loggingFileOutputStream = new FileOutputStream(logfile, true);
            Date now = new Date();
            int batLevel = getBatteryLevel();
            String logString = "APPSTART | " + now.toString() + " | " + batLevel + "\n";
            loggingFileOutputStream.write(logString.getBytes());
        } catch(FileNotFoundException e) {
            try {
                logfile.createNewFile();
                loggingFileOutputStream = new FileOutputStream(logfile);
            } catch(Exception ex) {

            }
        } catch(Exception ef) {

        }

        // instantiate BatteryManager object
        batteryManager = (BatteryManager)getSystemService(BATTERY_SERVICE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        FloatingActionButton distbut = findViewById(R.id.dist);
        staticRoute = new ArrayList<Location>();
        dynamicRoute = new ArrayList<Location>();
        Bundle extras = getIntent().getExtras();
        boolean select = extras.getBoolean("routeSelect");
        //create a dynamic route via MapQuest
        getMapQuestRoute();
        //This creates routes not via MapQuest
        if(select == true) {
            createHomeRoute();
        } else {
            createPittRoute();
        }

        //Setup the listener for the floating button
        distbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            isInNavigationMode = !isInNavigationMode;
            if(isInNavigationMode) {
                try {
                    //Log when the user starts the navigational app
                    Date now = new Date();
                    int batLevel = getBatteryLevel();
                    String logString = "NAV_START | " + now.toString() + " | " + batLevel + "%" + "\n";
                    loggingFileOutputStream.write("-------------------------------------------------------\n".getBytes());
                    loggingFileOutputStream.write(logString.getBytes());
                } catch(Exception e) {
                    velocityDisplay.setText(e.getMessage());
                }
                //We started navigation - so start a new run
                ListI = 0;
                distanceDisplay.setText("0 m");
                timeDisplay.setText("0 s");
                //Acquire the new destination
                currentDestination = staticRoute.get(ListI);
                //Add it to the map
                LatLng destLL = new LatLng(currentDestination.getLatitude(), currentDestination.getLongitude());
                MarkerOptions destMarkerOptions = new MarkerOptions().position(destLL).title("Next Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                //Add it to the map and save it in an object that we can use to remove it
                destMarker = mMap.addMarker(destMarkerOptions);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(destLL));
            } else {
                //We disabled navigation - remove the current marker
                destMarker.remove();
                distanceDisplay.setText("");
                timeDisplay.setText("");
                try {
                    //Log that the user stopped navigation manually
                    Date now = new Date();
                    int batLevel = getBatteryLevel();
                    String logString = "NAV_STOP (MANUAL) | " + now.toString() + " | " + batLevel + "%" + "\n";
                    loggingFileOutputStream.write(logString.getBytes());
                    loggingFileOutputStream.write("-------------------------------------------------------\n".getBytes());
                } catch(Exception e) {
                        velocityDisplay.setText(e.getMessage());
                }
            }
            }
        });
        setNewLocationListener();
        //Declare a location Manager
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        try {
            //LocM.requestSingleUpdate(LocationManager.GPS_PROVIDER, null);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        } catch (SecurityException e) {
            velocityDisplay.setText(e.getMessage());
        }

        // Creates a new Runnable that will turn the GPS back on for us when invoked.
        GPSOnRunnable = new Runnable() {
            @Override
            public void run() {
                if(logging == false) {
                    turnGPSOn(getApplicationContext());
                } else {
                    logGPSOn();
                }
            }
        };
    }

    private void getMapQuestRoute() {
        //Boilerplate Objects
        mRouteService = new RouteService.Builder().build(getApplicationContext(), BuildConfig.API_KEY);
        mLocationProviderAdapter = ((MQNavigationSampleApplication) getApplication()).getLocationProviderAdapter();
        //This wasn't working and we don't need a navigation manager in this case anyways, because we are just generating a route between two coordinates
        //mNavigationManager = new NavigationManager.Builder(this, BuildConfig.API_KEY, mLocationProviderAdapter).build();

        // Set up start and destination for the route
        //TODO convert the start coordinate to a current location access
        //This startCoordinate is in nyc
        Coordinate startCoordinate = new Coordinate(40.7326808, -73.9843407);
        //This endCoordinate is in boston
        Coordinate endCoordinate = new Coordinate(42.355097, -71.055464);
        //Add the end coordinate to the destination list
        dest.add(new Destination(endCoordinate, null));
        // Set up route options
        RouteOptions routeOptions = new RouteOptions.Builder()
                .maxRoutes(3)
                .systemOfMeasurementForDisplayText(SystemOfMeasurement.UNITED_STATES_CUSTOMARY) // or specify METRIC
                .language("en_US") // NOTE: alternately, specify "es_US" for Spanish in the US
                .highways(RouteOptionType.ALLOW)
                .tolls(RouteOptionType.ALLOW)
                .ferries(RouteOptionType.DISALLOW)
                .internationalBorders(RouteOptionType.DISALLOW)
                .unpaved(RouteOptionType.DISALLOW)
                .seasonalClosures(RouteOptionType.AVOID)
                .build();
        RoutesResponseListener responseListener = new RoutesResponseListener() {
            //This retrieves the route
            @Override
            public void onRoutesRetrieved(@NonNull List<Route> routes) {
                if (routes.size() > 0) {
                    //Don't start the navigation, because we don't need to
                    //mNavigationManager.startNavigation((Route) routes.get(0));
                    Route mRoute = routes.get(0);
                    List<RouteLeg> legs = mRoute.getLegs();
                    //System.out.println("Length of legs: " + legs.size());
                    RouteLeg onlyLeg = legs.get(0);
                    Shape s = onlyLeg.getShape();
                    //This gets the coordinate list
                    List<Coordinate> coords = s.getCoordinates();
                    System.out.println("There are: " + coords.size() + "coordinates in this route");
                    // Print out the list of coordinates for the route from nyc to boston.
                    // Almost 4,000(!!!) coordinates returned from this
                    // It does not return a coordinate for each turn, clearly

                    for (Coordinate coord : coords) {
                        Location newLoc = new Location("");
                        newLoc.setLatitude(coord.getLatitude());
                        newLoc.setLongitude(coord.getLongitude());
                        dynamicRoute.add(newLoc);
                    }

                }
                else {
                    System.out.println("No routes found for these coordinates");
                }
            }

            @Override
            public void onRequestFailed(@Nullable Integer integer, @Nullable IOException e) {

            }

            @Override
            public void onRequestMade() {

            }
        };
        mRouteService.requestRoutes(startCoordinate, dest, routeOptions, responseListener);
    }

    //creates route to Pitt
    private void createPittRoute() {
        if(user.equals("test")) {
            Location one = new Location("");
            one.setLatitude(40.443162);
            one.setLongitude(-79.953543);

            Location two = new Location("");
            two.setLatitude(40.443918);
            two.setLongitude(-79.950732);

            Location three = new Location("");
            three.setLatitude(40.445065);
            three.setLongitude(-79.951263);

            Location four = new Location("");
            four.setLatitude(40.445179);
            four.setLongitude(-79.950357);

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
        } else if(user.equals("mosse")) {
            Location one = new Location("");
            one.setLatitude(40.473427);
            one.setLongitude(-79.911090);

            Location two = new Location("");
            two.setLatitude(40.470195);
            two.setLongitude(-79.911648);

            Location three = new Location("");
            three.setLatitude(40.469281);
            three.setLongitude(-79.912592);

            Location four = new Location("");
            four.setLatitude(40.468563);
            four.setLongitude(-79.916626);

            Location five = new Location("");
            five.setLatitude(40.468800);
            five.setLongitude(-79.917835);

            Location six = new Location("");
            six.setLatitude(40.460807);
            six.setLongitude(-79.922612);

            Location seven = new Location("");
            seven.setLatitude(40.460575);
            seven.setLongitude(-79.923014);

            Location eight = new Location("");
            eight.setLatitude(40.459226);
            eight.setLongitude(-79.922089);

            Location nine = new Location("");
            nine.setLatitude(40.459039);
            nine.setLongitude(-79.924525);

            Location ten = new Location("");
            ten.setLatitude(40.458368);
            ten.setLongitude(-79.927332);

            Location eleven = new Location("");
            eleven.setLatitude(40.456369);
            eleven.setLongitude(-79.930510);

            Location twelve = new Location("");
            twelve.setLatitude(40.455084);
            twelve.setLongitude(-79.931894);

            Location thirteen = new Location("");
            thirteen.setLatitude(40.452736);
            thirteen.setLongitude(-79.936728);

            Location fourteen = new Location("");
            fourteen.setLatitude(40.451297);
            fourteen.setLongitude(-79.940955);

            Location fifteen = new Location("");
            fifteen.setLatitude(40.450489);
            fifteen.setLongitude(-79.945032);

            Location sixteen = new Location("");
            sixteen.setLatitude(40.447974);
            sixteen.setLongitude(-79.947687);

            Location seventeen = new Location("");
            seventeen.setLatitude(40.447345);
            seventeen.setLongitude(-79.947440);

            Location eighteen = new Location("");
            eighteen.setLatitude(40.447067);
            eighteen.setLongitude(-79.947634);

            Location nineteen = new Location("");
            nineteen.setLatitude(40.446716);
            nineteen.setLongitude(-79.951474);

            Location twenty = new Location("");
            twenty.setLatitude(40.442517);
            twenty.setLongitude(-79.957461);

            Location twentyone = new Location("");
            twentyone.setLatitude(40.441831);
            twentyone.setLongitude(-79.956195);

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
            staticRoute.add(five);
            staticRoute.add(six);
            staticRoute.add(seven);
            staticRoute.add(eight);
            staticRoute.add(nine);
            staticRoute.add(ten);
            staticRoute.add(eleven);
            staticRoute.add(twelve);
            staticRoute.add(thirteen);
            staticRoute.add(fourteen);
            staticRoute.add(fifteen);
            staticRoute.add(sixteen);
            staticRoute.add(seventeen);
            staticRoute.add(eighteen);
            staticRoute.add(nineteen);
            staticRoute.add(twenty);
            staticRoute.add(twentyone);
        } else if(user.equals("driver")) {
            Location one = new Location("");
            one.setLatitude(40.505380);
            one.setLongitude(-79.924719);

            Location two = new Location("");
            two.setLatitude(40.504662);
            two.setLongitude(-79.923664);

            Location three = new Location("");
            three.setLatitude(40.504288);
            three.setLongitude(-79.923376);

            Location four = new Location("");
            four.setLatitude(40.504687);
            four.setLongitude(-79.922622);

            Location five = new Location("");
            five.setLatitude(40.505061);
            five.setLongitude(-79.922419);

            Location six = new Location("");
            six.setLatitude(40.504802);
            six.setLongitude( -79.920952);

            Location seven = new Location("");
            seven.setLatitude(40.504841);
            seven.setLongitude(-79.920338);

            Location eight = new Location("");
            eight.setLatitude(40.505441);
            eight.setLongitude(-79.918783);

            Location nine = new Location("");
            nine.setLatitude(40.506032);
            nine.setLongitude(-79.918394);

            Location ten = new Location("");
            ten.setLatitude(40.506156);
            ten.setLongitude(-79.918000);

            Location eleven = new Location("");
            eleven.setLatitude(40.506066);
            eleven.setLongitude(-79.917633);

            Location twelve = new Location("");
            twelve.setLatitude(40.504010);
            twelve.setLongitude(-79.919374);

            Location thirteen = new Location("");
            thirteen.setLatitude(40.503504);
            thirteen.setLongitude(-79.920034);

            Location fourteen = new Location("");
            fourteen.setLatitude(40.502960);
            fourteen.setLongitude(-79.920918);

            Location fifteen = new Location("");
            fifteen.setLatitude(40.502262);
            fifteen.setLongitude(-79.921538);

            Location sixteen = new Location("");
            sixteen.setLatitude(40.501555);
            sixteen.setLongitude(-79.922884);

            Location seventeen = new Location("");
            seventeen.setLatitude(40.501037);
            seventeen.setLongitude(-79.923600);

            Location eighteen = new Location("");
            eighteen.setLatitude(40.500848);
            eighteen.setLongitude(-79.924062);

            Location nineteen = new Location("");
            nineteen.setLatitude(40.500673);
            nineteen.setLongitude(-79.924225);

            Location twenty = new Location("");
            twenty.setLatitude(40.497297);
            twenty.setLongitude(-79.925425);

            Location twentyone = new Location("");
            twentyone.setLatitude(40.496571);
            twentyone.setLongitude(-79.926633);

            Location twentytwo = new Location("");
            twentytwo.setLatitude(40.496299);
            twentytwo.setLongitude(-79.926580);

            Location twentythree = new Location("");
            twentythree.setLatitude(40.496402);
            twentythree.setLongitude(-79.925995);

            Location twentyfour = new Location("");
            twentyfour.setLatitude(40.496427);
            twentyfour.setLongitude(-79.925399);

            Location twentyfive = new Location("");
            twentyfive.setLatitude(40.494646);
            twentyfive.setLongitude(-79.925290);

            Location twentysix = new Location("");
            twentysix.setLatitude(40.494748);
            twentysix.setLongitude(-79.923533);

            Location twentyseven = new Location("");
            twentyseven.setLatitude(40.493134);
            twentyseven.setLongitude( -79.911484);

            Location twentyeight = new Location("");
            twentyeight.setLatitude(40.492776);
            twentyeight.setLongitude(-79.910667);

            Location twentynine = new Location("");
            twentynine.setLatitude(40.492384);
            twentynine.setLongitude(-79.910490);

            Location thirty = new Location("");
            thirty.setLatitude(40.486523);
            thirty.setLongitude(-79.913340);

            Location thirtyone = new Location("");
            thirtyone.setLatitude(40.486087);
            thirtyone.setLongitude(-79.913620);

            Location thirtytwo = new Location("");
            thirtytwo.setLatitude(40.486271);
            thirtytwo.setLongitude(-79.914975);

            Location thirtythree = new Location("");
            thirtythree.setLatitude(40.486503);
            thirtythree.setLongitude(-79.915159);

            Location thirtyfour = new Location("");
            thirtyfour.setLatitude(40.486570);
            thirtyfour.setLongitude(-79.914765);

            Location thirtyfive = new Location("");
            thirtyfive.setLatitude(40.485221);
            thirtyfive.setLongitude(-79.909418);

            Location thirtysix = new Location("");
            thirtysix.setLatitude(40.484810);
            thirtysix.setLongitude(-79.908489);

            Location thirtyseven = new Location("");
            thirtyseven.setLatitude(40.484438);
            thirtyseven.setLongitude(-79.908190);

            Location thirtyeight = new Location("");
            thirtyeight.setLatitude(40.483698);
            thirtyeight.setLongitude(-79.907904);

            Location thirtynine = new Location("");
            thirtynine.setLatitude(40.482504);
            thirtynine.setLongitude(-79.908044);

            Location forty = new Location("");
            forty.setLatitude(40.480646);
            forty.setLongitude(-79.907841);

            Location fortyone = new Location("");
            fortyone.setLatitude(40.479302);
            fortyone.setLongitude(-79.907847);

            Location fortytwo = new Location("");
            fortytwo.setLatitude(40.471237);
            fortytwo.setLongitude(-79.908559);

            Location fortythree = new Location("");
            fortythree.setLatitude(40.467464);
            fortythree.setLongitude(-79.908775);

            Location fortyfour = new Location("");
            fortyfour.setLatitude(40.463211);
            fortyfour.setLongitude(-79.905827);

            Location fortyfive = new Location("");
            fortyfive.setLatitude(40.462541);
            fortyfive.setLongitude(-79.905589);

            Location fortysix = new Location("");
            fortysix.setLatitude(40.461681);
            fortysix.setLongitude(-79.905798);

            Location fortyseven = new Location("");
            fortyseven.setLatitude(40.456749);
            fortyseven.setLongitude(-79.909474);

            Location fortyeight = new Location("");
            fortyeight.setLatitude(40.454036);
            fortyeight.setLongitude(-79.912367);

            Location fortynine = new Location("");
            fortynine.setLatitude(40.452700);
            fortynine.setLongitude(-79.922037);

            Location fifty = new Location("");
            fifty.setLatitude(40.450534);
            fifty.setLongitude(-79.927498);

            Location fiftyone = new Location("");
            fiftyone.setLatitude(40.447884);
            fiftyone.setLongitude(-79.935777);

            Location fiftytwo = new Location("");
            fiftytwo.setLatitude(40.447307);
            fiftytwo.setLongitude(-79.944174);

            Location fiftythree = new Location("");
            fiftythree.setLatitude(40.448675);
            fiftythree.setLongitude(-79.944220);

            Location fiftyfour = new Location("");
            fiftyfour.setLatitude(40.451344);
            fiftyfour.setLongitude(-79.945430);

            Location fiftyfive = new Location("");
            fiftyfive.setLatitude(40.449809);
            fiftyfive.setLongitude(-79.950745);

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
            staticRoute.add(five);
            staticRoute.add(six);
            staticRoute.add(seven);
            staticRoute.add(eight);
            staticRoute.add(nine);
            staticRoute.add(ten);
            staticRoute.add(eleven);
            staticRoute.add(twelve);
            staticRoute.add(thirteen);
            staticRoute.add(fourteen);
            staticRoute.add(fifteen);
            staticRoute.add(sixteen);
            staticRoute.add(seventeen);
            staticRoute.add(eighteen);
            staticRoute.add(nineteen);
            staticRoute.add(twenty);
            staticRoute.add(twentyone);
            staticRoute.add(twentytwo);
            staticRoute.add(twentythree);
            staticRoute.add(twentyfour);
            staticRoute.add(twentyfive);
            staticRoute.add(twentysix);
            staticRoute.add(twentyseven);
            staticRoute.add(twentyeight);
            staticRoute.add(twentynine);
            staticRoute.add(thirty);
            staticRoute.add(thirtyone);
            staticRoute.add(thirtytwo);
            staticRoute.add(thirtythree);
            staticRoute.add(thirtyfour);
            staticRoute.add(thirtyfive);
            staticRoute.add(thirtysix);
            staticRoute.add(thirtyseven);
            staticRoute.add(thirtyeight);
            staticRoute.add(thirtynine);
            staticRoute.add(forty);
            staticRoute.add(fortyone);
            staticRoute.add(fortytwo);
            staticRoute.add(fortythree);
            staticRoute.add(fortyfour);
            staticRoute.add(fortyfive);
            staticRoute.add(fortysix);
            staticRoute.add(fortyseven);
            staticRoute.add(fortyeight);
            staticRoute.add(fortynine);
            staticRoute.add(fifty);
            staticRoute.add(fiftyone);
            staticRoute.add(fiftytwo);
            staticRoute.add(fiftythree);
            staticRoute.add(fiftyfour);
            staticRoute.add(fiftyfive);
        } else if(user.equals("stephen")) {
            Location one = new Location("");
            one.setLatitude(40.435797);
            one.setLongitude(-79.962778);

            Location two = new Location("");
            two.setLatitude(40.436454);
            two.setLongitude(-79.962317);

            Location three = new Location("");
            three.setLatitude(40.437097);
            three.setLongitude(-79.963062);

            Location four = new Location("");
            four.setLatitude(40.442560);
            four.setLongitude(-79.955616);

            Location five = new Location("");
            five.setLatitude(40.442715);
            five.setLongitude(-79.955297);

            Location six = new Location("");
            six.setLatitude(40.443172);
            six.setLongitude(-79.953565);

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
            staticRoute.add(five);
            staticRoute.add(six);
        } else if(user.equals("matt")) {
            //Detect if it's the T
            if(MainActivity.Transportation == "T") {
                //Mcneilly
                Location one = new Location("");
                one.setLatitude(40.378012);
                one.setLongitude(-80.004230);

                //End of South Busway
                Location two = new Location("");
                two.setLatitude(40.382827);
                two.setLongitude(-79.996851);

                //South Bank
                Location three = new Location("");
                three.setLatitude(40.392673);
                three.setLongitude(-79.998082);

                //Denise
                Location four = new Location("");
                four.setLatitude(40.399630);
                four.setLongitude(-79.999019);

                //Bon Air
                Location five = new Location("");
                five.setLatitude(40.407941);
                five.setLongitude(-80.003105);

                //Boggs
                Location six = new Location("");
                six.setLatitude(40.416654);
                six.setLongitude(-80.010457);

                //Bend Closest to Boggs
                Location seven = new Location("");
                seven.setLatitude(40.418571);
                seven.setLongitude(-80.010873);

                //South Hills Junction
                Location eight = new Location("");
                eight.setLatitude(40.421131);
                eight.setLongitude(-80.006793);

                //Start of Mt. Washington Transit Tunnel
                Location nine = new Location("");
                nine.setLatitude(40.432027);
                nine.setLongitude(-80.004138);

                //Rounding the Bend
                Location ten = new Location("");
                ten.setLatitude(40.430870);
                ten.setLongitude(-80.001577);

                //Start of the River
                Location eleven = new Location("");
                eleven.setLatitude(40.431541);
                eleven.setLongitude(-79.999134);

                //First Avenue
                Location twelve = new Location("");
                twelve.setLatitude(40.435384);
                twelve.setLongitude(-79.996308);

                staticRoute.add(one);
                staticRoute.add(two);
                staticRoute.add(three);
                staticRoute.add(four);
                staticRoute.add(five);
                staticRoute.add(six);
                staticRoute.add(seven);
                staticRoute.add(eight);
                staticRoute.add(nine);
                staticRoute.add(ten);
                staticRoute.add(eleven);
                staticRoute.add(twelve);
            } else if(MainActivity.Transportation == "61") {
                //Fifth and Sixth Avenue
                Location one = new Location("");
                one.setLatitude(40.438941);
                one.setLongitude(-79.994737);
                //Sixth and  Forbes
                Location two = new Location("");
                two.setLatitude(40.438036);
                two.setLongitude(-79.994348);
                //Forbes and Birmingham
                Location three = new Location("");
                three.setLatitude(40.437065);
                three.setLongitude(-79.972453);
                //Forbes Stops bending
                Location four = new Location("");
                four.setLatitude(40.435949);
                four.setLongitude(-79.964750);
                //Forbes and David Lawrence Hall
                Location five = new Location("");
                five.setLatitude(40.442669);
                five.setLongitude(-79.955434);
                //Final destination - Forbes and Bigelow
                Location six = new Location("");
                six.setLatitude(40.443157);
                six.setLongitude(-79.953597);
                //Add the newly constructed routes
                staticRoute.add(one);
                staticRoute.add(two);
                staticRoute.add(three);
                staticRoute.add(four);
                staticRoute.add(five);
                staticRoute.add(six);
            } else if(MainActivity.Transportation == "71") {
                //Fifth and Sixth Avenue
                Location one = new Location("");
                one.setLatitude(40.438941);
                one.setLongitude(-79.994737);
                //Sixth and  Forbes
                Location two = new Location("");
                two.setLatitude(40.438036);
                two.setLongitude(-79.994348);
                //Forbes and Jumonville
                Location three = new Location("");
                three.setLatitude(40.437787);
                three.setLongitude(-79.977697);
                //Fifth and Jumonville
                Location four = new Location("");
                four.setLatitude(40.438228);
                four.setLongitude(-79.977649);
                //Fifth and Kirkpatrick
                Location five = new Location("");
                five.setLatitude(40.437917);
                five.setLongitude(-79.973414);
                //Fifth close to Brenham
                Location six = new Location("");
                six.setLatitude(40.436445);
                six.setLongitude(-79.968761);
                //Fifth and Blvd of the Allies
                Location seven = new Location("");
                seven.setLatitude(40.436576);
                seven.setLongitude(-79.966310);

                //Fifth Robinson
                Location eight = new Location("");
                eight.setLatitude(40.437646);
                eight.setLongitude(-79.965113);

                //Fifth Dunseith
                Location nine = new Location("");
                nine.setLatitude(40.438347);
                nine.setLongitude(-79.963134);

                //Final - Fifth and Bigelow
                Location ten = new Location("");
                ten.setLatitude(40.444408);
                ten.setLongitude(-79.954845);
                //Add the newly created route
                staticRoute.add(one);
                staticRoute.add(two);
                staticRoute.add(three);
                staticRoute.add(four);
                staticRoute.add(five);
                staticRoute.add(six);
                staticRoute.add(seven);
                staticRoute.add(eight);
                staticRoute.add(nine);
                staticRoute.add(ten);
            }
        }
    }

    //creates route to home
    private void createHomeRoute() {
        if(user.equals("test")) {
            Location one = new Location("");
            one.setLatitude(40.445179);
            one.setLongitude(-79.950357);

            Location two = new Location("");
            two.setLatitude(40.445065);
            two.setLongitude(-79.951263);

            Location three = new Location("");
            three.setLatitude(40.443918);
            three.setLongitude(-79.950732);

            Location four = new Location("");
            four.setLatitude(40.443162);
            four.setLongitude(-79.953543);

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
        } else if(user.equals("mosse")) {
            Location one = new Location("");
            one.setLatitude(40.441845);
            one.setLongitude(-79.956239);

            Location two = new Location("");
            two.setLatitude(40.441955);
            two.setLongitude(-79.956426);

            Location three = new Location("");
            three.setLatitude(40.442645);
            three.setLongitude(-79.955466);

            Location four = new Location("");
            four.setLatitude(40.444442);
            four.setLongitude(-79.948675);

            Location five = new Location("");
            five.setLatitude(40.446928);
            five.setLongitude(-79.949013);

            Location six = new Location("");
            six.setLatitude(40.447080);
            six.setLongitude(-79.947648);

            Location seven = new Location("");
            seven.setLatitude(40.447287);
            seven.setLongitude(-79.947414);

            Location eight = new Location("");
            eight.setLatitude(40.447989);
            eight.setLongitude(-79.947679);

            Location nine = new Location("");
            nine.setLatitude(40.450500);
            nine.setLongitude(-79.945023);

            Location ten = new Location("");
            ten.setLatitude(40.451304);
            ten.setLongitude(-79.940930);

            Location eleven = new Location("");
            eleven.setLatitude(40.452761);
            eleven.setLongitude(-79.936714);

            Location twelve = new Location("");
            twelve.setLatitude(40.455113);
            twelve.setLongitude(-79.931859);

            Location thirteen = new Location("");
            thirteen.setLatitude(40.456358);
            thirteen.setLongitude(-79.930534);

            Location fourteen = new Location("");
            fourteen.setLatitude(40.458362);
            fourteen.setLongitude(-79.927358);

            Location fifteen = new Location("");
            fifteen.setLatitude(40.459047);
            fifteen.setLongitude(-79.924499);

            Location sixteen = new Location("");
            sixteen.setLatitude(40.459072);
            sixteen.setLongitude(-79.923802);

            Location seventeen = new Location("");
            seventeen.setLatitude(40.459251);
            seventeen.setLongitude(-79.922713);

            Location eighteen = new Location("");
            eighteen.setLatitude(40.459190);
            eighteen.setLongitude(-79.922085);

            Location nineteen = new Location("");
            nineteen.setLatitude(40.459807);
            nineteen.setLongitude(-79.922584);

            Location twenty = new Location("");
            twenty.setLatitude(40.460550);
            twenty.setLongitude(-79.922997);

            Location twentyone = new Location("");
            twentyone.setLatitude(40.460720);
            twentyone.setLongitude(-79.922688);

            Location twentytwo = new Location("");
            twentytwo.setLatitude(40.462902);
            twentytwo.setLongitude(-79.921293);

            Location twentythree = new Location("");
            twentythree.setLatitude(40.463173);
            twentythree.setLongitude(-79.921033);

            Location twentyfour = new Location("");
            twentyfour.setLatitude(40.464936);
            twentyfour.setLongitude(-79.919976);

            Location twentyfive = new Location("");
            twentyfive.setLatitude(40.466037);
            twentyfive.setLongitude(-79.919287);

            Location twentysix = new Location("");
            twentysix.setLatitude(40.468835);
            twentysix.setLongitude(-79.917803);

            Location twentyseven = new Location("");
            twentyseven.setLatitude(40.468525);
            twentyseven.setLongitude(-79.916628);

            Location twentyeight = new Location("");
            twentyeight.setLatitude(40.469247);
            twentyeight.setLongitude(-79.912611);

            Location twentynine = new Location("");
            twentynine.setLatitude(40.469627);
            twentynine.setLongitude(-79.911940);

            Location thirty = new Location("");
            thirty.setLatitude(40.470202);
            thirty.setLongitude(-79.911602);

            Location thirtyone = new Location("");
            thirtyone.setLatitude(40.473442);
            thirtyone.setLongitude(-79.911092);

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
            staticRoute.add(five);
            staticRoute.add(six);
            staticRoute.add(seven);
            staticRoute.add(eight);
            staticRoute.add(nine);
            staticRoute.add(ten);
            staticRoute.add(eleven);
            staticRoute.add(twelve);
            staticRoute.add(thirteen);
            staticRoute.add(fourteen);
            staticRoute.add(fifteen);
            staticRoute.add(sixteen);
            staticRoute.add(seventeen);
            staticRoute.add(eighteen);
            staticRoute.add(nineteen);
            staticRoute.add(twenty);
            staticRoute.add(twentyone);
            staticRoute.add(twentytwo);
            staticRoute.add(twentythree);
            staticRoute.add(twentyfour);
            staticRoute.add(twentyfive);
            staticRoute.add(twentysix);
            staticRoute.add(twentyseven);
            staticRoute.add(twentyeight);
            staticRoute.add(twentynine);
            staticRoute.add(thirty);
            staticRoute.add(thirtyone);
        } else if(user.equals("driver")) {
            Location one = new Location("");
            one.setLatitude(40.450092);
            one.setLongitude(-79.950885);

            Location two = new Location("");
            two.setLatitude(40.449805);
            two.setLongitude(-79.950725);

            Location three = new Location("");
            three.setLatitude(40.469247);
            three.setLongitude(-79.912611);

            Location four = new Location("");
            four.setLatitude(40.451407);
            four.setLongitude(-79.945441);

            Location five = new Location("");
            five.setLatitude(40.452175);
            five.setLongitude(-79.941463);

            Location six = new Location("");
            six.setLatitude(40.4447717);
            six.setLongitude(-79.938835);

            Location seven = new Location("");
            seven.setLatitude(40.447922);
            seven.setLongitude(-79.935758);

            Location eight = new Location("");
            eight.setLatitude(40.450545);
            eight.setLongitude(-79.927512);

            Location nine = new Location("");
            nine.setLatitude(40.452727);
            nine.setLongitude(-79.921930);

            Location ten = new Location("");
            ten.setLatitude(40.454016);
            ten.setLongitude(-79.912438);

            Location eleven = new Location("");
            eleven.setLatitude(40.456784);
            eleven.setLongitude(-79.909434);

            Location twelve = new Location("");
            twelve.setLatitude(40.461690);
            twelve.setLongitude(-79.905818);

            Location thirteen = new Location("");
            thirteen.setLatitude(40.462433);
            thirteen.setLongitude(-79.905609);

            Location fourteen = new Location("");
            fourteen.setLatitude(40.463282);
            fourteen.setLongitude(-79.905840);

            Location fifteen = new Location("");
            fifteen.setLatitude(40.466922);
            fifteen.setLongitude(-79.908500);

            Location sixteen = new Location("");
            sixteen.setLatitude(40.467681);
            sixteen.setLongitude(-79.908782);

            Location seventeen = new Location("");
            seventeen.setLatitude(40.479791);
            seventeen.setLongitude(-79.907803);

            Location eighteen = new Location("");
            eighteen.setLatitude(40.482198);
            eighteen.setLongitude(-79.908039);

            Location nineteen = new Location("");
            nineteen.setLatitude(40.484263);
            nineteen.setLongitude(-79.907824);

            Location twenty = new Location("");
            twenty.setLatitude(40.485152);
            twenty.setLongitude(-79.909219);

            //stop on washington blv entrance curve

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
            staticRoute.add(five);
            staticRoute.add(six);
            staticRoute.add(seven);
            staticRoute.add(eight);
            staticRoute.add(nine);
            staticRoute.add(ten);
            staticRoute.add(eleven);
            staticRoute.add(twelve);
            staticRoute.add(thirteen);
            staticRoute.add(fourteen);
            staticRoute.add(fifteen);
            staticRoute.add(sixteen);
            staticRoute.add(seventeen);
            staticRoute.add(eighteen);
            staticRoute.add(nineteen);
            staticRoute.add(twenty);
        } else if(user.equals("stephen")) {
            Location one = new Location("");
            one.setLatitude(40.443172);
            one.setLongitude(-79.953565);

            Location two = new Location("");
            two.setLatitude(40.442715);
            two.setLongitude(-79.955297);

            Location three = new Location("");
            three.setLatitude(40.442560);
            three.setLongitude(-79.955616);

            Location four = new Location("");
            four.setLatitude(40.437097);
            four.setLongitude(-79.963062);

            Location five = new Location("");
            five.setLatitude(40.436454);
            five.setLongitude(-79.962317);

            Location six = new Location("");
            six.setLatitude(40.435797);
            six.setLongitude(-79.962778);

            staticRoute.add(one);
            staticRoute.add(two);
            staticRoute.add(three);
            staticRoute.add(four);
            staticRoute.add(five);
            staticRoute.add(six);
        } else if(user.equals("matt")) {
            if(MainActivity.Transportation == "T") {
                //Crossing the River
                Location one = new Location("");
                one.setLatitude(40.431541);
                one.setLongitude(-79.999134);

                //Rounding the Bend
                Location two = new Location("");
                two.setLatitude(40.430870);
                two.setLongitude(-80.001577);

                //Start of Mt. Washington Transit Tunnel
                Location three = new Location("");
                three.setLatitude(40.432027);
                three.setLongitude(-80.004138);

                //South Hills Junction
                Location four = new Location("");
                four.setLatitude(40.421131);
                four.setLongitude(-80.006793);

                //Bend Closest to Boggs
                Location five = new Location("");
                five.setLatitude(40.418571);
                five.setLongitude(-80.010873);

                //Boggs
                Location six = new Location("");
                six.setLatitude(40.416654);
                six.setLongitude(-80.010457);

                //Bon Air
                Location seven = new Location("");
                seven.setLatitude(40.407941);
                seven.setLongitude(-80.003105);
                //Denise
                Location eight = new Location("");
                eight.setLatitude(40.399630);
                eight.setLongitude(-79.999019);

                //South Bank
                Location nine = new Location("");
                nine.setLatitude(40.392673);
                nine.setLongitude(-79.998082);

                //End of South Busway
                Location ten = new Location("");
                ten.setLatitude(40.382827);
                ten.setLongitude(-79.996851);

                //Mcneilly
                Location eleven = new Location("");
                eleven.setLatitude(40.378012);
                eleven.setLongitude(-80.004230);

                //Final Destination - Killarney
                Location twelve = new Location("");
                twelve.setLatitude(40.373835);
                twelve.setLongitude(-80.007824);

                staticRoute.add(one);
                staticRoute.add(two);
                staticRoute.add(three);
                staticRoute.add(four);
                staticRoute.add(five);
                staticRoute.add(six);
                staticRoute.add(seven);
                staticRoute.add(eight);
                staticRoute.add(nine);
                staticRoute.add(ten);
                staticRoute.add(eleven);
                staticRoute.add(twelve);
            } else if(MainActivity.Transportation == "Bus") {
                //First - Fifth and Atwood
                Location one = new Location("");
                one.setLatitude(40.441770);
                one.setLongitude(-79.958493);

                //Fifth Dunseith
                Location two = new Location("");
                two.setLatitude(40.438347);
                two.setLongitude(-79.963134);

                //Fifth Robinson
                Location three = new Location("");
                three.setLatitude(40.437646);
                three.setLongitude(-79.965113);

                //Fifth and Blvd of the Allies
                Location four = new Location("");
                four.setLatitude(40.436576);
                four.setLongitude(-79.966310);

                //Fifth close to Brenham
                Location five = new Location("");
                five.setLatitude(40.436445);
                five.setLongitude(-79.968761);

                //Fifth and Kirkpatrick
                Location six = new Location("");
                six.setLatitude(40.437917);
                six.setLongitude(-79.973414);

                Location seven = new Location("");
                seven.setLatitude(40.438936);
                seven.setLongitude(-79.994712);

                staticRoute.add(one);
                staticRoute.add(two);
                staticRoute.add(three);
                staticRoute.add(four);
                staticRoute.add(five);
                staticRoute.add(six);
                staticRoute.add(seven);
            }
            else if(MainActivity.Transportation == "Car1"){
                //Wabash
                Location one = new Location("");
                one.setLatitude(40.372869);
                one.setLongitude(-80.010122);

                //Killarney
                Location two = new Location("");
                two.setLatitude(40.375268);
                two.setLongitude(-80.008786);

                //Library
                Location three = new Location("");
                three.setLatitude(40.373777);
                three.setLongitude(-80.006353);

                //McNeilly
                Location four = new Location("");
                four.setLatitude(40.377916);
                four.setLongitude(-80.003761);

                //Pioneer
                Location five = new Location("");
                five.setLatitude(40.395344);
                five.setLongitude(-80.030679);

                //19
                Location six = new Location("");
                six.setLatitude(40.395785);
                six.setLongitude(-80.033131);

                //Scott
                Location seven = new Location("");
                seven.setLatitude(40.388364);
                seven.setLongitude(-80.042914);
                //Anawanda
                Location eight = new Location("");
                eight.setLatitude(40.373082);
                eight.setLongitude(-80.034317);

                //BroadMoor
                Location nine = new Location("");
                nine.setLatitude(40.376483);
                nine.setLongitude(-80.031316);

                //Audobon
                Location ten = new Location("");
                ten.setLatitude(40.375556);
                ten.setLongitude(-80.030087);

                //Country Club
                Location eleven = new Location("");
                eleven.setLatitude(40.376925);
                eleven.setLongitude(-80.028861);

                //Briarwood
                Location twelve = new Location("");
                twelve.setLatitude(40.375534);
                twelve.setLongitude(-80.026310);

                //Sleepy Hollow
                Location thirteen = new Location("");
                thirteen.setLatitude(40.376421);
                thirteen.setLongitude(-80.023816);

                //Rosewood
                Location fourteen = new Location("");
                fourteen.setLatitude(40.375328);
                fourteen.setLongitude(-80.021662);

                //MapleWood
                Location fifteen = new Location("");
                fifteen.setLatitude(40.376279);
                fifteen.setLongitude(-80.020728);

                //Larch
                Location sixteen = new Location("");
                sixteen.setLatitude(40.375154);
                sixteen.setLongitude(-80.018344);

                //Rolling Rock
                Location seventeen = new Location("");
                seventeen.setLatitude(40.375173);
                seventeen.setLongitude(-80.015526);

                //Newport Drive
                Location eighteen = new Location("");
                eighteen.setLatitude(40.376267);
                eighteen.setLongitude(-80.015364);

                //Down to Wabash
                Location nineteen = new Location("");
                nineteen.setLatitude(40.372883);
                nineteen.setLongitude(-80.010141);

                staticRoute.add(one);
                staticRoute.add(two);
                staticRoute.add(three);
                staticRoute.add(four);
                staticRoute.add(five);
                staticRoute.add(six);
                staticRoute.add(seven);
                staticRoute.add(eight);
                staticRoute.add(nine);
                staticRoute.add(ten);
                staticRoute.add(eleven);
                staticRoute.add(twelve);
                staticRoute.add(thirteen);
                staticRoute.add(fourteen);
                staticRoute.add(fifteen);
                staticRoute.add(sixteen);
                staticRoute.add(seventeen);
                staticRoute.add(eighteen);
                staticRoute.add(nineteen);
            }
        } 
    }


    /**
     * Given two locations, calculates the distance between the two.
     *
     * @param cur current location
     * @param dest destination location
     * @return
     */
    private float calcDistance(Location cur, Location dest) {
        try {
            if(cur == null) {
                distanceDisplay.setText("Cur is null");
                return 0f;
            } else {
                float distance = cur.distanceTo(dest);
                //distancedisplay.setText(String.valueOf(distance) + " || " + cur.getLatitude() + " || " + cur.getLongitude());
                distanceDisplay.setText(String.valueOf(distance) + " m");
                return distance;
            }
        } catch (Exception e) {
            velocityDisplay.setText(e.getMessage());
        }
        return 0f;
    }

    /**
     * Calculates the travel time from the current location to destination and returns it as a float.
     *
     * @param currentLoc the current location
     * @param currentDestination the current destination
     * @param currentSpeed the current speed (unused in Waze mode)
     * @return travel time as a float in seconds
     */
    private float calculateTravelTime(Location currentLoc, Location currentDestination, float currentSpeed) {
        float travelTime = -1;
        Log.d(TAG, String.format("Calculating travel time for (%f, %f) to (%f, %f)", currentLoc.getLatitude(), currentLoc.getLongitude(), currentDestination.getLatitude(), currentDestination.getLongitude()));

        if (MainActivity.WazeMode) {
            // Calculate using Waze.
            String toLatitude = Double.toString(currentDestination.getLatitude());
            String toLongitude = Double.toString(currentDestination.getLongitude());
            String fromLatitude = Double.toString(currentLoc.getLatitude());
            String fromLongitude = Double.toString(currentLoc.getLongitude());

            travelTime = WazeWhisperer.getTravelTime(toLatitude, toLongitude, fromLatitude, fromLongitude);
            if (travelTime == -1) {
                // Waze failed to get travel time.
                Log.d(TAG, "Error getting travel time from Waze!");
            } else {
                Log.d(TAG, "Waze returned travel time of " + travelTime);
            }
        }

        // Check if Waze mode failed or if we aren't in Waze mode.
        if (travelTime == -1) {
            // Either we aren't in Waze mode or Waze mode failed, so use distance divided by speed.
            travelTime = (calcDistance(currentLoc, currentDestination) - ACCEPTABLE_DISTANCE_RADIUS) / currentSpeed;
        }

        return travelTime;
    }

    /**
     * This function creates a location listener that enables us to perform events on location
     * changes.
     */
    private void setNewLocationListener() {
        // Create the location listener.
        locationListener = new LocationListener() {


            /**
             * Called whenever the location changes. We use this as the workhorse of our GPS
             * navigator.
             *
             * @param location a parameter supplied by the application of the current location.
             */
            @Override
            public void onLocationChanged(Location location) {
                currentLocation = location;

                velocityDisplay.setText("Trying to acquire speed");

                // Check if we are able to acquire the speed from the Location object supplied to
                // us.
                if(location.hasSpeed()) {
                    // Update our display with the current speed.
                    velocityDisplay.setText(location.getSpeed() + " m/s");

                    // If we are currently in navigation mode, calculate distance to the next destination.
                    if(isInNavigationMode) {
                        float timeToDestination = calculateTravelTime(location, currentDestination, location.getSpeed());
                        timeDisplay.setText(timeToDestination + "s");
                        try {
                            Date now = new Date();
                            int batLevel = getBatteryLevel();
                            String logString = "CUR_LOC | " + now.toString() + " | " + batLevel + "% | " + location.getLatitude() + " | " + location.getLongitude() + " | " + location.getSpeed() + "m/s | " + location.distanceTo(currentDestination) + "m" + "\n";
                            loggingFileOutputStream.write(logString.getBytes());
                        } catch(Exception e) {
                            shouldLogCurrentLocation = true;
                        }
                    }
                }

                if(isInNavigationMode) {
                    // Check if we've arrived at the current destination (if it's within a
                    // specified radius).
                    if(location.distanceTo(currentDestination) <= ACCEPTABLE_DISTANCE_RADIUS) {

                        // We reached the destination radius - no need to test if we accidentally
                        // overshot the coordinate
                        shouldTestOvershot = false;

                        // We have reached our destination, so set a new destination.
                        if(setNextDestination()) {
                            // Calculate travel time to the new destination
                            float travelTimeToNextDestination = calculateTravelTime(location, currentDestination, location.getSpeed());

                            // Log the GPS being recommended to turn off.
                            try {
                                Date now = new Date();
                                int batLevel = getBatteryLevel();
                                String logString = "LOC_OFF | " + now.toString() + " | " + batLevel + "% | " + location.getLatitude() + " | " + location.getLongitude() + "\n";
                                loggingFileOutputStream.write(logString.getBytes());
                            } catch(Exception e) {
                            }

                            // Call a method to decide if we have enough time to turn off and
                            // regain a GPS lock, or if we should just wait out the maneuver.
                            decideShouldGPSTurnOff(travelTimeToNextDestination, location);
                        }
                    }
                    // We are not within the target destination's radius. Check if we need to test
                    // for overshooting.
                    else if(shouldTestOvershot) {
                        // Acquire the first distance to destination (next point in the list)
                        // and use that to determine if we overshot our destination
                        if(firstOvershot) {
                            firstDistance = location.distanceTo(currentDestination);
                            firstOvershot = false;
                        }

                        // We have a first distance and the location changed again - measure
                        else {
                            //Get the current distance
                            float curDistance = location.distanceTo(currentDestination);
                            //Since the users' location fluctuates ever so slightly even when they're standing still - we have to account for that, can't blindly compare if one is greater than the other
                            //If the distance changed more than 5 meters - a significant change
                            if(curDistance - firstDistance > 5f){
                                shouldTestOvershot = false;
                                //We overshot our destination since we increased distance by meters - acquire the next intersection
                                if(setNextDestination()) {
                                    //Log that we over shot and by how many meters
                                    try {
                                        Date now = new Date();
                                        int batLevel = getBatteryLevel();
                                        String logString = "MISSED_POINT | " + now.toString() + " | " + batLevel + "% | " + curDistance + "|" + location.getLatitude() + "|" + location.getLongitude() + "\n";
                                        loggingFileOutputStream.write(logString.getBytes());
                                    } catch(Exception e) {

                                    }
                                    // Calculate travel time to the new destination
                                    float travelTimeToNextDestination = calculateTravelTime(location, currentDestination, location.getSpeed());

                                    try {
                                        Date now = new Date();
                                        String logString = "LOC_OFF|" + location.getLatitude() + "|" + location.getLongitude() + "|" + now.toString()  +"\n";
                                        loggingFileOutputStream.write(logString.getBytes());
                                    } catch(Exception e) {

                                    }
                                    decideShouldGPSTurnOff(travelTimeToNextDestination, location);
                                }
                            }
                            //If we're gaining distance by more than a meter
                            else if(curDistance - firstDistance < -5f) {
                                //Just end the testing here - we're still moving closer to the destination so we did not overshoot
                                shouldTestOvershot = false;
                            }
                            //If neither of those occurred - do nothing - keep measuring the distance
                        }
                    }
                }
                //Check if we just turned the GPS On and need to log the first possible set of coordinates
                if(shouldLogCurrentLocation) {
                    shouldLogCurrentLocation = false;
                    try {
                        Date now = new Date();
                        int batLevel = getBatteryLevel();
                        String logString = "LOC_ON | " + now.toString() + " | " + batLevel + "% | " + location.getLatitude() + " | " + location.getLongitude() + " | " + "\n";
                        loggingFileOutputStream.write(logString.getBytes());
                    } catch(Exception e) {
                        shouldLogCurrentLocation = true;
                    }
                }
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
    }

    /**
     * Gets the current battery level of the device.
     *
     * @return battery percentage as an integer
     */
    public int getBatteryLevel() {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    /**
     * This function handles setting the next destination of a route. If the final destination has
     * been reached, it stops navigation.
     *
     * @return true if another destination was set, false if navigation is done
     */
    private boolean setNextDestination() {
        ListI++;
        //Check if the user reached their destination
        if(ListI == staticRoute.size()) {
            destMarker.remove();
            isInNavigationMode = false;
            ListI = 0;
            //Set the two text boxes relevant only to a destination to off
            distanceDisplay.setText("");
            timeDisplay.setText("");
            try {
                //Log that the user has reached their destination
                Date now = new Date();
                int batLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                String logString = "NAV_STOP (FINAL_DEST) | " + now.toString() + " | " + batLevel + "%" + "\n";
                loggingFileOutputStream.write(logString.getBytes());
                loggingFileOutputStream.write("-------------------------------------------------------\n".getBytes());
            } catch(Exception e) {
                velocityDisplay.setText(e.getMessage());
            }
            return false;
        } else {
            //Remove the current marker from the map
            destMarker.remove();
            // get the next destination
            currentDestination = staticRoute.get(ListI);
            //Create a marker at that point
            LatLng destLL = new LatLng(currentDestination.getLatitude(), currentDestination.getLongitude());
            MarkerOptions destMarkerOptions = new MarkerOptions().position(destLL).title("Next Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            //Add it to the map and save it in an object that we can use to remove it
            destMarker = mMap.addMarker(destMarkerOptions);
            return true;
        }
    }

    private void decideShouldGPSTurnOff(float time, Location location) {
        float turnOffTime = time - GPS_STARTUP_TIME;
        //If the odds of getting a new fix are lower than we like, don't bother shutting it off at all
        if(turnOffTime <= 0) {
            try {
                //Log that the time was too small for standard deviation of reacquiring signal
                Date now = new Date();
                int batLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                String logString = "GPS_NOT_OFF (STD_MISSED) | " + now.toString() + " | " + turnOffTime + "s | " + batLevel + "%" + "\n";
                loggingFileOutputStream.write(logString.getBytes());
            } catch(Exception e) {
                velocityDisplay.setText(e.getMessage());
            }
        }
        //Conversely, if the time we're shutting it off for is too small for any noticeable savings
        else if(turnOffTime <= POWER_SAVING_THRESHOLD_TIME) {
            try {
                Date now = new Date();
                int batLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                String logString = "GPS_NOT_OFF (NO_POWER_SAVED) | " + now.toString() + " | " + turnOffTime + "s | " + batLevel + "%" + "\n";
                loggingFileOutputStream.write(logString.getBytes());
            } catch(Exception e) {
                velocityDisplay.setText(e.getMessage());
            }
        }
        //We have enough time. Turn the GPS off and then back on again
        else {
            try {
                //Log the amount of time the GPS will be off
                Date now = new Date();
                int batLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                String logString = "ETAOFF | " + now.toString() + " | " + turnOffTime + "s | " + batLevel + "%" + "\n";
                loggingFileOutputStream.write(logString.getBytes());
            } catch(Exception e) {
                velocityDisplay.setText(e.getMessage());
            }
            if(logging == false) {
                turnGPSOff(getApplicationContext());
            } else {
                logGPSOff();
            }
            //Convert from seconds to milliseconds for the runnable
            long milliseconds = (long)(turnOffTime * 1000);

            //Call the GPS ON Method after
            handler.postDelayed(GPSOnRunnable, milliseconds);
        }
    }

    /*******************************************************
     * BELOW 2 METHODS USED FOR LOGGING OF GPS ON/OFF ONLY *
     *******************************************************/

    // method used to simulate GPS being turned off when only logging
    private void logGPSOff() {
        Date now = new Date();
        int batLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        // we have turned the GPS off
        String logString = "GPS_OFF (LOGGING_ONLY) | " + now.toString() + " | " + batLevel + "%" + "\n";
        try {
            loggingFileOutputStream.write(logString.getBytes());
        } catch(Exception e) {
            velocityDisplay.setText(e.getMessage());
        }
    }

    /**
     * Logs the GPS being turned on.
     */
    private void logGPSOn() {
        Date now = new Date();
        int batLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        // we have turned the GPS on but haven't necessarily acquired a signal yet (redundant with LOC_ON when only logging)
        String logString = "GPS_ON (LOGGING_ONLY) | " + now.toString() + " | " + batLevel + "%" + "\n";
        try {
            loggingFileOutputStream.write(logString.getBytes());
        } catch(Exception e) {
            velocityDisplay.setText(e.getMessage());
        }
        shouldLogCurrentLocation = true;
        shouldTestOvershot = true;
        firstOvershot = true;
    }

    /**********************************************************
     * BELOW 2 METHODS USED FOR MODULATING OF GPS POWER STATE *
     **********************************************************/

    /**
     *
     * This method is a wrapper to our function to turn the GPS off. We accomplish this by manipulating
     * the raw settings.
     *
     * @param context the Context object of the application
     */
    private void turnGPSOff(Context context) {
        if(null == beforeEnable) {
            String str = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if (null == str) {
                str = "";
            } else {
                String[] list = str.split (",");
                str = "";
                int j = 0;
                for (int i = 0; i < list.length; i++) {
                    if (!list[i].equals("network") && !list[i].equals("gps")) {
                        if (j > 0) {
                            str += ",";
                        }
                        str += list[i];
                        j++;
                    }
                }
            }
        }
        try {
            Settings.Secure.putString (context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, beforeEnable);
        } catch(Exception e) {
            velocityDisplay.setText(e.getMessage());
        }

        // Log turning the GPS off.
        Date now = new Date();
        int batLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        String logString = "GPS_OFF | " + now.toString() + " | " + batLevel + "%" + "\n";
        try {
            loggingFileOutputStream.write(logString.getBytes());
        } catch(Exception e) {
            velocityDisplay.setText(e.getMessage());
        }
    }

    /**
     *
     * This method is a wrapper to our function to turn the GPS on. We accomplish this by manipulating
     * the settings.
     *
     * @param context the Context object of the application
     */
    private void turnGPSOn(Context context) {

        // Turn on the GPS.
        try {
            Settings.Secure.putString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, "network,gps");
        } catch(Exception e) {
            velocityDisplay.setText(e.getMessage());
        }

        // Log the GPS turning on.
        Date now = new Date();
        int batLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        // We have turned the GPS on, but haven't necessarily acquired a lock with a satellite yet.
        String logString = "GPS_ON | " + now.toString() + " | " + batLevel + "%" + "\n";

        try {
            loggingFileOutputStream.write(logString.getBytes());
        } catch(Exception e) {
            velocityDisplay.setText(e.getMessage());
        }

        // We set the GPS to on, get the first coordinates that we can
        shouldLogCurrentLocation = true;
        shouldTestOvershot = true;
        firstOvershot = true;
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
    //@RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        //Now to get the user's live speed
    }

    /**
     * Evaluate whether we have access to the user's location
     * @return true if we have permission to access the user's location, false if we don't
     */
    public boolean canAccessLocation() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission was granted by the user
                    Toast.makeText(this, "Permission was granted", Toast.LENGTH_LONG).show();
                } else {
                    //permission was denied by the user
                    Toast.makeText(this, "Permission was denied. Can't get routes", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
}
