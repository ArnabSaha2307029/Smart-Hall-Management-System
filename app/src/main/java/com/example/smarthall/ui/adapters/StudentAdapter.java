package com.example.smarthall.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.databinding.ItemStudentBinding;
import com.example.smarthall.model.FeeRecord;
import com.example.smarthall.model.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<Student> studentList;
    private OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onStudentClick(Student student);
    }

    public StudentAdapter(List<Student> studentList, OnStudentClickListener listener) {
        this.studentList = studentList != null ? studentList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentBinding binding = ItemStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StudentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.bind(student, listener);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public void updateData(List<Student> newList) {
        this.studentList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentBinding binding;

        StudentViewHolder(ItemStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Student student, OnStudentClickListener listener) {
            binding.tvStudentName.setText(student.getName());
            binding.tvStudentRoll.setText("Roll: " + student.getRollNumber());
            binding.tvStudentDept.setText(student.getDepartment());

            // Room/Seat badge
            if (student.isAssigned()) {
                binding.tvRoomSeatBadge.setText("Room " + student.getAllocatedRoom() + " - Seat " + student.getAllocatedSeat());
                binding.tvRoomSeatBadge.setBackgroundColor(Color.parseColor("#ECEFF1"));
                binding.tvRoomSeatBadge.setTextColor(Color.parseColor("#37474F"));
            } else {
                binding.tvRoomSeatBadge.setText("Unassigned");
                binding.tvRoomSeatBadge.setBackgroundColor(Color.parseColor("#F5F5F5"));
                binding.tvRoomSeatBadge.setTextColor(Color.parseColor("#9E9E9E"));
            }

            // Fee status badge
            if (FeeRecord.STATUS_PAID.equals(student.getFeeStatus())) {
                binding.tvFeeStatusBadge.setText("PAID");
                binding.tvFeeStatusBadge.setBackgroundColor(Color.parseColor("#E8F5E9"));
                binding.tvFeeStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
            } else {
                binding.tvFeeStatusBadge.setText("UNPAID");
                binding.tvFeeStatusBadge.setBackgroundColor(Color.parseColor("#FFEBEE"));
                binding.tvFeeStatusBadge.setTextColor(Color.parseColor("#C62828"));
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onStudentClick(student);
            });
        }
    }
}
