package com.example.android.notepad;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Centralizes reading preference values and applying lightweight theme tweaks.
 */
final class UiPreferences {

    private UiPreferences() {}

    static boolean isDarkTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(SettingsActivity.KEY_DARK_THEME, false);
    }

    static float resolveEditorTextSize(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String size = prefs.getString(SettingsActivity.KEY_EDITOR_TEXT_SIZE, "normal");
        switch (size) {
            case "small":
                return 16f;
            case "large":
                return 22f;
            default:
                return 18f;
        }
    }

    static void styleListContainer(View root, ListView listView, TextView emptyView) {
        Context context = root.getContext();
        boolean dark = isDarkTheme(context);
        int background = context.getResources().getColor(
                dark ? R.color.app_window_background_dark : R.color.app_window_background);
        root.setBackgroundColor(background);
        listView.setBackgroundColor(background);
        if (emptyView != null) {
            emptyView.setTextColor(context.getResources().getColor(
                    dark ? R.color.editor_text_dark : R.color.editor_text_light));
        }
    }

    static void applyEditorStyling(View editorRoot, TextView editorText) {
        Context context = editorRoot.getContext();
        boolean dark = isDarkTheme(context);
        editorRoot.setBackgroundColor(context.getResources().getColor(
                dark ? R.color.app_window_background_dark : R.color.app_window_background));
        editorText.setBackgroundColor(context.getResources().getColor(
                dark ? R.color.editor_background_dark : R.color.editor_background_light));
        editorText.setTextColor(context.getResources().getColor(
                dark ? R.color.editor_text_dark : R.color.editor_text_light));
        float textSize = resolveEditorTextSize(context);
        editorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
    }
}

