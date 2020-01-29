package com.gps.gpstimer;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.lang.Thread;


public class MainActivity extends AppCompatActivity {
    static LocationManager GPSDetector;
    static LocationListener templistener;
    static boolean recording = false, GPSTurnOn = false;
    static String beforeEnable = "";
    static TextView MainText, GPSText;
    static Button toggleButton;
    static Runnable rOn, rOff;
    static Handler h;
    static long starttime, endtime, sumtime;
    static int numtimes;

    static File logfile;
    static FileOutputStream fos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Declare the textbox and button
        MainText = findViewById(R.id.MainView);
        GPSText = findViewById(R.id.GPSIndicator);
        toggleButton = findViewById(R.id.loopbool);
        logfile = new File(getApplicationContext().getFilesDir() + "/TimingLog.txt");
        //Initialize the log file
        try {
            fos = new FileOutputStream(logfile, true);
            Date now = new Date();
            String output = "APPSTART|" + now.toString() + "\n";
            fos.write(output.getBytes());
        }catch(FileNotFoundException e){
            //if file is not found - create it
            try {
                logfile.createNewFile();
            }catch(IOException i){
                MainText.setText(i.getMessage());
            }

        }
        catch (Exception e){
            MainText.setText("IO Exception : " + e.getMessage());
        }



        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                //recordingloop();
                if(!recording) {
                    //Log the starting of the loop
                    try{
                        Date now = new Date();
                        String output = "LOOPSTART|" + now.toString() + "\n";
                        fos.write("------------------------------------------------------\n".getBytes());
                        fos.write(output.getBytes());
                    }catch (Exception e){

                    }
                    rOn.run();
                    recording = true;
                }
            }
        });

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*dorecording = !dorecording;
                if(dorecording)
                    //toggleButton.setBackgroundDrawable(getResources().getColor(R.color.));
                    toggleButton.setBackgroundColor(android.graphics.Color.parseColor("#00FF00"));
                else
                    toggleButton.setBackgroundColor(android.graphics.Color.parseColor("#FF0000"));*/
                //To break the loop, we have to remove these callbacks
                try{
                    Date now = new Date();
                    String output = "LOOPEND|" + (sumtime/(double)numtimes) + "|" + (int)((sumtime/(double)numtimes) *0.0000010) +"ms|" + now.toString() + "\n";
                    fos.write(output.getBytes());
                    fos.write("------------------------------------------------------\n".getBytes());

                }catch(Exception e){

                }
                //Reset the variables -
                sumtime = 0;
                numtimes = 0;
                h.removeCallbacks(rOn);
                h.removeCallbacks(rOff);
                recording = false;
                MainText.setText("LOOP Broken!");
            }
        });
        //This is the code for handling the looping - the runnables hold the body, while the handler is what executes them
        h = new Handler();
        rOn = new Runnable() {
            @Override
            public void run() {
                turnGpsOn(getApplicationContext());
                MainText.setText("GPS Turn On Called!");
                h.postDelayed(rOff, 180000);
            }
        };
        rOff = new Runnable(){
            @Override
            public void run(){
                turnGpsOff(getApplicationContext());
                MainText.setText("GPS Turn Off Called!");
                h.postDelayed(rOn, 20000);
            }
        };
        //Set up the function to listen to the status of the GPS
        setUpGpsListener();

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
        //If I want to reset the file
        if (id == R.id.file_reset){
            MainText.setText("File Reset!");
            try{
                logfile.delete();
                logfile = new File(getApplicationContext().getFilesDir() + "/TimingLog.txt");
                fos = new FileOutputStream(logfile);
                Date now = new Date();
                String output = "APPSTART|" + now.toString() + "\n";
                fos.write(output.getBytes());
                numtimes =0;
                sumtime =0;

            }
            catch(Exception e){
                MainText.setText(e.getMessage());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //Notes from Matt
    //This is copy and pasted from the original project. If for any reason it changes in the original, it needs
    //to change in the
    private void turnGpsOn (Context context) {
        try {
            Settings.Secure.putString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, "network,gps");
        }
        catch(Exception e){
            MainText.setText(e.getMessage());
        }
        //We turned the GPS ON,
        GPSTurnOn = true;
    }


    private void turnGpsOff (Context context) {
        //LocationServices.SettingsApi
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
                    if (!list[i].equals (LocationManager.GPS_PROVIDER)) {
                        if (j > 0) {
                            str += ",";
                        }
                        str += list[i];
                        j++;
                    }
                }
                beforeEnable = str;
            }
        }
        try {
            Settings.Secure.putString (context.getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                    beforeEnable);
        } catch(Exception e) {
            //Let's add in some error checking - lets set one of the text views to an error message
            //for exception handling
            MainText.setText(e.getMessage());
        }
    }

    private void setUpGpsListener(){
        //First initialize the GPS Locator
        GPSDetector = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        setnewLocationListener();
        try{
            //Now add a GPS Status listener
            GPSDetector.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, templistener);
            @Deprecated GpsStatus.Listener test = new GpsStatus.Listener() {
                //Now to implement a listener
                @Override
                public void onGpsStatusChanged(int event) {
                    switch (event){
                        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                            break;
                        case GpsStatus.GPS_EVENT_FIRST_FIX:   // this means you  found GPS Co-ordinates
                            //Get the time
                            endtime = System.nanoTime();
                            //Calculate the total time taken
                            long timetaken = (endtime - starttime);
                            //Add it to the total
                            sumtime += timetaken;
                            numtimes++;
                            //Log it
                            try{
                                Date now = new Date();
                                String output = ("FIRSTFIX|" +endtime + "|" + timetaken + "|" + ((int)(timetaken * 0.0000010)) +"ms|" + now.toString() + "\n");
                                fos.write(output.getBytes());
                            }
                            catch (Exception e){
                                GPSText.setText(e.getMessage());
                            }
                            GPSText.setText("GPS Status : Got First Fix in " + (timetaken * 0.0000010) + "ms");
                            break;
                        case GpsStatus.GPS_EVENT_STARTED:
                            //Log the Gps Starting
                            starttime = System.nanoTime();
                            try{
                                Date now = new Date();
                                String output = "GPSSTART|" + starttime + "|" + now.toString() + "\n";
                                fos.write(output.getBytes());
                            }catch(Exception e){
                                GPSText.setText(e.getMessage());
                            }

                            GPSText.setText("GPS Status : GPS Started!");
                            break;
                        case GpsStatus.GPS_EVENT_STOPPED:
                            try{
                                Date now = new Date();
                                String output = "GPSSTOP|" + now.toString() + "\n";
                                fos.write(output.getBytes());
                            }catch(Exception e){
                                GPSText.setText(e.getMessage());
                            }
                            GPSText.setText("GPS Status : GPS Stopped");
                            break;

                    }
                }
            };
            GPSDetector.addGpsStatusListener(test);
        }catch (SecurityException e){
            MainText.setText(e.getMessage());
        }
        catch (Exception e){
            MainText.setText(e.getMessage());
        }
    }
    private void setnewLocationListener(){
        templistener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //If we turned the GPS ON and are currently looking to verify the times we are getting
                if(GPSTurnOn){
                    GPSTurnOn = false;
                    long curloctime = System.nanoTime();
                    long loctotaltime = curloctime - starttime;
                    try{
                        Date now = new Date();
                        String output = "FIRSTLOC|"+ curloctime + "|" + loctotaltime + "|" + (int)(loctotaltime *  0.0000010) + "ms|"+now.toString() + "\n";
                        fos.write(output.getBytes());
                    }catch(Exception e){
                        GPSText.setText(e.getMessage());
                        GPSTurnOn = true;
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
}
