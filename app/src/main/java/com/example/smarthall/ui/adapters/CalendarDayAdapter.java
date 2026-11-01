package com.example.smarthall.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ItemCalendarDayBinding;
import com.example.smarthall.model.CalendarDay;

import java.util.ArrayList;
import java.util.List;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {

    private List<CalendarDay> dayList;

    public CalendarDayAdapter(List<CalendarDay> dayList) {
        this.dayList = dayList != null ? new ArrayList<>(dayList) : new ArrayList<>();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarDayBinding binding = ItemCalendarDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DayViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        holder.bind(dayList.get(position));
    }

    @Override
    public int getItemCount() {
        return dayList.size();
    }

    public void updateDays(List<CalendarDay> newDays) {
        if (this.dayList == null) {
            this.dayList = new ArrayList<>();
        }
        this.dayList.clear();
        if (newDays != null) {
            this.dayList.addAll(newDays);
        }
        notifyDataSetChanged();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        private final ItemCalendarDayBinding binding;

        DayViewHolder(ItemCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CalendarDay day) {
            if (!day.isCurrentMonth() || day.getDayNumber() <= 0) {
                binding.tvDayNumber.setText("");
                binding.tvDayNumber.setBackground(null);
                binding.cellContainer.setVisibility(View.INVISIBLE);
                return;
            }

            binding.cellContainer.setVisibility(View.VISIBLE);
            binding.tvDayNumber.setText(String.valueOf(day.getDayNumber()));
            binding.tvDayNumber.setPaintFlags(binding.tvDayNumber.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            Context context = itemView.getContext();
            TypedValue typedValue = new TypedValue();
            int colorPrimary = 0xFFC62828;
            if (context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
                colorPrimary = typedValue.data;
            }

            String status = day.getStatus();
            if ("MEAL_CLOSE".equalsIgnoreCase(status)) {
                binding.tvDayNumber.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_meal_close));
                binding.tvDayNumber.setTextColor(Color.WHITE);
            } else if ("HALL_CLOSE".equalsIgnoreCase(status)) {
                binding.tvDayNumber.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_hall_close));
                binding.tvDayNumber.setTextColor(colorPrimary);
            } else if ("OFF".equalsIgnoreCase(status)) {
                binding.tvDayNumber.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_meal_off));
                binding.tvDayNumber.setTextColor(colorPrimary);
            } else {
                binding.tvDayNumber.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_meal_on));
                binding.tvDayNumber.setTextColor(Color.WHITE);
            }
        }
    }
}
