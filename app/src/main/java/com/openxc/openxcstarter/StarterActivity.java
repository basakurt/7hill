package com.openxc.openxcstarter;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;
import android.telephony.SmsManager ;
import android.widget.Toast;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import com.github.capur16.digitspeedviewlib.DigitSpeedView;


import com.openxcplatform.openxcstarter.R;
import com.openxc.VehicleManager;
import com.openxc.measurements.BrakePedalStatus;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.EngineSpeed;
import com.openxc.measurements.VehicleSpeed ;
import com.openxc.measurements.SteeringWheelAngle ;


import static com.google.common.collect.ComparisonChain.start;
import static java.lang.Math.sqrt;

public class StarterActivity extends Activity {

    DigitSpeedView digitSpeedView, digitSpeedView2 ;
    private static final String TAG = "StarterActivity";
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Context context;
    TextView txtLat;
    String lat;
    Location location;
    String provider;
    double latitude, longitude;
    protected boolean gps_enabled, network_enabled;

    private VehicleManager mVehicleManager;
    private TextView mEngineSpeedView;
    private TextView mVehicleSpeedView;
    private TextView mSteeringWheelAngleView;
    private ImageView slow;
    private TextView mBrakePedalPercentageView;

    VehicleSpeed speed;
    SteeringWheelAngle angle;
    BrakePedalStatus brakePedalStatus;
    boolean stopMessage = false;

    double angleValue, speedValue;
    double speedValueOldOld, speedValueOld;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_starter);

        final int messagePermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        txtLat = (TextView) findViewById(R.id.loc);

        digitSpeedView = (DigitSpeedView) findViewById(R.id.vehicle_speed);
        digitSpeedView2 = (DigitSpeedView) findViewById(R.id.wheelangle);


     //   mVehicleSpeedView = (TextView) findViewById(R.id.vehicle_speed);
      //  mSteeringWheelAngleView = (TextView) findViewById(R.id.wheelangle);
        slow = (ImageView) findViewById(R.id.slow);


        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 10);

            } else {
                Log.d("b", "b");
            }
        }

    else {

            try {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        while (true) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    double i = speedValue;
                                    double y = angleValue;

                                    boolean check = checkTurn(i, y);
                                    if (!check) {
                                        slow.setVisibility(slow.VISIBLE);
                                    } else {
                                        slow.setVisibility(slow.GONE);
                                    }

                                    boolean accident = checktheAccident(speedValue, speedValueOldOld);
                                    if ((accident)&&(stopMessage==false)) {
                                        stopMessage = true ;
                                       // Log.d("ACCIDENTTTTTT", "!!!!!!");
                                        sendSMS("+905546299982", "Accident Occured Here: https://www.google.com.tr/maps/@40.9755881,29.2338394,16.65z?hl=tr!") ;


                                    }
                                }
                            });


                            try {
                                Thread.sleep(250);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            } catch (Exception e) {

            }
        }



    }

    @Override
    public void onPause() {
        super.onPause();
        // When the activity goes into the background or exits, we want to make
        // sure to unbind from the service to avoid leaking memorSy
        if (mVehicleManager != null) {
            Log.i(TAG, "Unbinding from Vehicle Manager");
            // Remember to remove your listeners, in typical Android
            // fashion.
            mVehicleManager.removeListener(VehicleSpeed.class,
                    mSpeedListener);
            mVehicleManager.removeListener(SteeringWheelAngle.class, mSteeringWheelAngleListener);
            unbindService(mConnection);
            //   mVehicleManager.removeListener(BrakePedalStatus.class,mBrakePedalStatusListener);
            mVehicleManager = null;
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        // When the activity starts up or returns from the background,
        // re-connect to the VehicleManager so we can receive updates.
        if (mVehicleManager == null) {
            Intent intent = new Intent(this, VehicleManager.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /* This is an OpenXC measurement listener object - the type is recognized
     * by the VehicleManager as something that can receive measurement updates.
     * Later in the file, we'll ask the VehicleManager to call the receive()
     * function here whenever a new EngineSpeed value arrives.
     */
    VehicleSpeed.Listener mSpeedListener = new VehicleSpeed.Listener() {
        @Override
        public void receive(Measurement measurement) {
            // When we receive a new EngineSpeed value from the car, we want to
            // update the UI to display the new value. First we cast the generic
            // Measurement back to the type we know it to be, an EngineSpeed.
            final VehicleSpeed speed = (VehicleSpeed) measurement;
            // In order to modify the UI, we have to make sure the code is
            // running on the "UI thread" - Google around for this, it's an
            // important concept in Android.
            StarterActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    // Finally, we've got a new value and we're running on the
                    // UI thread - we set the text of the EngineSpeed view to
                    // the latest value
                 //   mVehicleSpeedView.setText("Vehicle speed (KH/H): "
                           // + speed.getValue().doubleValue());
                    int i = (int) (speed.getValue().doubleValue());
                    digitSpeedView.updateSpeed(i);
                    speedValueOldOld = speedValueOld;
                    speedValueOld = speedValue;
                    speedValue = speed.getValue().doubleValue();

                  // Log.d("oldold::", Double.toString(speedValueOldOld));
                  //  Log.d("old::", Double.toString(speedValueOld));
                   // Log.d("current::", Double.toString(speedValue));

                }
            });
        }
    };

    SteeringWheelAngle.Listener mSteeringWheelAngleListener = new SteeringWheelAngle.Listener() {

        @Override
        public void receive(Measurement measurement) {

            final SteeringWheelAngle angle = (SteeringWheelAngle) measurement;

            StarterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                 //   mSteeringWheelAngleView.setText("Steering Wheel angle:" + angle.getValue().doubleValue());
                    int m= (int) (angle.getValue().doubleValue());
                    digitSpeedView2.updateSpeed(m);
                    angleValue = angle.getValue().doubleValue();
                }
            });

        }
    };

   /* BrakePedalStatus.Listener mBrakePedalStatusListener = new BrakePedalStatus.Listener(){

        public void receive(Measurement measurement){

            final BrakePedalStatus brakePedalStatus = (BrakePedalStatus) measurement ;

                    StarterActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBrakePedalPercentageView.setText("Brake Pedal Status :" + brakePedalStatus.getValue().booleanValue());
                        }
                    });
        }
    };*/

    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the VehicleManager service is
        // established, i.e. bound.
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");
            // When the VehicleManager starts up, we store a reference to it
            // here in "mVehicleManager" so we can call functions on it
            // elsewhere in our code.
            mVehicleManager = ((VehicleManager.VehicleBinder) service).getService();


            // We want to receive updates whenever the EngineSpeed changes. We
            // have an EngineSpeed.Listener (see above, mSpeedListener) and here
            // we request that the VehicleManager call its receive() method
            // whenever the EngineSpeed changes

            mVehicleManager.addListener(VehicleSpeed.class, mSpeedListener);

            mVehicleManager.addListener(SteeringWheelAngle.class, mSteeringWheelAngleListener);

        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleManager Service  disconnected unexpectedly");
            mVehicleManager = null;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.starter, menu);
        return true;
    }

    public boolean checkTurn(double speed, double angle) {

        double minRadius = 11.9;

        if (angle < 0) {
            angle = -angle;
        }
        double wheelAngle = angle / 18;

        //  double radius=(minRadius*33.33*33.33)/(wheelAngle*wheelAngle); // quadratic

        double radius = (minRadius * 33.33 * 33.33) / (wheelAngle * wheelAngle); // linear
        double frictionCoeeficient = 0.82;
        double g = 9.81;
        double criticSpeed = sqrt(frictionCoeeficient * g * radius);
        if (speed > criticSpeed * 0.9) {
            return false;
        }

        return true;
    }

    public boolean checktheAccident(double speedValue, double speedValueOldOld) {
        if ((speedValueOldOld - speedValue) > 0.50) {
            return true;
        }

        return false;
    }


   /* public void sendMessage() {
        String telephoneNumber = "5546299982";
        String message = "ACCIDENT HERE!";
        SmsManager manager = SmsManager.getDefault();
        //  manager.sendMultimediaMessage();
        manager.sendTextMessage(telephoneNumber, null, message, null, null);

    }*/


        private void sendSMS(String phoneNumber, String message)
        {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, message, null, null);
        }


  
}




