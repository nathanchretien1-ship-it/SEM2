package com.ihealth.demo.business;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    private String URL_AUTH = "https://sem2grp2.istic.univ-rennes1.fr/api/auth.php";
    private String URL_MEASUREMENTS = "https://sem2grp2.istic.univ-rennes1.fr/api/measurements.php";
    private String apiToken = null;

    // UI Elements
    private LinearLayout loginLayout;
    private LinearLayout measurementLayout;
    private EditText editName;
    private EditText editEmail;
    private EditText editPassword;
    private Button buttonLogin;
    private Button buttonRegister;
    private Button buttonTest;
    TextView tvSpo2;
    TextView tvBpm;
    TextView tvTemperature;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginLayout = findViewById(R.id.login_layout);
        measurementLayout = findViewById(R.id.measurement_layout);
        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        buttonLogin = findViewById(R.id.button_login);
        buttonRegister = findViewById(R.id.button_register);
        buttonTest = findViewById(R.id.test_button);
        tvSpo2 = findViewById(R.id.tv_spo2);
        tvBpm = findViewById(R.id.tv_bpm);
        tvTemperature = findViewById(R.id.tv_temperature);

        checkPermission();

        buttonLogin.setOnClickListener(view -> {
            buttonLogin.setEnabled(false);
            Toast.makeText(this, "Connexion en cours...", Toast.LENGTH_SHORT).show();
            loginToApi(editEmail.getText().toString(), editPassword.getText().toString());
        });
        buttonRegister.setOnClickListener(view -> {
            buttonRegister.setEnabled(false);
            Toast.makeText(this, "Inscription en cours...", Toast.LENGTH_SHORT).show();
            registerToApi(editName.getText().toString(), editEmail.getText().toString(), editPassword.getText().toString());
        });
        buttonTest.setOnClickListener(view -> startAppLogic());
    }

    private String extractJson(String response) {
        if (response == null) return "{}";
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start != -1 && end != -1 && end >= start) {
            return response.substring(start, end + 1);
        }
        return "{}";
    }

    private void registerToApi(String name, String email, String password) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(URL_AUTH);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", "register");
                payload.put("name", name);
                payload.put("email", email);
                payload.put("password", password);

                Log.d("SANTE_APP_API", "Register request payload: " + payload.toString());

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                if (in != null) {
                    StringBuilder response = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        response.append(new String(buffer, 0, bytesRead));
                    }
                    in.close();

                    String jsonString = extractJson(response.toString());
                    Log.d("SANTE_APP_API", "Register response: " + jsonString);

                    JSONObject jsonResponse = new JSONObject(jsonString);
                    boolean success = jsonResponse.optBoolean("success", false);
                    String message = jsonResponse.optString("message", "Réponse inattendue");

                    runOnUiThread(() -> {
                        buttonRegister.setEnabled(true);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        if (success) {
                            // Si l'inscription réussit, on peut éventuellement vider le champ nom pour se connecter ensuite
                            editName.setText("");
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        buttonRegister.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Erreur serveur HTTP " + responseCode, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur d'inscription à l'API Auth", e);
                runOnUiThread(() -> {
                    buttonRegister.setEnabled(true);
                    String msg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                    Toast.makeText(MainActivity.this, "Erreur réseau: " + msg, Toast.LENGTH_LONG).show();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void loginToApi(String email, String password) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(URL_AUTH);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", "login");
                payload.put("email", email);
                payload.put("password", password);

                Log.d("SANTE_APP_API", "Login request payload: " + payload.toString());

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                if (in != null) {
                    StringBuilder response = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        response.append(new String(buffer, 0, bytesRead));
                    }
                    in.close();

                    String jsonString = extractJson(response.toString());
                    Log.d("SANTE_APP_API", "Login response: " + jsonString);

                    JSONObject jsonResponse = new JSONObject(jsonString);
                    boolean success = jsonResponse.optBoolean("success", false);
                    if (success) {
                        apiToken = jsonResponse.getString("token");
                        runOnUiThread(() -> {
                            buttonLogin.setEnabled(true);
                            Toast.makeText(MainActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();
                            loginLayout.setVisibility(View.GONE);
                            measurementLayout.setVisibility(View.VISIBLE);
                            buttonTest.setVisibility(View.VISIBLE);
                        });
                    } else {
                        String message = jsonResponse.optString("message", "Erreur de connexion");
                        runOnUiThread(() -> {
                            buttonLogin.setEnabled(true);
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        buttonLogin.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Erreur serveur HTTP " + responseCode, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur de connexion à l'API Auth", e);
                runOnUiThread(() -> {
                    buttonLogin.setEnabled(true);
                    String msg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                    Toast.makeText(MainActivity.this, "Erreur réseau: " + msg, Toast.LENGTH_LONG).show();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
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
                    MainActivity.this.envoyerAuServeur(deviceType, bpm, spo2, null);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (MainActivity.this.tvSpo2 != null) MainActivity.this.tvSpo2.setText("SpO2: " + spo2 + " %");
                            if (MainActivity.this.tvBpm != null) MainActivity.this.tvBpm.setText("BPM: " + bpm + " bpm");
                        }
                    });
                } else if (action.contains("result_nt13b")) {
                    double temp = json.getDouble("result");
                    MainActivity.this.envoyerAuServeur(deviceType, null, null, temp);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (MainActivity.this.tvTemperature != null) MainActivity.this.tvTemperature.setText("Température: " + temp + " °C");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur de traitement des données", e);
            }
        }
    }

    void envoyerAuServeur(String deviceType, Integer bpm, Integer spo2, Double temperature) {
        if (apiToken == null) {
            Log.e(TAG, "Pas de token, envoi impossible");
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(URL_MEASUREMENTS);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + apiToken);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("device_type", deviceType);
                if (bpm != null) payload.put("bpm", bpm);
                if (spo2 != null) payload.put("spo2", spo2);
                if (temperature != null) payload.put("temperature", temperature);

                Log.d("SANTE_APP_API", "Measurements request payload: " + payload.toString());

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                if (in != null) {
                    StringBuilder response = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        response.append(new String(buffer, 0, bytesRead));
                    }
                    in.close();

                    String jsonString = extractJson(response.toString());
                    Log.d("SANTE_APP_API", "Measurements response: " + jsonString);
                } else {
                    Log.d("SANTE_APP_API", "Measurements response code (no body): " + responseCode);
                }
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