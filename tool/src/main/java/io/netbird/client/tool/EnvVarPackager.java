package io.netbird.client.tool;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import io.netbird.gomobile.android.Android;
import io.netbird.gomobile.android.EnvList;

public class EnvVarPackager {
    private static final String LOGTAG = "EnvVarPackager";

    // Name of the optional file with NB_* environment overrides for testing, in
    // the app-specific external files directory, where adb can write it without
    // root: /sdcard/Android/data/<application id>/files/nb_env_overrides
    public static final String OVERRIDES_FILE_NAME = "nb_env_overrides";

    public static EnvList getEnvironmentVariables(Preferences preferences) {
        return getEnvironmentVariables(preferences, null);
    }

    public static EnvList getEnvironmentVariables(Preferences preferences, @Nullable File overridesFile) {
        var envList = new EnvList();

        envList.put(Android.getEnvKeyNBForceRelay(), String.valueOf(preferences.isConnectionForceRelayed()));

        if (overridesFile != null) {
            putOverrides(envList, overridesFile);
        }

        return envList;
    }

    // putOverrides adds the KEY=VALUE lines of the overrides file whose key starts
    // with NB_. Blank lines and lines starting with # are skipped. The engine
    // exports the variables to the process environment, so removing a line only
    // takes effect after the app process restarts.
    private static void putOverrides(EnvList envList, File overridesFile) {
        if (!overridesFile.isFile()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(overridesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int separator = line.indexOf('=');
                if (separator <= 0) {
                    Log.w(LOGTAG, "ignoring malformed override line: " + line);
                    continue;
                }

                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if (!key.startsWith("NB_")) {
                    Log.w(LOGTAG, "ignoring override of non NB_ variable: " + key);
                    continue;
                }

                Log.i(LOGTAG, "environment override from " + overridesFile + ": " + key + "=" + value);
                envList.put(key, value);
            }
        } catch (IOException e) {
            Log.e(LOGTAG, "failed to read environment overrides from " + overridesFile, e);
        }
    }
}
