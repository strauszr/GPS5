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


import android.content.Context;
import android.content.DialogInterface;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.w3c.dom.Text;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends ActionBarActivity {
    Button startButton, pauseButton, resetButton;
    private TextView latTV, lonTV, speedTV, provTV, altitudeTV, bearingTV, accuracyTV, timeTV, datumTV, systimeTV, avgspeedTV;
    private TextView nr_satsTV, gpsstatusTV, distanceTV, runTimeTV, corrDTV, appStateTV,startTimeTV,maxSpeedTV,heartRateTV,calTV;
    //LocationManager locationManager;
    //private String PROVIDER = LocationManager.GPS_PROVIDER;
    private String prov;
    private float gpsTotalDistance = 0;
    private long gpsTime, lastTime, startTime;
    private int durationMillis = 0;
    private int durationNoSpeedMillis =0;
    private float correctDistance;
    private boolean gpsEnabled;
    private boolean gpsFix = false;
    private double lat, lon, altitude, nowSpeed,avgSpeed,maxSpeed;
    private float gpsAccuracy = 0;
    private float speed, bearing;
    private static final long DURATION_TO_FIX_LOST_MS = 10000;
    private List<Float> rollingAverageData = new LinkedList<Float>();
    private List<Float> speedHistory = new LinkedList<Float>();
    private long timeStart = System.currentTimeMillis();
    private int appStatus = 0; // 0: start; 1: running; 2: pause
    final int UPDATE_INTERVAL_GPS = 500;
    final int MIN_ACCURACY_FOR_DISTANCE = 50;
    final Context context = this;
    private LocationManager locationManager;
    private MyGpsListener gpsListener;
    private Location lastLocation,nowLocation;
    private SimpleDateFormat time_fm = new SimpleDateFormat ("HH:mm:ss");

    private BandClient client = null;
    private int nowHeartRate;
    private long beginCalories=-1;
    private long nowCalories;
    private int bandSetting=1; //1="use band"; 0="not use band"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //keep device on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //link textviews to xml
        startButton = (Button) findViewById(R.id.btn_start);
        //pauseButton=(Button)findViewById(R.id.btn_pause);
        latTV = (TextView) findViewById(R.id.tv_lat_in);
        lonTV = (TextView) findViewById(R.id.tv_lon_in);
        speedTV = (TextView) findViewById(R.id.tv_speed_in);
        avgspeedTV = (TextView) findViewById(R.id.tv_avgspeed_in);
        provTV = (TextView) findViewById(R.id.tv_provider_in);
        altitudeTV = (TextView) findViewById(R.id.tv_altitude_in);
        distanceTV = (TextView) findViewById(R.id.tv_distance_in);
        bearingTV = (TextView) findViewById(R.id.tv_bearing_in);
        accuracyTV = (TextView) findViewById(R.id.tv_accuracy_in);
        datumTV = (TextView) findViewById(R.id.tv_date_in);
        systimeTV = (TextView) findViewById(R.id.tv_systime_in);
        timeTV = (TextView) findViewById(R.id.tv_time_in);
        nr_satsTV = (TextView) findViewById(R.id.tv_sats_in);
        gpsstatusTV = (TextView) findViewById(R.id.tv_gpsstatus_in);
        runTimeTV = (TextView) findViewById(R.id.tv_runtime_in);
        corrDTV = (TextView) findViewById(R.id.tv_corrD_in);
        appStateTV = (TextView) findViewById(R.id.tv_app_state_in);
        startTimeTV = (TextView) findViewById(R.id.tv_startttime_in);
        maxSpeedTV = (TextView) findViewById(R.id.tv_maxspeed_in);
        heartRateTV = (TextView) findViewById(R.id.tv_heartrate_in);
        calTV=(TextView) findViewById(R.id.tv_cal_in);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (appStatus) {
                    case 0:  // initial case -> running
                        mInitializeGPS();
                        if (bandSetting==1) {mInitializeBand();} //use band if 1
                        startButton.setText("Pause");
                        Toast.makeText(getApplicationContext(), "Starting...", Toast.LENGTH_SHORT).show();
                        appStatus = 1; //set status to begin
                        break;
                    case 1:
                        appStatus = 2; //set status to pause
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
                                        appStatus = 1; //set status to running
                                        dialog.cancel();
                                    }
                                })
                                .setNegativeButton("Stop", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        appStatus = 0; //set status to stop
                                        stopListening();
                                        startButton.setText("Start");
                                        stopListening();
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
                appStateTV.setText("" + appStatus);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopListening();
    }

    @Override
    protected void onResume() {
        //super.onResume();
        super.onResume();
        // ask Android for the GPS service
       locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // make a delegate to receive callbacks
        gpsListener = new MyGpsListener();
    }

    private void mInitializeBand(){
        new appTask().execute();
        beginCalories=-1;
        startTimeTV.setText("" + time_fm.format(startTime));
        calTV.setText("");
        heartRateTV.setText("");
    }

    private void mInitializeGPS(){
        // ask for updates on the GPS status
        locationManager.addGpsStatusListener(gpsListener);
        // ask for updates on the GPS location
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                UPDATE_INTERVAL_GPS, 0, gpsListener);
        long nowTime = System.currentTimeMillis();
        durationMillis = 0;
        gpsTotalDistance = 0;
        durationNoSpeedMillis =0;
        maxSpeed=0;
        startTime=nowTime;
        startTimeTV.setText("00:00:00");
        maxSpeedTV.setText("00");
        systimeTV.setText("" + time_fm.format(nowTime));
        datumTV.setText("" + DateFormat.getDateInstance().format(nowTime));
        latTV.setText("-");
        lonTV.setText("-");
        speedTV.setText("0.00");
        avgspeedTV.setText("0.00");
        provTV.setText("-");
        altitudeTV.setText("-");
        bearingTV.setText("-");
        accuracyTV.setText("-");
        timeTV.setText("00:00:00");
        distanceTV.setText("0.00");
        runTimeTV.setText("00:00:00");
        lastTime = nowTime;
        gpsstatusTV.setText(" ");
    }

    private void stopListening(){
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

    private void showLocation() {
        long nowTime = System.currentTimeMillis();
        if (appStatus==1) { //update only if not paused
                durationMillis += (nowTime - lastTime);    //normal running
                if (nowLocation.hasAccuracy()) {
                    if (nowLocation.getAccuracy()<MIN_ACCURACY_FOR_DISTANCE) {
                        if (nowLocation.hasSpeed()) {
                            if (nowLocation.getSpeed() == 0) {
                                durationNoSpeedMillis += (nowTime - lastTime);
                            } else {
                                gpsTotalDistance += nowLocation.distanceTo(lastLocation);
                                if (maxSpeed < nowLocation.getSpeed() * 3.6) {
                                    maxSpeed = nowLocation.getSpeed() * 3.6;
                                    maxSpeedTV.setText("" + String.format("%.2f", maxSpeed));
                                }
                            }
                        }
                        ;
                    }
                }
        }
        if (durationMillis>0){avgSpeed = gpsTotalDistance / (durationMillis) * 3600.;}
        lastTime = nowTime;
        if (nowLocation.hasAccuracy())
        {
            lastLocation = nowLocation;
            //Get GPS variables
            lat = nowLocation.getLatitude();
            lon = nowLocation.getLongitude();
            prov = nowLocation.getProvider();
            altitude = nowLocation.getAltitude();
            bearing = nowLocation.getBearing();
            gpsTime = nowLocation.getTime();
            if (nowLocation.hasSpeed()) {
                nowSpeed = nowLocation.getSpeed() * 3.6;
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
            //String avgSpeedString=String.format("%.2f", avgSpeed);
            //Date DatumGPS= new Date(GPStime);
            //Date DatumSys= new Date(System.currentTimeMillis());
            //SimpleDateFormat time_fm = new SimpleDateFormat ("HH:mm:ss");
            //SimpleDateFormat date_fm = new SimpleDateFormat ("dd.MM.yyyy");
            //Display nowTime
            systimeTV.setText("" + time_fm.format(nowTime));
            //datumTV.setText("" + date_fm.format(DatumGPS));
            //Display date of today
            datumTV.setText("" + DateFormat.getDateInstance().format(nowTime));
            //Display gps variables when not in 0 app.status
            if (!(appStatus == 0)) {
                latTV.setText("" + String.format("%.6f", lat));
                lonTV.setText("" + String.format("%.6f", lon));
                if (nowLocation.hasSpeed()) {
                    speedTV.setText("" + String.format("%.2f", nowSpeed));
                } else {
                    speedTV.setText("--");
                }
                avgspeedTV.setText("" + String.format("%.2f", avgSpeed));
                provTV.setText("" + nowLocation.getProvider());
                altitudeTV.setText("" + String.format("%.2f", altitude));
                bearingTV.setText("" + bearing);
                accuracyTV.setText("" + getGrade(gpsAccuracy));
                timeTV.setText("" + time_fm.format(gpsTime));
                distanceTV.setText("" + String.format("%.2f", gpsTotalDistance/1000));
                //corrDTV.setText("" + correctDistance);
                appStateTV.setText("" + appStatus);
                runTimeTV.setText(preHours + hoursRun + ":" + preMinutes + minutesRun + ":" + preSeconds + secondsRun);
            }
        }
    }



    private String getGrade(float accuracy) {

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
            if (locationManager != null) {
                nowLocation = location;
                if (nowLocation.hasAccuracy()) {
                    // rolling average of accuracy so "Signal Quality" is not erratic
                    updateRollingAverage(nowLocation.getAccuracy());
                }
                showLocation();
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
                    gpsstatusTV.setText(" "+"fix");
                } else {
                    gpsstatusTV.setText(" "+"NoFix");
                }
                if (!gpsEnabled) {
                    gpsstatusTV.setText(" "+"Disabled");
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
    };

    //BAND STUFF

    private class appTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    startCalorieListener();
                    if(client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        startHRListener();
                        //startCalorieListener();
                    } else {
                        // user has not consented yet, request it
                        client.getSensorManager().requestHeartRateConsent(MainActivity.this, mHeartRateConsentListener);
                    }
                    client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);

                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
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
                Toast.makeText(getApplicationContext(), string, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayCal(final long Calories) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                calTV.setText("" + Calories);
            }
        });
    }

    private void displayHR(final int HRint) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                heartRateTV.setText("" + HRint);
            }
        });
    }

    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                event.getHeartRate();
                nowHeartRate=event.getHeartRate();
                displayHR(nowHeartRate);
            }
        }

    };

    private BandCaloriesEventListener mCaloriesEventListener = new BandCaloriesEventListener() {
        @Override
        public void onBandCaloriesChanged(final BandCaloriesEvent event) {
            if (event != null) {
                nowCalories=event.getCalories();
                if (beginCalories<1) {
                    beginCalories=nowCalories;
                }
                displayCal(nowCalories-beginCalories);
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
    };

    private HeartRateConsentListener mHeartRateConsentListener = new HeartRateConsentListener() {
        @Override
        public void userAccepted(boolean b) {
            // handle user's heart rate consent decision
            if (b == true) {
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
            String exceptionMessage="";
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
            // register HR sensor event listener
            client.getSensorManager().registerCaloriesEventListener(mCaloriesEventListener);
//Debug OK            appendToUI("should register mCal here");
        } catch (BandIOException ex) {
            appendToUI(ex.getMessage());
        } catch (BandException e) {
            String exceptionMessage="";
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

}
