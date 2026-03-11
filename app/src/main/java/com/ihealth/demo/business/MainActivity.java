package com.ihealth.demo.business;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ihealth.communication.manager.DiscoveryTypeEnum;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.demo.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.json.JSONObject;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "SANTE_APP";
    // Remplace par l'adresse HTTPS de ton serveur
    private String URL_SERVEUR = "https://192.168.1.XX/api/data";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
        findViewById(R.id.test_button).setOnClickListener(view -> startAppLogic());
    }

    private void startAppLogic() {
        if (initSDK()) {
            iHealthDevicesManager.getInstance().registerClientCallback(new MyCallback());
            // Recherche active pour Oxymètre (PO3) et Thermomètre (NT13B)
            iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.PO3);
            iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.NT13B);
            Toast.makeText(this, "Scan en cours...", Toast.LENGTH_SHORT).show();
        }
    }

    private class MyCallback extends iHealthDevicesCallback {
        @Override
        public void onScanDevice(String mac, String deviceType, int rssi) {
            Log.d(TAG, "Dispositif trouvé : " + deviceType + " | MAC: " + mac);
            iHealthDevicesManager.getInstance().connectDevice("", mac, deviceType);
        }

        @Override
        public void onDeviceConnectionStateChange(String mac, String deviceType, int status, int errorID) {
            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTED) {
                Log.i(TAG, "Connecté avec succès à : " + deviceType);
                if (deviceType.equals("PO3")) {
                    iHealthDevicesManager.getInstance().getPo3Control(mac).startMeasure();
                } else if (deviceType.equals("NT13B")) {
                    iHealthDevicesManager.getInstance().getNT13BControl(mac).getMeasurement();
                }
            }
        }

        @Override
        public void onDeviceNotify(String mac, String deviceType, String action, String message) {
            try {
                JSONObject json = new JSONObject(message);
                if (action.contains("result_po3")) {
                    int spo2 = json.getInt("spo2");
                    int bpm = json.getInt("heartrate");
                    envoyerAuServeur("SpO2", spo2, mac);
                } else if (action.contains("result_nt13b")) {
                    double temp = json.getDouble("result");
                    envoyerAuServeur("Temperature", (int)temp, mac);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur de traitement des données", e);
            }
        }
    }

    private void envoyerAuServeur(String typeMesure, int valeur, String deviceId) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(URL_SERVEUR);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setConnectTimeout(10000);
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("patient_id", "PATIENT_DEMO_01");
                payload.put("type", typeMesure);
                payload.put("valeur", valeur);
                payload.put("device_mac", deviceId);
                payload.put("timestamp", System.currentTimeMillis());

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(payload.toString());
                os.flush();
                os.close();

                Log.d(TAG, "Réponse Serveur (HTTPS) : " + conn.getResponseCode());
            } catch (Exception e) {
                Log.e(TAG, "Échec de l'envoi HTTPS : " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private boolean initSDK() {
        iHealthDevicesManager.getInstance().init(getApplication(), Log.VERBOSE, Log.VERBOSE);
        try {
            InputStream is = getAssets().open("com_demo_sdk_android.pem");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return iHealthDevicesManager.getInstance().sdkAuthWithLicense(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Erreur Licence SDK", e);
            return false;
        }
    }

    private void checkPermission() {
        new RxPermissions(this).request(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        ).subscribe(granted -> {
            if (!granted) Toast.makeText(this, "Permissions nécessaires manquantes", Toast.LENGTH_LONG).show();
        });
    }
}