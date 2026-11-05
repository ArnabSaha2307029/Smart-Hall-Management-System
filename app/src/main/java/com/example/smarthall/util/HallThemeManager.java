package com.example.smarthall.util;

import android.content.Context;
import android.graphics.Color;

public class HallThemeManager {

    public static final int AMAR_EKUSHEY_PRIMARY = Color.parseColor("#1A237E");
    public static final int AMAR_EKUSHEY_PRIMARY_DARK = Color.parseColor("#172554");

    public static int getActivePrimaryColor(Context context) {
        if (context == null) {
            return AMAR_EKUSHEY_PRIMARY;
        }
        return ThemeHelper.getPrimaryColor(context);
    }

    public static int getActivePrimaryDarkColor(Context context) {
        if (context == null) {
            return AMAR_EKUSHEY_PRIMARY_DARK;
        }
        return ThemeHelper.getPrimaryDarkColor(context);
    }

    public static String getActiveTheme(Context context) {
        return ThemeHelper.getSelectedTheme(context);
    }

    public static void setActiveTheme(Context context, String themeKey) {
        ThemeHelper.setTheme(context, themeKey);
    }
}
