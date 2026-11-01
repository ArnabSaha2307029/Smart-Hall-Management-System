package com.example.smarthall.ui.theme;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smarthall.databinding.DialogThemeSelectorBinding;
import com.example.smarthall.ui.adapters.ThemeAdapter;
import com.example.smarthall.util.ThemeHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class ThemeSelectorBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ThemeSelectorBottomSheet";

    private DialogThemeSelectorBinding binding;

    public static ThemeSelectorBottomSheet newInstance() {
        return new ThemeSelectorBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogThemeSelectorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCloseThemeSelector.setOnClickListener(v -> dismiss());

        String currentTheme = ThemeHelper.getSelectedTheme(requireContext());
        List<ThemeHelper.ThemeItem> themeList = ThemeHelper.getThemeList();

        ThemeAdapter adapter = new ThemeAdapter(themeList, currentTheme, item -> {
            if (!item.getKey().equalsIgnoreCase(currentTheme)) {
                ThemeHelper.setTheme(requireContext(), item.getKey());
                dismiss();
                Activity activity = getActivity();
                if (activity != null) {
                    activity.recreate();
                }
            } else {
                dismiss();
            }
        });

        binding.rvThemeOptions.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvThemeOptions.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}