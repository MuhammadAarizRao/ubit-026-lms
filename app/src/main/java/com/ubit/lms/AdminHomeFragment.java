package com.ubit.lms;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AdminHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);

        View addUserButton = view.findViewById(R.id.addUserButton);
        addUserButton.setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddUserActivity.class)));

        View addCourseButton = view.findViewById(R.id.addCourseButton);
        addCourseButton.setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddCourseActivity.class)));

        return view;
    }
}