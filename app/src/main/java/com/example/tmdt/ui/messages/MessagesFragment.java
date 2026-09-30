package com.example.tmdt.ui.messages;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;

/** Visual destination only; removed conversations/messages flows remain disconnected. */
public final class MessagesFragment extends Fragment {
    public MessagesFragment(){super(R.layout.fragment_messages);}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState){
        view.findViewById(R.id.return_home).setOnClickListener(v->((MainActivity)requireActivity()).show("home"));
    }
}
