package com.example.smarthall.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.databinding.ItemStudentBinding;
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
        this.studentList = studentList != null ? new ArrayList<>(studentList) : new ArrayList<>();
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

    public void updateData(List<Student> newStudents) {
        if (this.studentList == null) {
            this.studentList = new ArrayList<>();
        }
        this.studentList.clear();
        if (newStudents != null) {
            this.studentList.addAll(newStudents);
        }
        notifyDataSetChanged();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentBinding binding;

        StudentViewHolder(ItemStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Student student, OnStudentClickListener listener) {
            binding.tvStudentRoll.setText("Roll: " + student.getRollNumber());
            binding.tvStudentName.setText(student.getName());

            if (student.isAssigned()) {
                binding.tvStudentRoom.setText("Room " + student.getAllocatedRoom() + " • Seat " + student.getAllocatedSeat());
            } else if (student.getAllocatedRoom() != null && !student.getAllocatedRoom().isEmpty()) {
                binding.tvStudentRoom.setText("Room " + student.getAllocatedRoom());
            } else {
                binding.tvStudentRoom.setText("Room: Unassigned");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStudentClick(student);
                }
            });
        }
    }
}
