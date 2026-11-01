package com.example.smarthall.ui.adapters;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.R;
import com.example.smarthall.databinding.ItemRoomCardBinding;
import com.example.smarthall.model.Room;

import java.util.ArrayList;
import java.util.List;

public class RoomCardAdapter extends RecyclerView.Adapter<RoomCardAdapter.RoomViewHolder> {

    public interface OnRoomClickListener {
        void onRoomClick(Room room);
    }

    private List<Room> roomList;
    private final OnRoomClickListener listener;

    public RoomCardAdapter(List<Room> roomList, OnRoomClickListener listener) {
        this.roomList = roomList != null ? roomList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<Room> newList) {
        this.roomList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRoomCardBinding binding = ItemRoomCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RoomViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Room room = roomList.get(position);
        holder.bind(room, listener);
    }

    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        private final ItemRoomCardBinding binding;

        RoomViewHolder(ItemRoomCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Room room, OnRoomClickListener listener) {
            int colorPrimary = 0xFFC62828;
            TypedValue typedValue = new TypedValue();
            if (itemView.getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
                colorPrimary = typedValue.data;
            }

            String roomId = room.getRoomId() != null ? room.getRoomId() : "N/A";
            binding.tvCardRoomId.setText("Room " + roomId);
            binding.tvCardFloor.setText("Floor " + room.getFloor() + " • 4 Seats");

            int actualCount = room.getActualOccupiedSeatsCount();
            binding.tvCardOccupancyBadge.setText(actualCount + "/4 Occupied");
            if (actualCount >= 4) {
                binding.tvCardOccupancyBadge.setBackgroundResource(R.drawable.bg_badge_occupied);
                binding.tvCardOccupancyBadge.setTextColor(Color.WHITE);
            } else {
                binding.tvCardOccupancyBadge.setBackgroundResource(R.drawable.bg_badge_available);
                binding.tvCardOccupancyBadge.setTextColor(colorPrimary);
            }

            updateSeatBadge(binding.badgeSeatA, room.isSeatAvailable(Room.SEAT_A), colorPrimary);
            updateSeatBadge(binding.badgeSeatB, room.isSeatAvailable(Room.SEAT_B), colorPrimary);
            updateSeatBadge(binding.badgeSeatC, room.isSeatAvailable(Room.SEAT_C), colorPrimary);
            updateSeatBadge(binding.badgeSeatD, room.isSeatAvailable(Room.SEAT_D), colorPrimary);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRoomClick(room);
                }
            });
        }

        private void updateSeatBadge(TextView badge, boolean isAvailable, int colorPrimary) {
            if (isAvailable) {
                badge.setBackgroundResource(R.drawable.bg_badge_available);
                badge.setTextColor(colorPrimary);
            } else {
                badge.setBackgroundResource(R.drawable.bg_badge_occupied);
                badge.setTextColor(Color.WHITE);
            }
        }
    }
}
