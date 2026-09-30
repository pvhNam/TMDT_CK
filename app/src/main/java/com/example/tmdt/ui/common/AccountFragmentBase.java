package com.example.tmdt.ui.common;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.AccountState;
import com.example.tmdt.AccountValidation;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.google.android.material.appbar.MaterialToolbar;

/** Shared form behavior only. Each feature owns its layout and event handlers. */
public abstract class AccountFragmentBase extends Fragment {
    protected AccountState state;
    protected AccountFragmentBase(int layout) { super(layout); }

    @Override public final void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState) {
        super.onViewCreated(view,savedInstanceState);
        state=new ViewModelProvider(requireActivity()).get(AccountState.class);
        ((MaterialToolbar)view.findViewById(R.id.toolbar)).setNavigationOnClickListener(v->host().back());
        bindFields(view);
        state.changes().observe(getViewLifecycleOwner(),ignored->{
            showMessage(view.findViewById(R.id.error_message),state.error());
            showMessage(view.findViewById(R.id.notice_message),state.notice());
            view.findViewById(R.id.progress).setVisibility(state.isBusy()?View.VISIBLE:View.GONE);
            enable(view.findViewById(R.id.form),!state.isBusy());
            renderState();
        });
    }
    protected abstract void bindFields(View view);
    protected void renderState() {}
    protected MainActivity host() { return (MainActivity)requireActivity(); }
    protected void go(String destination) {
        state.clearFeedback();
        state.removeDraft("login.password");
        host().hideKeyboard();
        host().show(destination);
    }
    protected void draft(EditText input,String key,String fallback) {
        input.setText(state.draft(key,fallback));
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                state.draft(key,s.toString(),false);input.setError(null);
            }
            public void afterTextChanged(Editable editable){}
        });
    }
    protected void password(EditText input,CheckBox toggle,String key) {
        draft(input,key,"");
        toggle.setOnCheckedChangeListener((button,checked)->{
            int selection=input.getSelectionStart();
            input.setTransformationMethod(checked?null:PasswordTransformationMethod.getInstance());
            input.setSelection(Math.max(0,selection));
        });
    }
    protected String text(EditText input) { return input.getText().toString().trim(); }
    protected boolean check(EditText input,boolean valid,int error) {
        if(valid)return true;input.setError(getString(error));input.requestFocus();return false;
    }
    protected boolean email(EditText input) { return check(input,AccountValidation.validEmail(text(input)),R.string.error_email); }
    protected boolean name(EditText input) { return check(input,AccountValidation.validName(text(input)),R.string.error_name); }
    protected boolean phone(EditText input) { return check(input,AccountValidation.validPhone(text(input)),R.string.error_phone); }
    private void showMessage(TextView label,String message) {
        label.setText(message);label.setVisibility(message.isEmpty()?View.GONE:View.VISIBLE);
    }
    private void enable(View view,boolean enabled) {
        view.setEnabled(enabled);
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)enable(group.getChildAt(i),enabled);
        }
    }
}

