package com.software.chavesjm.croquetswingtrainer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.software.chavesjm.croquetswingtrainer.orientationProvider.AccelerometerCompassProvider;
import com.software.chavesjm.croquetswingtrainer.orientationProvider.CalibratedGyroscopeProvider;
import com.software.chavesjm.croquetswingtrainer.orientationProvider.GravityCompassProvider;
import com.software.chavesjm.croquetswingtrainer.orientationProvider.ImprovedOrientationSensor1Provider;
import com.software.chavesjm.croquetswingtrainer.orientationProvider.ImprovedOrientationSensor2Provider;
import com.software.chavesjm.croquetswingtrainer.orientationProvider.OrientationProvider;
import com.software.chavesjm.croquetswingtrainer.orientationProvider.RotationVectorProvider;
import com.software.chavesjm.croquetswingtrainer.representation.Quaternion;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements OnDataSendToActivity, AdapterView.OnItemSelectedListener {

    private static final String TAG = "MainActivity";

    private static final int MAX_ERRORLEVEL = 5;
    private static final int INIT_ERRORLEVEL = 3;

    private enum Provider{
        ACCELEROMETER_COMPASS, CALIBRATED_GYROSCOPE, GRAVITY_COMPASS, IMPROVED_ORIENTATION_SENSOR_1, IMPROVED_ORIENTATION_SENSOR_2,
        ROTATION_VECTOR
    }

    private TextView mInitialValue = null;
    private TextView mErrorMeasure = null;
    private SeekBar mSeekBar = null;
    private TextView mTextError = null;
    private ImageButton mPlayButton = null;

    private ToneGenerator mTone;

    private TCPCommunicator m_tcp_writer;

    private SensorManager m_sensorManager;
    private OrientationThread m_orientationThread;
    private OrientationProvider m_currentOrientationProvider;

    private Provider m_current_provider = Provider.IMPROVED_ORIENTATION_SENSOR_1;

    private TextView m_textViewW1 = null;
    private TextView m_textViewX1 = null;
    private TextView m_textViewY1 = null;
    private TextView m_textViewZ1 = null;

    private TextView m_textViewRoll = null;
    private TextView m_textViewPitch = null;
    private TextView m_textViewYaw = null;

    private ProgressBar mErrorLeft;
    private ProgressBar mErrorRigth;

    private TextView m_textViewIP = null;

    private Spinner m_spinner = null;

    private boolean m_isPlaying = false;
    private boolean m_getInitialValue = false;

    private Handler handler;
    private ToneGenerator mLaserTone = null;
    private AudioManager mAudioManager = null;

    private int m_current_level_error = 0;

    private double m_yaw_degree = 0;
    private double m_pitch_degree = 0;
    private double m_roll_degree = 0;


    @Override
    public void onResume(){
        super.onResume();

    }

    @Override
    public void onPause() {

        super.onPause();

//        m_orientationThread.cancel(true);
//
//        try {
//            m_tcp_writer.closeConnections();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mInitialValue = findViewById(R.id.initialPosition);
        mErrorMeasure = findViewById(R.id.errorMeasure);

        mSeekBar = findViewById(R.id.difficult);
        mPlayButton = findViewById(R.id.playButton);

        m_textViewW1 = findViewById(R.id.textViewW1);
        m_textViewX1 = findViewById(R.id.textViewX1);
        m_textViewY1 = findViewById(R.id.textViewY1);
        m_textViewZ1 = findViewById(R.id.textViewZ1);

        m_textViewRoll = findViewById(R.id.textViewRoll);
        m_textViewPitch = findViewById(R.id.textViewPitch);
        m_textViewYaw = findViewById(R.id.textViewYaw);

        m_textViewIP = findViewById(R.id.textViewIP);

        mTextError = findViewById(R.id.mTextError);

        mErrorLeft = findViewById(R.id.errorLeft);
        mErrorRigth = findViewById(R.id.errorRigth);

        handler = new Handler();

        m_spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.provider_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_spinner.setAdapter(adapter);
        m_spinner.setOnItemSelectedListener(this);

        mSeekBar.setMax(MAX_ERRORLEVEL * 10);
        mSeekBar.incrementProgressBy(1);
        mSeekBar.setProgress(INIT_ERRORLEVEL * 10);

        updateErrorLevel(INIT_ERRORLEVEL * 10);


        mTone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        mLaserTone = new ToneGenerator(0,ToneGenerator.MAX_VOLUME);
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        m_textViewIP.setText(ip);

        m_sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(m_sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).size() <= 0) {

            Log.e(TAG,"Gyroscope sensor not exist");

            finish();
            System.exit(0);

        }

        new Thread(m_tcp_writer = new TCPCommunicator(36001)).start();

        m_currentOrientationProvider = new ImprovedOrientationSensor1Provider(m_sensorManager);
        m_currentOrientationProvider.start();

        mPlayButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        if(!m_isPlaying)
                        {
                            mPlayButton.setImageResource(R.drawable.shotting);
                            // Use a new tread as this can take a while
                            final Thread thread = new Thread(new Runnable() {
                                public void run() {
                                    handler.post(new Runnable() {

                                        public void run() {
                                            playSound();
                                        }
                                    });
                                }
                            });
                            thread.start();

                        }
                        break;
                    case MotionEvent.ACTION_UP:

                        mLaserTone.stopTone();

                        if(!m_isPlaying) {

                            m_orientationThread = new OrientationThread(MainActivity.this,m_currentOrientationProvider);
                            m_orientationThread.execute((Void[]) null);

                            mPlayButton.setImageResource(R.drawable.stop);

                            m_isPlaying = true;
                            m_getInitialValue = true;
                        }
                        else{
                            m_orientationThread.cancel(true);
                            mPlayButton.setImageResource(R.drawable.start);
                            m_isPlaying = false;

                            mErrorLeft.setProgress(0);
                            mErrorRigth.setProgress(0);
                            mErrorMeasure.setText("0º");
                            mInitialValue.setText("0º");
                            mTone.stopTone();
                        }

                        m_spinner.setEnabled(!m_isPlaying);
                    default:
                        break;
                }
                return true;
            }
        });

        mPlayButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if(!m_isPlaying) {

                    m_orientationThread = new OrientationThread(MainActivity.this,m_currentOrientationProvider);
                    m_orientationThread.execute((Void[]) null);

                    mPlayButton.setImageResource(R.drawable.stop);

                    m_isPlaying = true;
                }
                else{
                    m_orientationThread.cancel(true);
                    mPlayButton.setImageResource(R.drawable.start);
                    m_isPlaying = false;
                }

                m_spinner.setEnabled(!m_isPlaying);
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue,
                                          boolean fromUser) {

                updateErrorLevel(progresValue);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


            }

        });

    }

    private void updateErrorLevel(int i) {

        m_current_level_error = i;

        double currentLevelError = i/10.0;

        mTextError.setText("Error Level: " + String.valueOf(currentLevelError) + " º");

    }


    @Override
    public void sendQuaternionData(Quaternion quaternion) {

        final String W1 = String.valueOf(quaternion.getW());
        final String X1 = String.valueOf(quaternion.getX());
        final String Y1 = String.valueOf(quaternion.getY());
        final String Z1 = String.valueOf(quaternion.getZ());

        m_textViewW1.setText(W1);
        m_textViewX1.setText(X1);
        m_textViewY1.setText(Y1);
        m_textViewZ1.setText(Z1);

        if(m_tcp_writer.isConnected()) {

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    m_tcp_writer.writeToSocket(W1 + "," + X1 + "," + Y1 + "," + Z1);
                }
            });
            thread.start();
        }

    }

    @Override
    public void sendEulerAngles(float[] eulerAngles) {

        double yaw_degree = Math.toDegrees(eulerAngles[0]);
        double pitch_degree = Math.toDegrees(eulerAngles[1]);
        double roll_degree = Math.toDegrees(eulerAngles[2]);

        if(m_getInitialValue){

            m_yaw_degree = yaw_degree;
            m_pitch_degree = pitch_degree;
            m_roll_degree = roll_degree;

            String s = String.format("%.2f", yaw_degree);

            mInitialValue.setText(s + "º");

            m_getInitialValue = false;
        }

        checkError(yaw_degree, pitch_degree, roll_degree);


        final String yaw = String.valueOf(yaw_degree);
        final String pitch = String.valueOf(pitch_degree);
        final String roll = String.valueOf(roll_degree);

        m_textViewYaw.setText(yaw);
        m_textViewPitch.setText(pitch);
        m_textViewRoll.setText(roll);

        if(m_tcp_writer.isConnected()) {

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    m_tcp_writer.writeToSocket(yaw + "," + pitch + "," + roll);
                }
            });
            thread.start();
        }
    }

    private void checkError(double yaw_degree, double pitch_degree, double roll_degree) {

        double currentError = m_yaw_degree - yaw_degree;

        //String s = String.format("%.2f", currentError);
        String s = String.format("%.2f", yaw_degree);

        mErrorMeasure.setText(s);


        double referenceVumenter = (m_current_level_error / 10.0)/10.0;
        //Error hacia la izquierda
        if((currentError) > 0){
            mErrorRigth.setProgress(0);
            mErrorLeft.setProgress((int)(Math.abs(currentError)/referenceVumenter));

        }
        else //Error hacia la derecha
        {
            mErrorLeft.setProgress(0);
            mErrorRigth.setProgress((int)(Math.abs(currentError)/referenceVumenter));
        }

        if(currentError > (m_current_level_error / 10.0))
        {
            mTone.startTone(ToneGenerator.TONE_PROP_BEEP);
        }
        else if(currentError < -1*(m_current_level_error / 10.0))
        {
            mTone.startTone(ToneGenerator.TONE_DTMF_0);
        }
        else
        {
            mTone.stopTone();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        m_currentOrientationProvider.stop();

        switch (i) {
            case 0:
                m_currentOrientationProvider = new ImprovedOrientationSensor1Provider(m_sensorManager); break;
            case 1:
                m_currentOrientationProvider = new ImprovedOrientationSensor2Provider(m_sensorManager); break;
            case 2:
                m_currentOrientationProvider = new RotationVectorProvider(m_sensorManager); break;
            case 3:
                m_currentOrientationProvider = new CalibratedGyroscopeProvider(m_sensorManager); break;
            case 4:
                m_currentOrientationProvider = new GravityCompassProvider(m_sensorManager); break;
            case 5:
                m_currentOrientationProvider = new AccelerometerCompassProvider(m_sensorManager); break;
        }

        m_currentOrientationProvider.start();

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    void playSound(){
        mLaserTone.startTone(ToneGenerator.TONE_DTMF_0);
    }



}
