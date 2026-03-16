package com.ihealth.demo.business;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ihealth.communication.manager.DiscoveryTypeEnum;
import com.ihealth.demo.business.data.AppDatabase;
import com.ihealth.demo.business.data.MeasurementDao;
import com.ihealth.demo.business.data.MeasurementEntity;
import com.ihealth.demo.business.data.SessionManager;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "SANTE_APP";
    private String URL_AUTH = "https://sem2grp2.istic.univ-rennes1.fr/api/auth.php";
    private String URL_MEASUREMENTS = "https://sem2grp2.istic.univ-rennes1.fr/api/measurements.php";
    private String apiToken = null;

    // UI Elements
    private View loginLayout;
    private View measurementLayout;
    private View historyLayout;
    private View devicesLayout;
    private LinearLayout devicesListContainer;

    private EditText editName;
    private EditText editEmail;
    private EditText editPassword;
    private Button buttonLogin;
    private Button buttonRegister;
    private Button buttonTest;
    private ImageView buttonLogout;

    private BottomNavigationView bottomNavigationView;

    // History RecyclerView
    private RecyclerView recyclerHistory;
    private HistoryAdapter historyAdapter;

    // Local Data
    private SessionManager sessionManager;
    private MeasurementDao measurementDao;

    // Executor for Database operations
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    TextView tvSpo2;
    TextView tvBpm;
    TextView tvTemperature;

    // Device Tracking
    private Map<String, String> deviceStates = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init Local Data
        sessionManager = new SessionManager(this);
        measurementDao = AppDatabase.getDatabase(this).measurementDao();

        loginLayout = findViewById(R.id.login_layout);
        measurementLayout = findViewById(R.id.measurement_layout);
        historyLayout = findViewById(R.id.history_layout);
        devicesLayout = findViewById(R.id.devices_layout);
        devicesListContainer = findViewById(R.id.devices_list_container);

        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        buttonLogin = findViewById(R.id.button_login);
        buttonRegister = findViewById(R.id.button_register);
        buttonTest = findViewById(R.id.test_button);
        buttonLogout = findViewById(R.id.btn_logout);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        recyclerHistory = findViewById(R.id.recycler_history);
        historyAdapter = new HistoryAdapter();
        recyclerHistory.setAdapter(historyAdapter);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));

        tvSpo2 = findViewById(R.id.tv_spo2);
        tvBpm = findViewById(R.id.tv_bpm);
        tvTemperature = findViewById(R.id.tv_temperature);

        checkPermission();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_measurements) {
                showMeasurements();
                return true;
            } else if (id == R.id.nav_history) {
                showHistory();
                return true;
            } else if (id == R.id.nav_devices) {
                showDevices();
                return true;
            }
            return false;
        });

        buttonLogout.setOnClickListener(v -> logout());

        if (sessionManager.isLoggedIn()) {
            apiToken = sessionManager.getToken();
            loginLayout.setVisibility(View.GONE);
            measurementLayout.setVisibility(View.VISIBLE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            buttonLogout.setVisibility(View.VISIBLE);
        }

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
                payload.put("nameUsers", name);
                payload.put("email", email);
                payload.put("passwordUser", password);

                // Champs requis avec des valeurs par défaut pour correspondre à la BDD
                payload.put("sexe", "Other");
                payload.put("birthDate", "2000-01-01");
                payload.put("weight", 70.0);
                payload.put("height", 170.0);

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
                payload.put("passwordUser", password);

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
                        sessionManager.saveSession(apiToken, email);
                        runOnUiThread(() -> {
                            buttonLogin.setEnabled(true);
                            Toast.makeText(MainActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();
                            loginLayout.setVisibility(View.GONE);
                            measurementLayout.setVisibility(View.VISIBLE);
                            bottomNavigationView.setVisibility(View.VISIBLE);
                            buttonLogout.setVisibility(View.VISIBLE);
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

    private void logout() {
        sessionManager.clearSession();
        apiToken = null;
        loginLayout.setVisibility(View.VISIBLE);
        measurementLayout.setVisibility(View.GONE);
        historyLayout.setVisibility(View.GONE);
        devicesLayout.setVisibility(View.GONE);
        bottomNavigationView.setVisibility(View.GONE);
        buttonLogout.setVisibility(View.GONE);

        // Reset inputs
        editEmail.setText("");
        editPassword.setText("");
        editName.setText("");
    }

    private void showMeasurements() {
        measurementLayout.setVisibility(View.VISIBLE);
        historyLayout.setVisibility(View.GONE);
        devicesLayout.setVisibility(View.GONE);
    }

    private void showHistory() {
        measurementLayout.setVisibility(View.GONE);
        historyLayout.setVisibility(View.VISIBLE);
        devicesLayout.setVisibility(View.GONE);
        loadHistoryFromDatabase();
    }

    private void showDevices() {
        measurementLayout.setVisibility(View.GONE);
        historyLayout.setVisibility(View.GONE);
        devicesLayout.setVisibility(View.VISIBLE);
        refreshDevicesList();
    }

    private void loadHistoryFromDatabase() {
        databaseExecutor.execute(() -> {
            List<MeasurementEntity> measurements = measurementDao.getAllMeasurements();
            runOnUiThread(() -> historyAdapter.setMeasurements(measurements));
        });
    }

    private void updateDeviceState(String mac, String name, String state) {
        runOnUiThread(() -> {
            deviceStates.put(mac, name + " - " + state);
            refreshDevicesList();
        });
    }

    private void refreshDevicesList() {
        if (devicesListContainer == null) return;
        devicesListContainer.removeAllViews();

        if (deviceStates.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("Aucun appareil trouvé.");
            emptyView.setTextSize(16);
            devicesListContainer.addView(emptyView);
            return;
        }

        for (Map.Entry<String, String> entry : deviceStates.entrySet()) {
            TextView deviceView = new TextView(this);
            deviceView.setText(entry.getValue());
            deviceView.setTextSize(18);
            deviceView.setPadding(0, 8, 0, 8);
            devicesListContainer.addView(deviceView);
        }
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
            updateDeviceState(mac, deviceType, "Trouvé");
            iHealthDevicesManager.getInstance().connectDevice("", mac, deviceType);
        }

        @Override
        public void onDeviceConnectionStateChange(String mac, String deviceType, int status, int errorID) {
            if (status == iHealthDevicesManager.DEVICE_STATE_CONNECTED) {
                Log.i(TAG, "Connecté avec succès à : " + deviceType);
                updateDeviceState(mac, deviceType, "Connecté");
                if (deviceType.equals("PO3")) {
                    iHealthDevicesManager.getInstance().getPo3Control(mac).startMeasure();
                } else if (deviceType.equals("NT13B")) {
                    iHealthDevicesManager.getInstance().getNT13BControl(mac).getMeasurement();
                }
            } else if (status == iHealthDevicesManager.DEVICE_STATE_DISCONNECTED) {
                Log.i(TAG, "Déconnecté de : " + deviceType);
                updateDeviceState(mac, deviceType, "Déconnecté");
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
        long timestamp = System.currentTimeMillis();
        // Save to local database
        databaseExecutor.execute(() -> {
            MeasurementEntity entity = new MeasurementEntity(deviceType, bpm, spo2, temperature, timestamp);
            measurementDao.insert(entity);
            // Delete records older than 7 days (7 * 24 * 60 * 60 * 1000 ms = 604800000 ms)
            measurementDao.deleteOlderThan(timestamp - 604800000L);
        });

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
                payload.put("deviceType", deviceType);
                if (bpm != null) payload.put("bpm", bpm);
                if (spo2 != null) payload.put("spo2", spo2);
                if (temperature != null) payload.put("temperature", temperature);

                // Add measureDate and measureTime based on the current device time
                Date now = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                payload.put("measureDate", dateFormat.format(now));
                payload.put("measureTime", timeFormat.format(now));

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