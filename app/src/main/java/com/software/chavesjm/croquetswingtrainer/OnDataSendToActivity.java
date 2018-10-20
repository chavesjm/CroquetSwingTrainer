package com.software.chavesjm.croquetswingtrainer;

import com.software.chavesjm.croquetswingtrainer.representation.Quaternion;

public interface OnDataSendToActivity {

    void sendQuaternionData(Quaternion m_quaternion);

    void sendEulerAngles(float[] m_eulerAngles);
}
