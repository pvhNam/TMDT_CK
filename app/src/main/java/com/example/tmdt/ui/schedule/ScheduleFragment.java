package com.example.tmdt.ui.schedule;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;

/** Navigation destination; scheduling services have not been connected yet. */
public final class ScheduleFragment extends Fragment {
    public ScheduleFragment(){super(R.layout.fragment_schedule);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState){
        view.findViewById(R.id.return_home).setOnClickListener(v->((MainActivity)requireActivity()).show("home"));
    }
}
