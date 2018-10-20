package com.software.chavesjm.croquetswingtrainer;


import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.software.chavesjm.croquetswingtrainer.orientationProvider.OrientationProvider;
import com.software.chavesjm.croquetswingtrainer.representation.Quaternion;


class OrientationThread extends AsyncTask<Void,Float, Void>
{

    private  double m_lastTimeStamp = 0.0;
    private String TAG = "OrientationThread";

    private OnDataSendToActivity m_dataSendToActivity;
    private OrientationProvider m_orientationProvider;
    private Quaternion m_quaternion;
    private float m_eulerAngles[];

    OrientationThread(Activity activity,
                      OrientationProvider orientationProvider) {

        m_dataSendToActivity = (OnDataSendToActivity)activity;
        m_orientationProvider = orientationProvider;

        m_quaternion = new Quaternion();
        m_eulerAngles = new float[3];
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Log.d(TAG,"doInBackground - enter");

        while(!isCancelled())
        {
            double currentTimeStamp = System.currentTimeMillis();

            Log.d(TAG,"doInBackGround - " + (currentTimeStamp-m_lastTimeStamp));

            m_lastTimeStamp = currentTimeStamp;

            if(m_orientationProvider != null){
                m_orientationProvider.getQuaternion(m_quaternion);
                m_orientationProvider.getEulerAngles(m_eulerAngles);
            }

            publishProgress(m_eulerAngles[0], m_eulerAngles[1], m_eulerAngles[2]);

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        return null;

    }

    @Override
    protected void onProgressUpdate(Float... progress)
    {
        //m_dataSendToActivity.sendQuaternionData(m_quaternion);
        m_eulerAngles[0] = progress[0];
        m_eulerAngles[1] = progress[1];
        m_eulerAngles[2] = progress[2];

        m_dataSendToActivity.sendEulerAngles(m_eulerAngles);
    }

    @Override
    protected void onPreExecute() {

        Log.d(TAG,"onPreExecute - enter");

    }

    @Override
    protected void onCancelled() {

        Log.d(TAG,"onCancelled - enter");
    }
}
