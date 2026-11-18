package com.example.tmdt.ui.exchange;

import androidx.fragment.app.Fragment;
import com.example.tmdt.ui.common.SectionHubFragment;

public final class ContractsHubFragment extends SectionHubFragment {
    @Override protected String title(){return "Hợp đồng của tôi";}
    @Override protected String selectionKey(){return "contracts";}
    @Override protected String[] sections(){return new String[]{"Hợp đồng dạy","Với hệ thống"};}
    @Override protected Fragment page(int index){return index==0?new ContractsFragment():new AgreementFragment();}
}
