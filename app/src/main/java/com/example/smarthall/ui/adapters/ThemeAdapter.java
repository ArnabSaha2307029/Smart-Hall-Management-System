package com.example.smarthall.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.databinding.ItemThemeOptionBinding;
import com.example.smarthall.util.ThemeHelper;

import java.util.List;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder> {

    public interface OnThemeSelectListener {
        void onThemeSelect(ThemeHelper.ThemeItem item);
    }

    private final List<ThemeHelper.ThemeItem> themeList;
    private String currentThemeKey;
    private final OnThemeSelectListener listener;

    public ThemeAdapter(List<ThemeHelper.ThemeItem> themeList, String currentThemeKey, OnThemeSelectListener listener) {
        this.themeList = themeList;
        this.currentThemeKey = currentThemeKey;
        this.listener = listener;
    }

    public void setCurrentThemeKey(String key) {
        this.currentThemeKey = key;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemThemeOptionBinding binding = ItemThemeOptionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ThemeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ThemeViewHolder holder, int position) {
        ThemeHelper.ThemeItem item = themeList.get(position);
        boolean isSelected = item.getKey().equalsIgnoreCase(currentThemeKey);
        holder.bind(item, isSelected, listener);
    }

    @Override
    public int getItemCount() {
        return themeList != null ? themeList.size() : 0;
    }

    static class ThemeViewHolder extends RecyclerView.ViewHolder {
        private final ItemThemeOptionBinding binding;

        ThemeViewHolder(ItemThemeOptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ThemeHelper.ThemeItem item, boolean isSelected, OnThemeSelectListener listener) {
            binding.viewThemeSwatch.setColors(item.getPrimaryColor(), item.getSecondaryColor());

            float density = binding.getRoot().getContext().getResources().getDisplayMetrics().density;
            if (isSelected) {
                binding.cardThemeOption.setStrokeColor(item.getPrimaryColor());
                binding.cardThemeOption.setStrokeWidth((int) (3 * density));
                binding.cardThemeOption.setCardElevation(4 * density);
                binding.cardThemeOption.setScaleX(1.08f);
                binding.cardThemeOption.setScaleY(1.08f);
            } else {
                binding.cardThemeOption.setStrokeWidth(0);
                binding.cardThemeOption.setCardElevation(0f);
                binding.cardThemeOption.setScaleX(1.0f);
                binding.cardThemeOption.setScaleY(1.0f);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onThemeSelect(item);
                }
            });
        }
    }
}