package com.example.smarthall.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import com.example.smarthall.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ThemeHelper {

    public static final String THEME_CRIMSON = "crimson";
    public static final String THEME_NAVY = "slate_navy";
    public static final String THEME_FOREST = "forest_sage";
    public static final String THEME_INDIGO = "modern_indigo";
    public static final String THEME_CHARCOAL = "charcoal_white";

    private static final String PREFS_NAME = "smart_hall_prefs";
    private static final String KEY_APP_THEME = "app_theme";

    public static void applyTheme(Activity activity) {
        if (activity == null) return;
        String themeKey = getSelectedTheme(activity);
        activity.setTheme(getThemeResId(themeKey));
    }

    public static void setTheme(Context context, String themeKey) {
        if (context == null || themeKey == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_APP_THEME, themeKey).apply();
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                SharedPreferences studentPrefs = context.getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
                studentPrefs.edit().putString(KEY_APP_THEME, themeKey).apply();
            }
        } catch (Exception ignored) {
        }
    }

    public static String getSelectedTheme(Context context) {
        if (context == null) return THEME_CRIMSON;
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                SharedPreferences studentPrefs = context.getSharedPreferences("student_prefs_" + user.getUid(), Context.MODE_PRIVATE);
                if (studentPrefs.contains(KEY_APP_THEME)) {
                    return studentPrefs.getString(KEY_APP_THEME, THEME_CRIMSON);
                }
            }
        } catch (Exception ignored) {
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_APP_THEME, THEME_CRIMSON);
    }

    public static int getThemeResId(String themeKey) {
        if (themeKey == null) return R.style.Theme_SmartHall_Crimson;
        switch (themeKey) {
            case THEME_NAVY:
            case "navy":
            case "amar_ekushey":
            case "amar_ekushey_hall":
                return R.style.Theme_SmartHall_SlateNavy;
            case THEME_FOREST:
            case "forest":
                return R.style.Theme_SmartHall_ForestSage;
            case THEME_INDIGO:
            case "indigo":
                return R.style.Theme_SmartHall_ModernIndigo;
            case THEME_CHARCOAL:
            case "charcoal":
                return R.style.Theme_SmartHall_CharcoalWhite;
            case THEME_CRIMSON:
            default:
                return R.style.Theme_SmartHall_Crimson;
        }
    }

    public static int getPrimaryColor(Context context) {
        if (context == null) return Color.parseColor("#1A237E");
        String themeKey = getSelectedTheme(context);
        return getPrimaryColorForTheme(themeKey);
    }

    public static int getPrimaryDarkColor(Context context) {
        if (context == null) return Color.parseColor("#172554");
        String themeKey = getSelectedTheme(context);
        return getPrimaryDarkColorForTheme(themeKey);
    }

    public static int getPrimaryColorForTheme(String themeKey) {
        if (themeKey == null) return Color.parseColor("#1A237E");
        switch (themeKey) {
            case THEME_NAVY:
            case "navy":
            case "amar_ekushey":
            case "amar_ekushey_hall":
                return Color.parseColor("#1A237E");
            case THEME_FOREST:
            case "forest":
                return Color.parseColor("#1B5E20");
            case THEME_INDIGO:
            case "indigo":
                return Color.parseColor("#4F46E5");
            case THEME_CHARCOAL:
            case "charcoal":
                return Color.parseColor("#18181B");
            case THEME_CRIMSON:
            default:
                return Color.parseColor("#C62828");
        }
    }

    public static int getPrimaryDarkColorForTheme(String themeKey) {
        if (themeKey == null) return Color.parseColor("#172554");
        switch (themeKey) {
            case THEME_NAVY:
            case "navy":
            case "amar_ekushey":
            case "amar_ekushey_hall":
                return Color.parseColor("#172554");
            case THEME_FOREST:
            case "forest":
                return Color.parseColor("#0E3A13");
            case THEME_INDIGO:
            case "indigo":
                return Color.parseColor("#3730A3");
            case THEME_CHARCOAL:
            case "charcoal":
                return Color.parseColor("#09090B");
            case THEME_CRIMSON:
            default:
                return Color.parseColor("#8E0000");
        }
    }

    public static List<ThemeItem> getThemeList() {
        List<ThemeItem> list = new ArrayList<>();
        list.add(new ThemeItem(THEME_CRIMSON, "Crimson Minimal", "Classic bold red and white hall identity", Color.parseColor("#C62828"), Color.parseColor("#FFCDD2")));
        list.add(new ThemeItem(THEME_NAVY, "Slate Navy & Soft White", "Deep ocean blue with serene slate accents", Color.parseColor("#1A237E"), Color.parseColor("#3B82F6")));
        list.add(new ThemeItem(THEME_FOREST, "Forest & Sage", "Natural deep emerald with calming sage tones", Color.parseColor("#1B5E20"), Color.parseColor("#4CAF50")));
        list.add(new ThemeItem(THEME_INDIGO, "Modern Indigo & Amber", "Vibrant high-contrast indigo with warm amber", Color.parseColor("#4F46E5"), Color.parseColor("#F59E0B")));
        list.add(new ThemeItem(THEME_CHARCOAL, "Charcoal & Pure White", "Minimalist dark slate with crisp stark surfaces", Color.parseColor("#18181B"), Color.parseColor("#FFFFFF")));
        return list;
    }

    public static class ThemeItem {
        private final String key;
        private final String title;
        private final String description;
        private final int primaryColor;
        private final int secondaryColor;

        public ThemeItem(String key, String title, String description, int primaryColor, int secondaryColor) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
        }

        public String getKey() {
            return key;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public int getPrimaryColor() {
            return primaryColor;
        }

        public int getSecondaryColor() {
            return secondaryColor;
        }
    }
}