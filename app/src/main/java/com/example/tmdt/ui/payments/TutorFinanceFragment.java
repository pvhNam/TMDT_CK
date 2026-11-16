package com.example.tmdt.ui.payments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.example.tmdt.ui.common.SectionHubFragment;

public final class TutorFinanceFragment extends SectionHubFragment {
    public static TutorFinanceFragment income(){
        return section(1);
    }
    public static TutorFinanceFragment wallet(){return section(0);}
    private static TutorFinanceFragment section(int index){TutorFinanceFragment page=new TutorFinanceFragment();Bundle args=new Bundle();args.putInt("section",index);page.setArguments(args);return page;}
    @Override protected String title(){return "Ví & thanh toán";}
    @Override protected String selectionKey(){return "finance_wallet";}
    @Override protected String[] sections(){return new String[]{"Ví","Thu nhập","Thanh toán"};}
    @Override protected Fragment page(int index){return index==0?new TutorWalletFragment():index==1?new TutorIncomeFragment():new PaymentHistoryFragment();}
}
