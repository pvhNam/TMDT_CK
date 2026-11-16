package com.example.tmdt.ui.requests;

import androidx.fragment.app.Fragment;
import com.example.tmdt.ui.common.SectionHubFragment;

public final class RequestsHubFragment extends SectionHubFragment {
    @Override protected String title(){return "Quản lý tin đăng";}
    @Override protected String selectionKey(){return "requests";}
    @Override protected String[] sections(){return new String[]{"Tin của tôi","Tìm tin đăng"};}
    @Override protected Fragment page(int index){return index==0?new ManageTutorRequestsFragment():new SearchTutorRequestsFragment();}
}
