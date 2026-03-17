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
import com.ihealth.communication.control.PoProfile;
import com.ihealth.communication.control.NT13BProfile;
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
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "SANTE_APP";
    private String URL_AUTH = "https://sem2grp2.istic.univ-rennes1.fr/api/auth.php";
    private String URL_MEASUREMENTS = "https://sem2grp2.istic.univ-rennes1.fr/api/measurements.php";
    private String URL_PATIENT = "https://sem2grp2.istic.univ-rennes1.fr/api/patient.php";
    private String apiToken = null;

    // UI Elements
    private View loginLayout;
    private View measurementLayout;
    private View historyLayout;
    private View devicesLayout;
    private View profileLayout;
    private LinearLayout devicesListContainer;

    private EditText editName;
    private EditText editEmail;
    private EditText editPassword;
    private Button buttonLogin;
    private Button buttonRegister;
    private ImageView buttonLogout;
    private ImageView buttonProfile;
    private Button buttonRefreshDevices;

    // Profile Elements
    private EditText editProfileName;
    private EditText editProfileFirstName;
    private android.widget.AutoCompleteTextView editProfileSexe;
    private EditText editProfileBirthDate;
    private EditText editProfileWeight;
    private EditText editProfileHeight;
    private Button buttonSaveProfile;
    private ImageView buttonBackProfile;

    // Registration Elements
    private View registrationFields;
    private EditText editRegFirstName;
    private android.widget.AutoCompleteTextView editRegSexe;
    private EditText editRegBirthDate;
    private EditText editRegWeight;
    private EditText editRegHeight;

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

    // Discovery Handler
    private Handler discoveryHandler = new Handler(Looper.getMainLooper());
    private Runnable discoveryRunnable = new Runnable() {
        @Override
        public void run() {
            if (apiToken != null) {
                if (!isScanningDevice) {
                    startAppLogic();
                } else {
                    Log.d(TAG, "Un scan est déjà en cours, on passe ce tour.");
                }
                // On planifie systématiquement le prochain essai dans 30 secondes
                discoveryHandler.postDelayed(this, 30000);
            }
        }
    };

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
        profileLayout = findViewById(R.id.profile_layout);
        devicesListContainer = findViewById(R.id.devices_list_container);

        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        buttonLogin = findViewById(R.id.button_login);
        buttonRegister = findViewById(R.id.button_register);
        buttonLogout = findViewById(R.id.btn_logout);
        buttonProfile = findViewById(R.id.btn_profile);
        buttonRefreshDevices = findViewById(R.id.button_refresh_devices);

        editProfileName = findViewById(R.id.edit_profile_name);
        editProfileFirstName = findViewById(R.id.edit_profile_firstname);
        editProfileSexe = findViewById(R.id.edit_profile_sexe);
        editProfileBirthDate = findViewById(R.id.edit_profile_birthdate);
        editProfileWeight = findViewById(R.id.edit_profile_weight);
        editProfileHeight = findViewById(R.id.edit_profile_height);
        buttonSaveProfile = findViewById(R.id.button_save_profile);
        buttonBackProfile = findViewById(R.id.button_back_profile);

        registrationFields = findViewById(R.id.registration_fields);
        editRegFirstName = findViewById(R.id.edit_reg_firstname);
        editRegSexe = findViewById(R.id.edit_reg_sexe);
        editRegBirthDate = findViewById(R.id.edit_reg_birthdate);
        editRegWeight = findViewById(R.id.edit_reg_weight);
        editRegHeight = findViewById(R.id.edit_reg_height);

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
        buttonProfile.setOnClickListener(v -> showProfile());

        buttonSaveProfile.setOnClickListener(v -> updateProfile());
        buttonBackProfile.setOnClickListener(v -> showMeasurements());

        // Setup DatePicker for profile birthdate
        editProfileBirthDate.setOnClickListener(v -> showDatePickerDialog(editProfileBirthDate));

        // Setup DatePicker for registration birthdate
        editRegBirthDate.setOnClickListener(v -> showDatePickerDialog(editRegBirthDate));

        // Setup Sexe Spinner for profile
        String[] sexes = new String[] {"Homme", "Femme", "Autre"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, sexes);
        editProfileSexe.setAdapter(adapter);

        // Setup Sexe Spinner for registration
        editRegSexe.setAdapter(adapter);

        if (sessionManager.isLoggedIn()) {
            apiToken = sessionManager.getToken();
            hideAllLayouts();
            measurementLayout.setVisibility(View.VISIBLE);
            buttonProfile.setVisibility(View.VISIBLE);
            findViewById(R.id.img_app_logo).setVisibility(View.VISIBLE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            buttonLogout.setVisibility(View.VISIBLE);
            startDiscoveryLoop();
        } else {
            hideAllLayouts();
            loginLayout.setVisibility(View.VISIBLE);
        }

        buttonLogin.setOnClickListener(view -> {
            hideKeyboard();
            buttonLogin.setEnabled(false);
            Toast.makeText(this, "Connexion en cours...", Toast.LENGTH_SHORT).show();
            loginToApi(editEmail.getText().toString(), editPassword.getText().toString());
        });
        buttonRegister.setOnClickListener(view -> {
            if (editName.getVisibility() == View.GONE) {
                editName.setVisibility(View.VISIBLE);
                registrationFields.setVisibility(View.VISIBLE);
                buttonLogin.setVisibility(View.GONE);
                buttonRegister.setText("Valider l'inscription");
            } else {
                String name = editName.getText().toString();
                String email = editEmail.getText().toString();
                String pwd = editPassword.getText().toString();
                String firstName = editRegFirstName.getText().toString();
                String sexe = editRegSexe.getText().toString();
                String birthDate = editRegBirthDate.getText().toString();
                String weightStr = editRegWeight.getText().toString();
                String heightStr = editRegHeight.getText().toString();

                if (name.isEmpty() || email.isEmpty() || pwd.isEmpty() || firstName.isEmpty() || sexe.isEmpty() || birthDate.isEmpty() || weightStr.isEmpty() || heightStr.isEmpty()) {
                    Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                    return;
                }

                hideKeyboard();
                buttonRegister.setEnabled(false);
                Toast.makeText(this, "Inscription en cours...", Toast.LENGTH_SHORT).show();
                registerToApi(name, firstName, email, pwd, sexe, birthDate, Float.parseFloat(weightStr), Float.parseFloat(heightStr));
            }
        });
        buttonRefreshDevices.setOnClickListener(view -> {
            Toast.makeText(this, "Actualisation de la connexion...", Toast.LENGTH_SHORT).show();
            startAppLogic();
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
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

    private void registerToApi(String name, String firstName, String email, String password, String sexe, String birthDate, float weight, float height) {
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
                payload.put("firstName", firstName);
                payload.put("email", email);
                payload.put("passwordUser", password);

                payload.put("sexe", sexe);
                payload.put("birthDate", birthDate);
                payload.put("weight", weight);
                payload.put("height", height);

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
                            startDiscoveryLoop();
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


    private void hideAllLayouts() {
        loginLayout.setVisibility(View.GONE);
        measurementLayout.setVisibility(View.GONE);
        historyLayout.setVisibility(View.GONE);
        devicesLayout.setVisibility(View.GONE);
        profileLayout.setVisibility(View.GONE);
        bottomNavigationView.setVisibility(View.GONE);
        buttonLogout.setVisibility(View.GONE);
        buttonProfile.setVisibility(View.GONE);
        findViewById(R.id.img_app_logo).setVisibility(View.GONE);
    }

    private void showDatePickerDialog(EditText dateEditText) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    String selectedDate = String.format(java.util.Locale.FRANCE, "%04d-%02d-%02d", year1, month1 + 1, dayOfMonth);
                    dateEditText.setText(selectedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void showProfile() {
        hideAllLayouts();
        profileLayout.setVisibility(View.VISIBLE);
        bottomNavigationView.setVisibility(View.VISIBLE);
        fetchProfile();
    }

    private void updateProfile() {
        String name = editProfileName.getText().toString();
        String firstName = editProfileFirstName.getText().toString();
        String sexe = editProfileSexe.getText().toString();
        String birthDate = editProfileBirthDate.getText().toString();
        String weightStr = editProfileWeight.getText().toString();
        String heightStr = editProfileHeight.getText().toString();

        if (name.isEmpty() || firstName.isEmpty() || sexe.isEmpty() || birthDate.isEmpty() || weightStr.isEmpty() || heightStr.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        hideKeyboard();
        buttonSaveProfile.setEnabled(false);
        Toast.makeText(this, "Mise à jour...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(URL_PATIENT);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + apiToken);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("nameUsers", name);
                payload.put("firstName", firstName);
                payload.put("sexe", sexe);
                payload.put("birthDate", birthDate);
                payload.put("weight", Float.parseFloat(weightStr));
                payload.put("height", Float.parseFloat(heightStr));

                Log.d("SANTE_APP_API", "Update Profile request payload: " + payload.toString());

                java.io.OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                java.io.InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                if (in == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                        buttonSaveProfile.setEnabled(true);
                    });
                    return;
                }

                java.util.Scanner scanner = new java.util.Scanner(in).useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";
                scanner.close();

                Log.d("SANTE_APP_API", "Update Profile Response Code: " + responseCode);
                Log.d("SANTE_APP_API", "Update Profile Response Body: " + responseBody);

                runOnUiThread(() -> {
                    buttonSaveProfile.setEnabled(true);
                    if (responseCode >= 200 && responseCode < 300) {
                        Toast.makeText(this, "Profil mis à jour", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SANTE_APP_API", "Exception: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                    buttonSaveProfile.setEnabled(true);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void fetchProfile() {
        if (apiToken == null) return;

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(URL_PATIENT);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + apiToken);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                java.io.InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                if (in == null) return;

                java.util.Scanner scanner = new java.util.Scanner(in).useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";
                scanner.close();

                Log.d("SANTE_APP_API", "Fetch Profile Response Code: " + responseCode);
                Log.d("SANTE_APP_API", "Fetch Profile Response Body: " + responseBody);

                String jsonBody = extractJson(responseBody);

                if (responseCode >= 200 && responseCode < 300 && !jsonBody.equals("{}")) {
                    JSONObject jsonObject = new JSONObject(jsonBody);
                    // Handle wrapped responses, typical API pattern
                    JSONObject data = jsonObject.has("data") ? jsonObject.getJSONObject("data") : jsonObject;

                    runOnUiThread(() -> {
                        try {
                            if (data.has("nameUsers")) editProfileName.setText(data.getString("nameUsers"));
                            else if (data.has("name")) editProfileName.setText(data.getString("name"));

                            if (data.has("firstName")) editProfileFirstName.setText(data.getString("firstName"));

                            if (data.has("sexe")) {
                                String sexe = data.getString("sexe");
                                editProfileSexe.setText(sexe, false); // false to not trigger dropdown on setText
                            }
                            if (data.has("birthDate")) editProfileBirthDate.setText(data.getString("birthDate"));
                            if (data.has("weight")) editProfileWeight.setText(String.valueOf(data.getDouble("weight")));
                            if (data.has("height")) editProfileHeight.setText(String.valueOf(data.getDouble("height")));
                        } catch (org.json.JSONException e) {
                            Log.e("SANTE_APP_API", "Erreur parsing JSON profile: " + e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SANTE_APP_API", "Exception fetch profile: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }


    private void logout() {
        try {
            for (String mac : deviceStates.keySet()) {
                if (deviceStates.get(mac).contains("PO3")) {
                    iHealthDevicesManager.getInstance().disconnectDevice(mac, iHealthDevicesManager.TYPE_PO3);
                } else if (deviceStates.get(mac).contains("NT13B")) {
                    iHealthDevicesManager.getInstance().disconnectDevice(mac, iHealthDevicesManager.TYPE_NT13B);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la deconnexion des devices", e);
        }

        sessionManager.clearSession();
        apiToken = null;
        stopDiscoveryLoop();
        Toast.makeText(this, "Déconnexion...", Toast.LENGTH_SHORT).show();
        hideAllLayouts();
        loginLayout.setVisibility(View.VISIBLE);

        // Reset inputs
        editEmail.setText("");
        editPassword.setText("");
        editName.setText("");
        editName.setVisibility(View.GONE);
        registrationFields.setVisibility(View.GONE);
        editRegSexe.setText("");
        editRegBirthDate.setText("");
        editRegWeight.setText("");
        editRegHeight.setText("");
        buttonLogin.setVisibility(View.VISIBLE);
        buttonRegister.setText("Créer un compte");
    }

    private void startDiscoveryLoop() {
        discoveryHandler.removeCallbacks(discoveryRunnable);
        discoveryHandler.post(discoveryRunnable);
    }

    private void stopDiscoveryLoop() {
        discoveryHandler.removeCallbacks(discoveryRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDiscoveryLoop();
    }

    private void showMeasurements() {
        hideAllLayouts();
        measurementLayout.setVisibility(View.VISIBLE);
        bottomNavigationView.setVisibility(View.VISIBLE);
        buttonLogout.setVisibility(View.VISIBLE);
        buttonProfile.setVisibility(View.VISIBLE);
        findViewById(R.id.img_app_logo).setVisibility(View.VISIBLE);
        startDiscoveryLoop();
    }

    private void showHistory() {
        hideAllLayouts();
        historyLayout.setVisibility(View.VISIBLE);
        bottomNavigationView.setVisibility(View.VISIBLE);
        buttonLogout.setVisibility(View.VISIBLE);
        buttonProfile.setVisibility(View.VISIBLE);
        findViewById(R.id.img_app_logo).setVisibility(View.VISIBLE);
        stopDiscoveryLoop();
        loadHistoryFromDatabase();
    }

    private void showDevices() {
        hideAllLayouts();
        devicesLayout.setVisibility(View.VISIBLE);
        bottomNavigationView.setVisibility(View.VISIBLE);
        buttonLogout.setVisibility(View.VISIBLE);
        buttonProfile.setVisibility(View.VISIBLE);
        findViewById(R.id.img_app_logo).setVisibility(View.VISIBLE);
        stopDiscoveryLoop();
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

    private boolean isSdkInitialized = false;
    private boolean isScanningDevice = false;
    private boolean isScanningPO3 = false;

    private void startAppLogic() {
        if (!isSdkInitialized) {
            isSdkInitialized = initSDK();
            if (isSdkInitialized) {
                iHealthDevicesManager.getInstance().registerClientCallback(new MyCallback());
            }
        }

        if (isSdkInitialized) {
            isScanningDevice = true;
            try {
                iHealthDevicesManager.getInstance().stopDiscovery();
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'arrêt du scan", e);
            }
            // Début du scan séquentiel : on commence par le PO3
            isScanningPO3 = true;
            iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.PO3);
            Log.d(TAG, "Scan silencieux en arrière-plan lancé pour PO3");
        }
    }

    private class MyCallback extends iHealthDevicesCallback {
        @Override
        public void onScanFinish() {
            super.onScanFinish();
            Log.d(TAG, "Fin du scan actuel.");
            if (isScanningPO3) {
                Log.d(TAG, "Scan PO3 terminé, lancement du scan NT13B.");
                isScanningPO3 = false;
                iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.NT13B);
            } else {
                Log.d(TAG, "Scan NT13B terminé. En attente du prochain cycle.");
                isScanningDevice = false;
            }
        }

        @Override
        public void onScanError(String mac, long errorId) {
            super.onScanError(mac, errorId);
            Log.e(TAG, "Erreur de scan sur l'appareil MAC: " + mac + ", erreurID: " + errorId);
        }

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
                if (action.equals(PoProfile.ACTION_RESULTDATA_PO) || action.equals(PoProfile.ACTION_LIVEDA_PO)) {
                    // iHealth SDK documentation mentions keys like bloodoxygen or spo2, and heartrate or bpm
                    int spo2 = 0;
                    if (json.has(PoProfile.BLOOD_OXYGEN_PO)) {
                        spo2 = json.getInt(PoProfile.BLOOD_OXYGEN_PO);
                    } else if (json.has("spo2")) {
                        spo2 = json.getInt("spo2");
                    }
                    int bpm = 0;
                    if (json.has(PoProfile.PULSE_RATE_PO)) {
                        bpm = json.getInt(PoProfile.PULSE_RATE_PO);
                    } else if (json.has("bpm")) {
                        bpm = json.getInt("bpm");
                    }

                    if (spo2 > 0 && bpm > 0) {
                        int finalSpo2 = spo2;
                        int finalBpm = bpm;
                        String sendDeviceType = deviceType.equals("PO3") ? "oxymetre" : deviceType;
                        MainActivity.this.envoyerAuServeur(sendDeviceType, finalBpm, finalSpo2, null);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (MainActivity.this.tvSpo2 != null) MainActivity.this.tvSpo2.setText(finalSpo2 + " %");
                                if (MainActivity.this.tvBpm != null) MainActivity.this.tvBpm.setText(finalBpm + " bpm");
                            }
                        });
                    }
                } else if (action.equals(NT13BProfile.ACTION_MEASUREMENT_RESULT)) {
                    double temp = 0;
                    if (json.has(NT13BProfile.RESULT)) {
                        temp = json.getDouble(NT13BProfile.RESULT);
                    } else if (json.has("temperature")) {
                        temp = json.getDouble("temperature");
                    }

                    if (temp > 0) {
                        // Arrondir à 1 chiffre après la virgule
                        double finalTemp = Math.round(temp * 10.0) / 10.0;
                        String sendDeviceType = deviceType.equals("NT13B") ? "thermometre" : deviceType;
                        MainActivity.this.envoyerAuServeur(sendDeviceType, null, null, finalTemp);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (MainActivity.this.tvTemperature != null) MainActivity.this.tvTemperature.setText(String.format(Locale.getDefault(), "%.1f", finalTemp) + " °C");
                            }
                        });
                    }
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
                payload.put("device_type", deviceType);
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