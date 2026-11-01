package com.example.smarthall.ui.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthall.databinding.ItemUnallocatedStudentBinding;
import com.example.smarthall.model.Student;

import java.util.ArrayList;
import java.util.List;

public class UnallocatedStudentAdapter extends RecyclerView.Adapter<UnallocatedStudentAdapter.StudentViewHolder> {

    public interface OnStudentSelectListener {
        void onStudentSelect(Student student);
    }

    private List<Student> fullList = new ArrayList<>();
    private List<Student> filteredList = new ArrayList<>();
    private final OnStudentSelectListener listener;

    public UnallocatedStudentAdapter(List<Student> students, OnStudentSelectListener listener) {
        this.fullList = students != null ? new ArrayList<>(students) : new ArrayList<>();
        this.filteredList = new ArrayList<>(fullList);
        this.listener = listener;
    }

    public void updateList(List<Student> students) {
        this.fullList = students != null ? new ArrayList<>(students) : new ArrayList<>();
        this.filteredList = new ArrayList<>(fullList);
        notifyDataSetChanged();
    }

    public int filter(String query) {
        filteredList.clear();
        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(fullList);
        } else {
            String lower = query.toLowerCase().trim();
            for (Student s : fullList) {
                String name = s.getName() != null ? s.getName().toLowerCase() : "";
                String roll = !TextUtils.isEmpty(s.getRollNumber()) ? s.getRollNumber().toLowerCase() : (s.getRoll() != null ? s.getRoll().toLowerCase() : "");
                if (name.contains(lower) || roll.contains(lower)) {
                    filteredList.add(s);
                }
            }
        }
        notifyDataSetChanged();
        return filteredList.size();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUnallocatedStudentBinding binding = ItemUnallocatedStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StudentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = filteredList.get(position);
        holder.bind(student, listener);
    }

    @Override
    public int getItemCount() {
        return filteredList != null ? filteredList.size() : 0;
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final ItemUnallocatedStudentBinding binding;

        StudentViewHolder(ItemUnallocatedStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Student student, OnStudentSelectListener listener) {
            binding.tvStudentSelectName.setText(student.getName() != null ? student.getName() : "Student");
            String roll = !TextUtils.isEmpty(student.getRollNumber()) ? student.getRollNumber() : student.getRoll();
            String dept = !TextUtils.isEmpty(student.getDepartment()) ? student.getDepartment() : "N/A";
            binding.tvStudentSelectRoll.setText("Roll: " + roll + " • " + dept);

            binding.btnSelectStudent.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStudentSelect(student);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStudentSelect(student);
                }
            });
        }
    }
}
