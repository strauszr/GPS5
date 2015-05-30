package com.rolandstrausz.roland.gps5;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandCaloriesEvent;
import com.microsoft.band.sensors.BandCaloriesEventListener;
import com.microsoft.band.sensors.SampleRate;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class BigScreenActivity extends ActionBarActivity {

    Button startButton, pauseButton, resetButton;
    private TextView nr_satsTV, gpsstatusTV, debugTV;
    private float gpsTotalDistance = 0;
    private long gpsTime, lastTime, startTime, fileTime;
    private int durationMillis = 0;
    private int durationNoSpeedMillis = 0;
    private float correctDistance;
    private boolean gpsEnabled,lastLocationValid;
    private boolean gpsFix = false;
    private double nowLat, nowLon, nowAltitude, avgSpeed, maxSpeed,nowSpeed;
    private float gpsAccuracy = 0;
    private float nowBearing;
    private double avgHeartBeat = 0;
    private static final long DURATION_TO_FIX_LOST_MS = 10000;
    private List<Float> rollingAverageData = new LinkedList<Float>();
    private List<Float> speedHistory = new LinkedList<Float>();
    private int appStatus = 0; // 0: start; 1: running; 2: pause
    final int UPDATE_INTERVAL_GPS = 1000;
    final int MIN_ACCURACY_FOR_DISTANCE = 50;
    final int UPDATE_DISPLAY = 1000;
    final Context context = this;
    private LocationManager locationManager;
    private MyGpsListener gpsListener;
    private Location lastLocation, nowLocation;
    private SimpleDateFormat time_fm = new SimpleDateFormat("HH:mm:ss");
    private SimpleDateFormat date_fm = new SimpleDateFormat("dd.MM");
    private SimpleDateFormat gpxdate_fm = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat gpxtime_fm = new SimpleDateFormat("HH:mm:ss");

    private BandClient client = null;
    private int nowHeartRate,maxHeartRate,minHeartRate;
    private long beginCalories = -1;
    private long nowCalories;
    private rgsDisplayItem dItemCalories = new rgsDisplayItem("Calories", "0", "kCal", R.drawable.calories);
    private rgsDisplayItem dItemSpeed = new rgsDisplayItem("Speed", "0.00", "km/h", R.drawable.speed);
    private rgsDisplayItem dItemDistance = new rgsDisplayItem("Distance", "0.00", "km", R.drawable.distance);
    private rgsDisplayItem dItemHeartRate = new rgsDisplayItem("HeartRate", "0", "bpm", R.drawable.heart);
    private rgsDisplayItem dItemMaxHeartRate = new rgsDisplayItem("Maximum heartRate", "0", "bpm", R.drawable.max_heart);
    private rgsDisplayItem dItemMinHeartRate = new rgsDisplayItem("Minimum heartRate", "0", "bpm", R.drawable.min_heart);
    private rgsDisplayItem dItemAvgHeartRate = new rgsDisplayItem("AvgHeartRate", "0", "bpm", R.drawable.avgheart);
    private rgsDisplayItem dItemAvgSpeed = new rgsDisplayItem("AvgSpeed", "0.00", "km/h", R.drawable.avgspeed);
    private rgsDisplayItem dItemDuration = new rgsDisplayItem("Duration", "00:00:00", "", R.drawable.clock);
    private rgsDisplayItem dItemLongitude = new rgsDisplayItem("Longitude", "-", "", R.drawable.longitude);
    private rgsDisplayItem dItemLatitude = new rgsDisplayItem("Latitude", "-", "", R.drawable.longitude);
    private rgsDisplayItem dItemStartTime = new rgsDisplayItem("StartTime", "00:00:00", "", R.drawable.start);
    private rgsDisplayItem dItemNowTime = new rgsDisplayItem("CurrentTime", "00:00:00", "", R.drawable.clock_now);
    private rgsDisplayItem dItemMaxSpeed = new rgsDisplayItem("MaxSpeed", "0.00", "km/h", R.drawable.max_speed);
    private rgsDisplayItem dItemAltitude = new rgsDisplayItem("Altitude", "0", "m", R.drawable.altitude);
    private rgsDisplayItem dItemBearing = new rgsDisplayItem("Bearing", "-", "degrees", R.drawable.bearing);
    private rgsDisplayItem dItemNowDate = new rgsDisplayItem("Today", ""+date_fm.format(System.currentTimeMillis()), "", R.drawable.date);
    private rgsDisplay display1 = new rgsDisplay();
    private rgsDisplay display2 = new rgsDisplay();
    private rgsDisplay display3 = new rgsDisplay();
    private rgsDisplay display4 = new rgsDisplay();
    private rgsDisplay display5 = new rgsDisplay();

    private List<rgsDisplayItem> itemarraytest = new LinkedList<rgsDisplayItem>();
    private File gpxLogFile, tcxLogFile;
    private String tcxFileName,gpxFileName;

    private AlertDialog alertDialogStart;

    Timer refreshDisplayTimer; //to run a timer for refreshing the screen every second:
    TimerTask timerRefreshDisplayTask;
    final Handler handlerDisplay = new Handler();

    Timer refreshLogTimer; //to run a timer for refreshing the screen every second:
    TimerTask timerLogTask;
    final Handler handlerLog = new Handler();

    //Internal status
    private boolean wroteLogEntryGpx = false;
    private boolean wroteLogEntryTcx = false;
    private boolean waitForGpsAlertBox = false;

    //Settings
    private int setting_band = 1; //1="use band"; 0="not use band"
    private int setting_gps = 1; //1="use gps"; 0="not use gps"
    private int setting_gpx_log = 1; //1="log in gpx"; 0="do not log in gpx"
    private int setting_tcx_log = 1; //1="log in tcx"; 0="do not log in tcx"
    private int setting_log =1; // 1=log 0=no log overrides gpx/setting_tcx_log
    private int setting_refresh_logtime= 5000; //Refreshtime in ms
    private int setting_keep_screen_on=1; // keep screen on

    //debugging
    private static final String TAG = BigScreenActivity.class.getSimpleName();
    private boolean DEBUG=true;
    private File debugFile;
    // debug messages by
    //Log.d(TAG, "messsage");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("RGS", "onCreateCalled");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_big_screen);
        //keep device on
        if (setting_keep_screen_on==1) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        //DEBUGGING SETUP
        if (DEBUG) {
            debugFile = new File(getExternalFilesDir(null), "DebugRGS" + System.currentTimeMillis());
            debugMessage("Debugging started; app started");
        }


        //link TextViews to xml
        startButton = (Button) findViewById(R.id.btn_start);
        nr_satsTV = (TextView) findViewById(R.id.tv_sats_in);
        gpsstatusTV = (TextView) findViewById(R.id.tv_gpsstatus_in);
        debugTV = (TextView) findViewById(R.id.debug);
        //Start GPS
        // ask Android for the GPS service
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // make a delegate to receive callbacks
        gpsListener = new MyGpsListener();
        // ask for updates on the GPS status
        locationManager.addGpsStatusListener(gpsListener);
        // ask for updates on the GPS location
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                UPDATE_INTERVAL_GPS, 0, gpsListener);
        debugMessage("calling mDefineDisplay");
        mDefineDisplay(); //Connect the displayitems with the different displays and textviews/imageviews in xml
        debugMessage("returned from mDefineDisplay gracefully");

        display1.displayImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                display1.nextDisplayItem();
                debugMessage("Changed display 1 to " + display1.getCurrentItem().inputType);
            }
        });

        display2.displayImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                display2.nextDisplayItem();
                Toast.makeText(getApplicationContext(), "Changed display 2 to " + display2.getCurrentItem().inputType, Toast.LENGTH_SHORT).show();
            }
        });

        display3.displayImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                display3.nextDisplayItem();
                Toast.makeText(getApplicationContext(), "Changed  3 to " + display3.getCurrentItem().inputType, Toast.LENGTH_SHORT).show();
            }
        });

        display4.displayImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                display4.nextDisplayItem();
                Toast.makeText(getApplicationContext(), "Changed  4 to " + display4.getCurrentItem().inputType, Toast.LENGTH_SHORT).show();
            }
        });

        display5.displayImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Changed  5 to " + display5.getCurrentItem().inputType, Toast.LENGTH_SHORT).show();
                display5.nextDisplayItem();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugMessage("entered startButton Click (228)");
                switch (appStatus) {
                    case 0:  // initial case -> running
                        if (gpsFix) {
                            Toast.makeText(getApplicationContext(), "Started...", Toast.LENGTH_SHORT).show();
                            StartRunningApp();
                            debugMessage("returned from StartRunningApp (234)");
                        }
                        else
                        {
                            //Alertbox start
                            AlertDialog.Builder alertDialogBuilderStart = new AlertDialog.Builder(
                                    context);
                            // set title
                            alertDialogBuilderStart.setTitle("Starting...");
                            // set dialog message
                            alertDialogBuilderStart
                                    .setMessage("Waiting for GPS")
                                    .setCancelable(false)
                                    .setPositiveButton("Do not wait", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // if this button is clicked,
                                            waitForGpsAlertBox = false;
                                            StartRunningApp();
                                            dialog.cancel();
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            waitForGpsAlertBox = false;
                                            dialog.cancel();
                                        }
                                    });
                            // create alert dialog
                            alertDialogStart = alertDialogBuilderStart.create();
                            Dialog dialogStart = new Dialog(context);
                            // show it
                            waitForGpsAlertBox = true;
                            debugMessage("make Alertbox Wait for GPS (266)");
                            alertDialogStart.show();
                            debugMessage("made Alertbox Wait for GPS (268)");
                        }
                        break;
                    case 1:
                        appStatus = 2; //set status to pause
                        //Alertbox for pause
                        debugMessage("entered click for pause (274)");
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                context);
                        // set title
                        alertDialogBuilder.setTitle("PAUSED");
                        // set dialog message
                        alertDialogBuilder
                                .setMessage("Please select.")
                                .setCancelable(true)
                                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // if this button is clicked,
                                        appStatus = 1; //set status again to running
                                        debugMessage("dialog cancel clicked for pause /cancel/ (287)");
                                        dialog.cancel();
                                    }
                                })
                                .setNegativeButton("Stop", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        appStatus = 0; //set status to stop
                                        startButton.setText("Start");
                                        //stopApp();
                                        if (setting_gpx_log==1 && (!(gpxLogFile==null))) {
                                            if (wroteLogEntryGpx) {
                                                appendGpxLog(getString(R.string.gpx_closing_script));
                                            }
                                            else
                                            {
                                                gpxLogFile.delete();
                                            }
                                            mSetGpxFilename();
                                            debugMessage("returned from mSetGPXFilename (300) gracefully");
                                        }
                                        if (setting_tcx_log==1 && (!(tcxLogFile==null))) {
                                            if (wroteLogEntryTcx) {
                                                rewriteTcx();
                                            }
                                            else
                                            {
                                                tcxLogFile.delete();
                                            }
                                            mSetTcxFilename();
                                            debugMessage("returned from mSetFilename (311) gracefully");
                                        }
                                        debugMessage("dialog cancel clicked for pause /stop/ (318)");
                                        dialog.cancel();
                                    }
                                });
                        // create alert dialog
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        // show it
                        alertDialog.show();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), "Waiting for GPSFix", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        Log.d("RGS", "onPause called");
        super.onPause();
        Log.d("RGS", "calling stopDisplayRefreshTimer");
        stopDisplayRefreshTimer();
        Log.d("RGS", "calling stopListening");
        stopListening();
    }

    @Override
    protected void onResume() {
        Log.d("RGS", "onResume called");
        super.onResume();
        new appTask().execute();
        // ask for updates on the GPS status
        locationManager.addGpsStatusListener(gpsListener);
        // ask for updates on the GPS location
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                UPDATE_INTERVAL_GPS, 0, gpsListener);
        startDisplayRefreshTimer();
        refreshDisplay();
        // for automatic start decomment below
        //if (setting_band==1) {mInitializeBand();} //use band if 1
        //if (setting_gps==1) { mInitializeGPS();}
        //appStatus = 1;
        //startButton.setText("Pause");
    }

    @Override
    protected void onDestroy(){
        Log.d("RGS", "OnDestroy called");
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("RGS", "OnStop called");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            Log.d("RGS", "Now in landscape");
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Log.d("RGS", "Now in portrait");
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    public void StartRunningApp(){
        Log.d("RGS", "StartRunningApp called");
        debugMessage("entered StartRunning App 360");
        mInitializeDisplayItems(); //Reset the Text of dItems with initialString
        mResetValues(); //Reset all old values
        if (setting_log==1) { //If to log setup Logging gpx/tcx depending on their settings
            fileTime=System.currentTimeMillis();
            if (setting_gpx_log == 1) {
                mInitializeGpxLog();
            }
            if (setting_tcx_log == 1) {
                mInitializeTcxLog();
            }
            startLogTimer();
        }
        if (setting_band == 1) { //initialize band if setting=1
            mInitializeBand();
        }
        startButton.setText("Pause"); // Turn status of app to on
        appStatus = 1; //set status to begin
        startDisplayRefreshTimer(); //start the timer for refreshing display
        debugMessage("left StartRunning App 379");
    }

    public void stopApp()    {
        Log.d("RGS", "StopApp called");
        Log.d("RGS", "calling stopDisplayRefreshTimer");
        stopDisplayRefreshTimer();
        if (setting_log==1) {
            Log.d("RGS", "calling stopLogTimer");
            stopLogTimer();}
        Log.d("RGS", "calling stopListening");
        stopListening();
    }

    public void startLogTimer() {
        //set a new Timer
        refreshLogTimer = new Timer();
        //initialize the TimerTask's job
        initializeLogTimerTask();
        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        refreshLogTimer.schedule(timerLogTask, setting_refresh_logtime, setting_refresh_logtime); //
    }

    public void stopLogTimer() {
        //stop the timer, if it's not already null
        if (refreshLogTimer != null) {
            refreshLogTimer.cancel();
            refreshLogTimer = null;
        }
    }

    public void initializeLogTimerTask(){
        timerLogTask = new TimerTask() {
            @Override
            public void run() {
                handlerLog.post(new Runnable() {
                    public void run(){
                        writeLogSentence();
                    }
                }
                );
            }};
    }

    public void startDisplayRefreshTimer() {
        //set a new Timer
        debugMessage("entered startDisplayRefreshtimes 420");
        refreshDisplayTimer = new Timer();
        //initialize the TimerTask's job
        initializeRefreshDisplayTimerTask();
        //schedule the timer, after the first 1000ms the TimerTask will run every 10000ms
        refreshDisplayTimer.schedule(timerRefreshDisplayTask, UPDATE_DISPLAY, UPDATE_DISPLAY); //
        debugMessage("leave startDisplayRefreshtimes 426");
    }

    public void stopDisplayRefreshTimer() {
              //stop the timer, if it's not already null
        if (refreshDisplayTimer != null) {
            refreshDisplayTimer.cancel();
            refreshDisplayTimer = null;
        }
    }

    public void initializeRefreshDisplayTimerTask(){
        timerRefreshDisplayTask = new TimerTask() {
            @Override
            public void run() {
                handlerDisplay.post(new Runnable() {
                    public void run(){
                   refreshDisplay();
                    }
                }
                );
        }};
    }

    //Connect the displayitems with the different displays and textviews/imageviews in xml
    private void mDefineDisplay() {
        debugMessage("entered mDefineDispaly 452");
        display1.setDisplayIds(R.id.rgstextview1, R.id.rgsimageview1, R.id.rgsunit1);
        display2.setDisplayIds(R.id.rgstextview2, R.id.rgsimageview2, R.id.rgsunit2);
        display3.setDisplayIds(R.id.rgstextview3, R.id.rgsimageview3, R.id.rgsunit3);
        display4.setDisplayIds(R.id.rgstextview4, R.id.rgsimageview4, R.id.rgsunit4);
        display5.setDisplayIds(R.id.rgstextview5, R.id.rgsimageview5, R.id.rgsunit5);
        display1.displayItem.add(dItemSpeed);
        display1.displayItem.add(dItemAvgSpeed);
        display1.displayItem.add(dItemMaxSpeed);
        display2.displayItem.add(dItemDuration);
        display2.displayItem.add(dItemStartTime);
        display2.displayItem.add(dItemNowTime);
        display2.displayItem.add(dItemNowDate);
        display3.displayItem.add(dItemDistance);
        display3.displayItem.add(dItemAltitude);
        display4.displayItem.add(dItemHeartRate);
        display4.displayItem.add(dItemAvgHeartRate);
        display4.displayItem.add(dItemMaxHeartRate);
        display4.displayItem.add(dItemMinHeartRate);
        display4.displayItem.add(dItemCalories);
        display5.displayItem.add(dItemCalories);
        dItemNowDate.initialString = "" + date_fm.format(System.currentTimeMillis());
        display1.setDisplayItem(0);
        display2.setDisplayItem(0);
        display3.setDisplayItem(0);
        display4.setDisplayItem(0);
        display5.setDisplayItem(0);
    }

    //Reset the Text of dItems with initialString
    private void mInitializeDisplayItems() {
        debugMessage("entered mInitializeDisplayItems 480");
        dItemCalories.setText(dItemCalories.initialString);
        dItemAltitude.setText(dItemAltitude.initialString);
        dItemAvgHeartRate.setText(dItemAvgHeartRate.initialString);
        dItemAvgSpeed.setText(dItemAvgSpeed.initialString);
        dItemBearing.setText(dItemBearing.initialString);
        dItemDuration.setText(dItemDuration.initialString);
        dItemDistance.setText(dItemDistance.initialString);
        dItemHeartRate.setText(dItemHeartRate.initialString);
        dItemLongitude.setText(dItemLongitude.initialString);
        dItemLatitude.setText(dItemLatitude.initialString);
        dItemMaxSpeed.setText(dItemMaxSpeed.initialString);
        dItemMaxHeartRate.setText(dItemMaxHeartRate.initialString);
        dItemNowTime.setText(dItemNowTime.initialString);
        dItemNowDate.setText(dItemNowDate.initialString);
        dItemSpeed.setText(dItemSpeed.initialString);
        dItemStartTime.setText(dItemStartTime.initialString);
        }

    private void mSetGpxFilename(){
        gpxFileName = "track_" + gpxdate_fm.format(fileTime) + "T" + gpxtime_fm.format(fileTime).replaceAll(":", "_")+"_"+(fileTime%1000) + ".gpx";
        gpxLogFile = new File(getExternalFilesDir(null), gpxFileName);
   }

    private void mSetTcxFilename(){
        tcxFileName = "track_" + gpxdate_fm.format(fileTime) + "T" + gpxtime_fm.format(fileTime).replaceAll(":", "_")+"_"+(fileTime%1000) + ".tcx.temp";
        tcxLogFile = new File(getExternalFilesDir(null), tcxFileName);
    }

    private void mInitializeGpxLog() {
        mSetGpxFilename();
        appendGpxLog(getString(R.string.GPX_initials));
        appendGpxLog("<name><![CDATA[08.09.2014 8:22]]></name>");
        appendGpxLog("<desc><![CDATA[]]></desc>");
        appendGpxLog("</metadata>");
        appendGpxLog("<trk>");
        appendGpxLog("<name><![CDATA[08.09.2014 8:22]]></name>");
        appendGpxLog("<desc><![CDATA[]]></desc>");
        appendGpxLog("<type><![CDATA[Fahrrad fahren]]></type>");
        appendGpxLog("<trkseg>");
    }

    private void mInitializeTcxLog() {
        mSetTcxFilename();
        appendTcxLog("<Track>");
    }

    private void mInitializeBand() {
        new appTask().execute();
        beginCalories = -1;
    }

    private void mResetValues() {
        wroteLogEntryTcx=false;
        wroteLogEntryGpx=false;
        long nowTime = System.currentTimeMillis();
        startTime = nowTime;
        lastTime = nowTime;
        nowLat=0;
        nowLon=0;
        nowAltitude=0;
        avgSpeed=0;
        maxSpeed=0;
        nowSpeed=0;
        durationMillis = 0;
        gpsTotalDistance = 0;
        durationNoSpeedMillis = 0;
        nowBearing=0;
        nowHeartRate=0;
        maxHeartRate=0;
        minHeartRate=500;
        beginCalories = -1;
        nowCalories=0;
        avgHeartBeat=0;
        lastLocationValid=false;
        dItemStartTime.setText(time_fm.format(startTime));
        gpsstatusTV.setText("start");
        refreshDisplay();
    }

    private void stopListening() {
        locationManager.removeUpdates(gpsListener);
        //locationManager.removeUpdates();
        if (client != null) {
            try {
                // client.getSensorManager().unregisterAccelerometerEventListeners();
                client.getSensorManager().unregisterHeartRateEventListeners();
                client.getSensorManager().unregisterCaloriesEventListeners();
                //registerHeartRateEventListener
            } catch (BandIOException e) {
                appendToUI(e.getMessage());
            }
        }

    }

    private void refreshDisplay() {
        dItemNowTime.setText(time_fm.format(System.currentTimeMillis()));
        dItemNowDate.setText(date_fm.format(System.currentTimeMillis()));
        display1.setText(display1.getCurrentItem().text);
        display2.setText(display2.getCurrentItem().text);
        display3.setText(display3.getCurrentItem().text);
        display4.setText(display4.getCurrentItem().text);
        display5.setText(display5.getCurrentItem().text);
    }

    private void updateLocation() {
        debugMessage("entered updateLocation (579)");
        long nowTime = System.currentTimeMillis();
        if (lastLocationValid){
            if (appStatus == 1) { //update only if not paused
                debugMessage("in updateLocation (605)");
                durationMillis += (nowTime - lastTime);    //running in appStatus =1
                    if (nowLocation.getAccuracy() < MIN_ACCURACY_FOR_DISTANCE && nowLocation.hasSpeed()) {
                            if (nowLocation.getSpeed() == 0) { //adjust duration without speed
                                debugMessage("in updateLocation (611)");
                                durationNoSpeedMillis += (nowTime - lastTime);
                            } else {
                                debugMessage("in updateLocation (613)");
                                if (!(lastLocation==null)) {
                                    if (lastLocation.getTime()>startTime) {
                                        gpsTotalDistance += nowLocation.distanceTo(lastLocation); //adjust distance
                                    }
                                }
                                if (maxSpeed < nowLocation.getSpeed() * 3.6) { //adjust maxspeed if needed
                                    maxSpeed = nowLocation.getSpeed() * 3.6;
                                    debugMessage("in updateLocation (618)");
                                    dItemMaxSpeed.setText(String.format("%.2f", maxSpeed));
                                }
                            }
                    }
                debugMessage("in updateLocation (626)");
                if (avgHeartBeat < 30) { //check if avgHeartBeat was intialized
                    avgHeartBeat = nowHeartRate; //basically initialize avgHeartRate
                    dItemAvgHeartRate.setText(String.format("%.2f", avgHeartBeat));
                } else {
                    if (durationMillis > 0 && nowHeartRate > 30) { //update avgHeartBeat
                        avgHeartBeat = (avgHeartBeat * (durationMillis) + nowHeartRate * (nowTime - lastTime)) / (durationMillis + nowTime - lastTime);
                        dItemAvgHeartRate.setText(String.format("%.2f", avgHeartBeat));
                    }
                }
                if (durationMillis > 0) {
                    avgSpeed = gpsTotalDistance / (durationMillis) * 3600.;//adjust average speed
                    dItemAvgSpeed.setText(String.format("%.2f", avgSpeed)); //write in ItemId
                }
                //Get GPS variables
                nowLat = nowLocation.getLatitude(); //get Lat
                dItemLatitude.setText("" + nowLat); // write in ItemId
                nowLon = nowLocation.getLongitude(); // get Lon
                dItemLongitude.setText("" + nowLon); // write in ItemId
                nowAltitude = nowLocation.getAltitude(); // get alt
                dItemAltitude.setText(String.format("%.2f", nowAltitude)); //write in ItemId
                nowBearing = nowLocation.getBearing(); //get bearing
                dItemBearing.setText("" + nowBearing);  // write in ItemId
                if (nowLocation.hasSpeed()) {
                    nowSpeed = nowLocation.getSpeed() * 3.6; // get now Speed
                    dItemSpeed.setText(String.format("%.2f", nowSpeed)); // write in Itemid
                } else {
                    dItemSpeed.setText("--");
                }
                //formatting of output
                String preHours = "";
                String preMinutes = "";
                String preSeconds = "";
                double timePassedInSec = (durationMillis) / 1000.;
                int hoursRun = (int) (timePassedInSec / 3600);
                int minutesRun = (int) (timePassedInSec / 60 - hoursRun * 60);
                if (hoursRun < 10) {
                    preHours = "0";
                }
                if (minutesRun < 10) {
                    preMinutes = "0";
                }
                int secondsRun = (int) (timePassedInSec - hoursRun * 3600 - minutesRun * 60);
                if (secondsRun < 10) {
                        preSeconds = "0";
                    }
                    //Toast.makeText(getApplicationContext(), "Date= "+DateFormat.getDateInstance().format(nowTime), Toast.LENGTH_SHORT).show();
                    //Display gps variables when not in 0 app.status
                    if (!(appStatus == 0)) {
                        dItemDistance.setText(String.format("%.2f", gpsTotalDistance / 1000));
                        dItemDuration.setText(preHours + hoursRun + ":" + preMinutes + minutesRun + ":" + preSeconds + secondsRun);
                    }
            }
        }
        lastTime = nowTime;
        lastLocation = nowLocation;
        lastLocationValid=true; // after lastLocation is defined set lastLocationValid to true
        debugMessage("exit updateLocation (667)");
    }

    private void writeLogSentence() {
        debugMessage("entered writeLogSentence (670");
        long logTimemillis = System.currentTimeMillis();
        debugTV.setText(" Diff: "+(logTimemillis-dItemLatitude.timeStamp));
        if (logTimemillis-dItemLatitude.timeStamp<setting_refresh_logtime-UPDATE_INTERVAL_GPS) { //Write log only if information not too old
            debugTV.setText("I write entry; Diff: "+(logTimemillis-dItemLatitude.timeStamp));
            int logRestMillis = (int) (logTimemillis % 1000);
            String logTimeString = gpxdate_fm.format(logTimemillis) + "T" + gpxtime_fm.format(logTimemillis) + "." + logRestMillis;
            // do gpx
            if (setting_gpx_log == 1) {
                String gpxSentence = "<trkpt lat=\"" + dItemLatitude.text + "\" lon=\"" + dItemLongitude.text + "\">\n";
                gpxSentence = gpxSentence + "<ele>" + dItemAltitude.text + "</ele>\n";
                gpxSentence = gpxSentence + "<time>" + logTimeString + "Z</time>\n";
                if (setting_band == 1) {
                    gpxSentence = gpxSentence + "<extensions>\n<gpxtpx:TrackPointExtension>\n<gpxtpx:hr>" + dItemHeartRate.text + "</gpxtpx:hr>\n</gpxtpx:TrackPointExtension>\n</extensions>";
                }
                gpxSentence = gpxSentence + "</trkpt>";
                appendGpxLog(gpxSentence); //write gpx sentence to gpx log file
                wroteLogEntryGpx=true;
            }
            //now do tcx
            debugMessage("in writeLogSentence (699");
            if (setting_tcx_log == 1) {
                String tcxSentence = "<Trackpoint>\n<Time>" + logTimeString + "Z</Time>\n<Position>\n<LatitudeDegrees>";
                tcxSentence = tcxSentence + dItemLatitude.text + "</LatitudeDegrees>\n<LongitudeDegrees>";
                tcxSentence = tcxSentence + dItemLongitude.text + "</LongitudeDegrees>\n</Position>\n<AltitudeMeters>";
                tcxSentence = tcxSentence + dItemAltitude.text + "</AltitudeMeters>\n<DistanceMeters>" + dItemDistance.text + "</DistanceMeters>\n";
                if (setting_band == 1) {
                    tcxSentence = tcxSentence + "<HeartRateBpm>\n<Value>" + dItemHeartRate.text + "</Value>\n</HeartRateBpm>\n";
                } else {
                    tcxSentence = tcxSentence + "<SensorState>Absent</SensorState>\n";
                }
                tcxSentence = tcxSentence + "</Trackpoint>";
                appendTcxLog(tcxSentence); //write tcx sentence to tcx log file
                wroteLogEntryTcx=true;
                debugMessage("leaving writeLogSentence (713)");
            }
        }
    }

    private String getGrade(float accuracy) {
        debugMessage("entered getGrade (709)");
        if (!gpsEnabled) {
            return "Disabled";
        }
        if (!gpsFix) {
            return "NoFix";
        }
        if (accuracy <= 10) {
            return "Good";
        }
        if (accuracy <= 30) {
            return "Fair";
        }
        if (accuracy <= 100) {
            return "Bad";
        }
        return "Unusable";
    }


//    private void updateSpeedHistory(float value) {
//        //adjust average
//        avgSpeed = (avgSpeed*speedhistory.size()+value)/(1+speedhistory.size());
//        // add value to speed history
//        speedhistory.add(value);
//        }

    private void updateRollingAverage(float value) {
        debugMessage("entered updateRollingAcerage (737)");
        // does a simple rolling average
        rollingAverageData.add(value);
        if (rollingAverageData.size() > 10) {
            rollingAverageData.remove(0);
        }
        float average = 0.0f;
        for (Float number : rollingAverageData) {
            average += number;
        }
        average = average / rollingAverageData.size();
        gpsAccuracy = average;
    }

    // Define a gps listener
    protected class MyGpsListener implements GpsStatus.Listener, LocationListener {
        private int satellitesTotal, satellitesUsed;
        private long locationTime = 0;

        @Override
        public void onLocationChanged(Location location) {
            debugMessage("entered onLoationChanged (758)");
            if (locationManager != null) {
                nowLocation = location;
                if (nowLocation.hasAccuracy()) {
                    gpsTime=nowLocation.getTime();
                    if (waitForGpsAlertBox) {
                        waitForGpsAlertBox=false;
                        alertDialogStart.dismiss();
                        StartRunningApp();
                    }
                    // rolling average of accuracy so "Signal Quality" is not erratic
                    updateRollingAverage(nowLocation.getAccuracy());
                    updateLocation();
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


        @Override
        public void onGpsStatusChanged(int changeType) {
            int newSatTotal = 0;
            int newSatUsed = 0;
            //debugTV.setText("Gpsstatus change");
            debugMessage("entered GPSStatusChanged");
            if (locationManager != null) {
                // status changed so ask what the change was
                GpsStatus status = locationManager.getGpsStatus(null);
                switch (changeType) {
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        gpsEnabled = true;
                        gpsFix = true;
                        Toast.makeText(getApplicationContext(), "Got GPSFix", Toast.LENGTH_SHORT).show();
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        gpsEnabled = true;
                        // if it has been more then 10 seconds since the last update, consider the fix lost
                        gpsFix = System.currentTimeMillis() - gpsTime < DURATION_TO_FIX_LOST_MS;
                        break;
                    case GpsStatus.GPS_EVENT_STARTED: // GPS turned on
                        gpsEnabled = true;
                        gpsFix = false;
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED: // GPS turned off
                        gpsEnabled = false;
                        gpsFix = false;
                        break;
                    default:
                        //Log.w(TAG, "unknown GpsStatus event type. "+changeType);
                        return;
                }
                if (gpsFix) {
                    gpsstatusTV.setText(" " + "fix");
                } else {
                    gpsstatusTV.setText(" " + "NoFix");
                }
                if (!gpsEnabled) {
                    gpsstatusTV.setText(" " + "Disabled");
                }
                // number of satellites, not useful, but cool
                for (GpsSatellite sat : status.getSatellites()) {
                    newSatTotal++;
                    if (sat.usedInFix()) {
                        newSatUsed++;
                    }
                }
                satellitesTotal = newSatTotal;
                satellitesUsed = newSatUsed;
                nr_satsTV.setText("#Sat: " + satellitesUsed + "/" + satellitesTotal);

            }
        }
    }

    //BAND STUFF

    private class appTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    startCalorieListener();
                    debugMessage("returned from startCalorieListener (844) gracefully");
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        startHRListener();
                        debugMessage("returned from startHRListener (847) gracefully");
                        //startCalorieListener();
                    } else {
                        // user has not consented yet, request it
                        client.getSensorManager().requestHeartRateConsent(BigScreenActivity.this, mHeartRateConsentListener);
                    }
                    client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);

                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage = "";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage();
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //heartRateTV.setText(string);
                Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayCal(final long Calories) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dItemCalories.setText("" + Calories);

            }
        });
    }

    private void displayHR(final int HRint) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dItemHeartRate.setText("" + HRint);
            }
        });
    }

    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                event.getHeartRate();
                nowHeartRate = event.getHeartRate();
                dItemHeartRate.setText("" + nowHeartRate);
                if (appStatus == 0) {
                    avgHeartBeat = nowHeartRate;
                    dItemAvgHeartRate.setText(String.format("%.2f", avgHeartBeat));
                }
                if (appStatus == 1) { //update only if not paused
                if (maxHeartRate < nowHeartRate) { //adjust max heart rate if needed
                    maxHeartRate = nowHeartRate;
                    dItemMaxHeartRate.setText(""+maxHeartRate);
                }
                if (minHeartRate > nowHeartRate) { //adjust min heart rate if needed
                    minHeartRate = nowHeartRate;
                    dItemMinHeartRate.setText(""+minHeartRate);
                }
               }
            }
        }
    };

    private BandCaloriesEventListener mCaloriesEventListener = new BandCaloriesEventListener() {
        @Override
        public void onBandCaloriesChanged(final BandCaloriesEvent event) {
            if (event != null) {
                nowCalories = event.getCalories();
                if (beginCalories < 1) {
                    beginCalories = nowCalories;
                }
                dItemCalories.setText("" + (nowCalories - beginCalories));
                //displayCal(nowCalories - beginCalories);
            }
        }
    };


    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }
        appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private HeartRateConsentListener mHeartRateConsentListener = new HeartRateConsentListener() {
        @Override
        public void userAccepted(boolean b) {
            // handle user's heart rate consent decision
            if (b) {
                // Consent has been given, start HR sensor event listener
                startHRListener();
            } else {
                // Consent hasn't been given
                appendToUI(String.valueOf(b));
            }
        }
    };

    public void startHRListener() {
        try {
            // register HR sensor event listener
            client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
        } catch (BandIOException ex) {
            appendToUI(ex.getMessage());
        } catch (BandException e) {
            String exceptionMessage = "";
            switch (e.getErrorType()) {
                case UNSUPPORTED_SDK_VERSION_ERROR:
                    exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                    break;
                case SERVICE_ERROR:
                    exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                    break;
                default:
                    exceptionMessage = "Unknown error occurred: " + e.getMessage();
                    break;
            }
            appendToUI(exceptionMessage);

        } catch (Exception e) {
            appendToUI(e.getMessage());
        }
    }

    public void startCalorieListener() {
        try {
            debugMessage("entered startCalorieListener (1010)");
            // register HR sensor event listener
            client.getSensorManager().registerCaloriesEventListener(mCaloriesEventListener);
//Debug OK            appendToUI("should register mCal here");
        } catch (BandIOException ex) {
            appendToUI(ex.getMessage());
        } catch (BandException e) {
            String exceptionMessage = "";
            switch (e.getErrorType()) {
                case UNSUPPORTED_SDK_VERSION_ERROR:
                    exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                    break;
                case SERVICE_ERROR:
                    exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                    break;
                default:
                    exceptionMessage = "Unknown error occurred: " + e.getMessage();
                    break;
            }
            appendToUI(exceptionMessage);

        } catch (Exception e) {
            appendToUI(e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_big_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        debugMessage("entered onOptionsItemSelected (1049)");
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.exit_app) {
            stopApp();
            debugMessage("returned from stopApp (1038) gracefully");
            fileTime=System.currentTimeMillis();
            if (setting_gpx_log==1 && (!(gpxLogFile==null))) {
                if (wroteLogEntryGpx) {
                    appendGpxLog(getString(R.string.gpx_closing_script));
                }
                else
                {
                    gpxLogFile.delete();
                }
                mSetGpxFilename();
            }
            if (setting_tcx_log==1 && (!(tcxLogFile==null))) {
                if (wroteLogEntryTcx) {
                    rewriteTcx();
                    debugMessage("returned from rewriteTcx (1053) gracefully");
                } else {
                    tcxLogFile.delete();
                }
                mSetTcxFilename();
            }
            debugMessage("going to finish App gracefully (1076)");
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A Display object that displays DisplayItems
     */
    public class rgsDisplay {
        public TextView displayTextView;
        public ImageView displayImageView;
        public TextView displayUnit;
        public List<rgsDisplayItem> displayItem = new LinkedList<rgsDisplayItem>();
        public int displayCurrentItem;
        public rgsDisplayItem displayNextItem;

        /**
         * Constructs an empty rgsDisplay object representing a display
         */
        public rgsDisplay() {
            displayCurrentItem = 0;
        }

        /**
         * Returns the text in displayTextView
         *
         * @return the text in displayTextView
         */
        private String getText() {
            return displayTextView.getText().toString();
        }

        /**
         * Connects the display with the xml file
         *
         * @param idTV   the main TextView that is to display the changing data
         * @param idIV   the ImageView that displays the icon associated with the vaules
         * @param idUnit the units of the shown values
         */
        private void setDisplayIds(int idTV, int idIV, int idUnit) {
            displayTextView = (TextView) findViewById(idTV);
            displayImageView = (ImageView) findViewById(idIV);
            displayUnit = (TextView) findViewById(idUnit);
        }

        /**
         * Adds a displayItem in the list collecting the Item to be shown in the display
         *
         * @param Item the displayItem to be added
         */
        private void addDisplayItem(rgsDisplayItem Item) {
            displayCurrentItem = 0;
            //displayItem.add(Item);
        }

        /**
         * Sets the Display to show one of the items in the collection of displayItems
         *
         * @param itemnr the index of the displayItem to be shown
         */
        private void setDisplayItem(int itemnr) {
            if (itemnr >= displayItem.size()) {
                itemnr = displayItem.size() - 1;
            }
            if (itemnr < 0) {
                itemnr = 0;
            }
            displayCurrentItem = itemnr;
            displayUnit.setText(displayItem.get(itemnr).unit);
            displayTextView.setText(displayItem.get(itemnr).text);
            displayImageView.setImageResource(displayItem.get(displayCurrentItem).iconId);
        }

        /**
         * Returns the currently active displayItem
         *
         * @return displayItem
         */
        private rgsDisplayItem getCurrentItem() {
            return displayItem.get(displayCurrentItem);
        }

        /**
         * Sets the Display to show the next Dispayitems in the collection of displayItems
         */
        private void nextDisplayItem() {
            displayCurrentItem++;
            if (displayCurrentItem >= displayItem.size()) {
                displayCurrentItem = 0;
            }
            displayUnit.setText(displayItem.get(displayCurrentItem).unit);
            displayTextView.setText(displayItem.get(displayCurrentItem).text);
            displayImageView.setImageResource(displayItem.get(displayCurrentItem).iconId);
        }

        private void setText(String rgsText) {
            displayTextView.setText(rgsText);
        }

        private void setUnit(String rgsText) {
            displayUnit.setText(rgsText);
        }

    }

    public void appendGpxLog(String text) {
        if (setting_gpx_log == 1 && (!(gpxLogFile==null))) {
            appendToFile(text, gpxLogFile);
        }
    }

    public void appendTcxLog(String text) {
        if (setting_tcx_log == 1 && (!(tcxLogFile==null))) {
            appendToFile(text, tcxLogFile);
        }
    }

    //rewrite file to get the tcx right with proper begining and ending
    public void rewriteTcx(){
        int fileNameLength=tcxFileName.length();
        if (tcxFileName.substring(fileNameLength-8).equals("tcx.temp")) {
            String fn=tcxFileName.substring(0,fileNameLength-5);
            File tcxProperFile=new File(getExternalFilesDir(null), fn);
            appendToFile("<TrainingCenterDatabase xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2 http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\">",tcxProperFile);
            appendToFile("<Activities>",tcxProperFile);
            appendToFile("<Activity Sport=\"TheActivity\">",tcxProperFile);
            String gpxTime = gpxdate_fm.format(startTime) + "T" + gpxtime_fm.format(startTime) + "Z";
            appendToFile("<Id>"+gpxTime+"</Id>",tcxProperFile);
            appendToFile("<Lap StartTime=\""+gpxTime+"\">",tcxProperFile);
            appendToFile("<TotalTimeSeconds>"+durationMillis/1000+"</TotalTimeSeconds>",tcxProperFile);
            appendToFile("<DistanceMeters>"+gpsTotalDistance+"</DistanceMeters>",tcxProperFile);
            appendToFile("<MaximumSpeed>"+maxSpeed+"</MaximumSpeed>",tcxProperFile);
            appendToFile("<Calories>"+(nowCalories-beginCalories)+"</Calories>",tcxProperFile);
            appendToFile("<AverageHeartRateBpm><Value>"+String.format("%.2f", avgHeartBeat)+"</Value></AverageHeartRateBpm>",tcxProperFile);
            appendToFile("<MaximumHeartRateBpm><Value>"+maxHeartRate+"</Value></MaximumHeartRateBpm>",tcxProperFile);
            appendToFile("<Intensity>Active</Intensity><TriggerMethod>Location</TriggerMethod>",tcxProperFile);
            appendToFile(readTcxTemp(tcxLogFile),tcxProperFile); //copy file context of temp to tcxFile
            appendToFile("</Track>\n</Lap>\n</Activity>\n</Activities>\n</TrainingCenterDatabase>",tcxProperFile);
            tcxLogFile.delete();
        }
    }

    public String readTcxTemp(File file) {
        //Read text from file
    StringBuilder text = new StringBuilder();
    try
    {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            text.append(line);
            text.append('\n');
        }
        br.close();
    }
    catch(
    IOException e
    )
    {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
        return text.toString();
    }

    public void appendToFile(String text, File filename) {
            if (!filename.exists()) {
                try {
                    filename.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    debugMessage("exception 1249");
                    e.printStackTrace();
                }
            }
            try {
                //BufferedWriter for performance, true to set append to file flag
                BufferedWriter buf = new BufferedWriter(new FileWriter(filename, true));
                buf.append(text);
                buf.newLine();
                buf.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    private void debugMessage(String message){
        if (DEBUG) {
            appendToFile(time_fm.format(System.currentTimeMillis()) + ":" + (System.currentTimeMillis() % 1000) +": " + message, debugFile);
        }
    }

//end
}