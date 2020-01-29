package com.gps.gpsoptimizationproject;

import android.content.Context;
import android.content.Intent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.provider.Settings;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    static LocationListener templistener;
    static LocationManager GPSDetector;
    static boolean textstatus = false;
    static String beforeEnable;
    static TextView MainText;
    static TextView GPSText;
    static boolean route = false;
    // provides buttons for Matt route select
    final static boolean Matt = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //declaring the menu items that were created in the xml files
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        GPSText = findViewById(R.id.GPSStatus);

        final TextView gpsIndicator = (TextView) findViewById(R.id.GPSindicator);
        MainText = findViewById(R.id.MainTextView);
        //Making the button do something
        Button gpsButton = findViewById(R.id.GPSToggle);
        Button pitt = findViewById(R.id.pitt);
        Button home = findViewById(R.id.home);
        //Notes from matt - extra buttons to accommodate the two different bus routes I take
        Button pitt61 = findViewById(R.id.pitt61);
        Button pitt71 = findViewById(R.id.pitt71);
        Button homebus = findViewById(R.id.homebus);
        Button customButton = findViewById(R.id.customButton);

        gpsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(textstatus){
                    textstatus = false;
                    gpsIndicator.setText("GPS ON");
                    turnGPSOn(getApplicationContext());
                }
                else{
                    textstatus = true;
                    gpsIndicator.setText("GPS OFF");
                    turnGPSOff(getApplicationContext());
                }
            }
        });

        //sets route to home --> pitt
        pitt.setOnClickListener(new View.OnClickListener() {
            @Override
           public void onClick(View v) {
                GlobalVars.Transportation = "T";
                route = false;
                moveToMapActivity();
           }

        });

        //sets route to pitt --> home
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalVars.Transportation = "T";
                route = true;
                moveToMapActivity();
            }
        });
        //Make the three new buttons invisible if the build is not for Matt
        if(!Matt) {
            pitt61.setVisibility(View.GONE);
            pitt71.setVisibility(View.GONE);
            homebus.setVisibility(View.GONE);
            customButton.setVisibility(View.GONE);
        } else {
            pitt.setText("To Pitt T");
            home.setText("To Home T");
        }
        //Sets the routes for Matt's trips
        pitt61.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalVars.Transportation = "61";
                route = false;
                moveToMapActivity();
            }
        });
        pitt71.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalVars.Transportation = "71";
                route = false;
                moveToMapActivity();
            }
        });
        homebus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalVars.Transportation = "Bus";
                route = true;
                moveToMapActivity();
            }
        });
        customButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalVars.Transportation = "Car1";
                route = true;
                moveToMapActivity();
            }
        });

        //Now to add the GPS Status listener
        setUpGPSListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /* Note from Matt - Code from StackOverFlow at this link.
    https://stackoverflow.com/questions/4721449/how-can-i-enable-or-disable-the-gps-programmatically-on-android
    AS */

    /* these turnGPSOn and turnGPSOff are only used for the top page button, to toggle GPS for testing */
    /* turnGPSOn and turnGPSOff in MapsActivities are used to toggle GPS for actual use */
    
    private void turnGPSOn (Context context) {
        try {
            Settings.Secure.putString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, "network,gps");
        } catch (Exception e) {
            MainText.setText(e.getMessage());
        }
    }

    private void turnGPSOff (Context context) {
        if (null == beforeEnable) {
            String str = Settings.Secure.getString (context.getContentResolver(),
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
                beforeEnable = str;
                GPSText.setText(str);
            }
        }
        try {
            Settings.Secure.putString (context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, beforeEnable);
        } catch(Exception e) {
            //Let's add in some error checking - lets set one of the text views to an error message
            //for exception handling
            MainText.setText(e.getMessage());
        }
    }

    //code to detect when the satellite is connected
    private void setUpGPSListener() {
        //First initialize the GPS Locator
        GPSDetector = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        setNewLocationListener();
        try {
            //Now add a GPS Status listener
            GPSDetector.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, -1, templistener);
            @Deprecated GpsStatus.Listener test = new GpsStatus.Listener() {
                //Now to implement a listener
                @Override
                public void onGpsStatusChanged(int event) {
                    switch (event) {
                        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                            break;
                        case GpsStatus.GPS_EVENT_FIRST_FIX:   // this means you  found GPS Co-ordinates
                            GPSText.setText("Got First Fix");
                            break;
                        case GpsStatus.GPS_EVENT_STARTED:
                            GPSText.setText("GPS Started!");
                            break;
                        case GpsStatus.GPS_EVENT_STOPPED:
                            GPSText.setText("GPS Stopped");
                            break;

                    }
                }
            };
            GPSDetector.addGpsStatusListener(test);
        } catch (SecurityException e) {
            MainText.setText(e.getMessage());
        } catch (Exception e) {
            MainText.setText(e.getMessage());
        }
    }

    private void moveToMapActivity() {
        Intent intent = new Intent(MainActivity.this, MapsActivity.class); 
        intent.putExtra("routeSelect", route);  // capture which route is select, add it to variable intent
        startActivity(intent); // calls the MapsActivity (another page). (does it start another thread?)
    }

    // these next few methods "have to be there" for GPS to work
    private void setNewLocationListener() {
       templistener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {

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

}
