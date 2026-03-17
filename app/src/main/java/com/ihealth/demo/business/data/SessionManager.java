package com.ihealth.demo.business.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "SanteAppSession";
    private static final String KEY_TOKEN = "api_token";
    private static final String KEY_EMAIL = "user_email";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveSession(String token, String email) {
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, null);
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void saveIdDoctor(int idDoctor) {
        editor.putInt("idDoctor", idDoctor);
        editor.apply();
    }

    public int getIdDoctor() {
        return pref.getInt("idDoctor", -1); // -1 if not found
    }
}