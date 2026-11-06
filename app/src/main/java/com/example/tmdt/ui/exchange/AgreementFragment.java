package com.example.tmdt.ui.exchange;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.tmdt.AgreementState;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.util.Arrays;

/**
 * Hợp đồng cộng tác giữa gia sư và hệ thống (UC23). Gia sư đã được duyệt đọc mẫu GIA_SU_HE_THONG của quản trị viên
 * rồi ký; phải ký trước khi gửi hợp đồng dạy cho học viên. Mẫu có phiên bản mới thì ký lại.
 */
public final class AgreementFragment extends ScreenFragment {
    private boolean agreed;
    private int consentVersion=-1;
    private LinearLayout body;

    @Override public void onCreate(@Nullable Bundle state){
        super.onCreate(state);
        if(state!=null){agreed=state.getBoolean("agreed");consentVersion=state.getInt("consent_version",-1);}
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        Exchange exchange = ((MainActivity) requireActivity()).exchange;
        exchange.agreements.changes().observe(getViewLifecycleOwner(), ignored -> {
            String notice = exchange.agreements.takeNotice();
            if (!notice.isEmpty()) classroom.notice(notice);
            refreshData();
        });
        exchange.agreements.start();
    }

    @Override protected Object dataSnapshot(){
        AgreementState state=((MainActivity)requireActivity()).exchange.agreements;
        return com.example.tmdt.ui.common.RenderSnapshot.of(super.dataSnapshot(),state.template(AgreementState.TUTOR_PLATFORM),
                state.agreement(),state.templateLoaded(AgreementState.TUTOR_PLATFORM),state.agreementLoaded(),state.busy(),state.loadError());
    }

    @Override protected View build() {
        Exchange exchange = ((MainActivity) requireActivity()).exchange;
        LinearLayout root = ui.column();
        if(!com.example.tmdt.ui.common.SectionHubFragment.embedded(this))ui.header(root, "Hợp đồng cộng tác", exchange::back, null);
        body = ui.page(root);render();return root;
    }

    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}

    private void render(){
        Exchange exchange=((MainActivity)requireActivity()).exchange;
        AgreementState state=exchange.agreements;
        StableViews rows=StableViews.at(body);rows.begin();

        AgreementState.Template template = state.template(AgreementState.TUTOR_PLATFORM);
        AgreementState.Agreement signed = state.agreement();
        if (!exchange.isTutor()) {
            message(rows,"Chỉ gia sư đã được duyệt hồ sơ mới ký hợp đồng cộng tác với hệ thống.");return;
        }
        if (!state.templateLoaded(AgreementState.TUTOR_PLATFORM) || !state.agreementLoaded()) {
            message(rows,state.loadError().isEmpty()?"Đang tải hợp đồng…":state.loadError());return;
        }
        if (template == null && signed == null) {
            message(rows,"Chưa có hợp đồng cộng tác.");return;
        }

        boolean showCurrent = signed == null || state.outdated();
        String title = showCurrent ? template.title : signed.title;
        String content = showCurrent ? template.content : signed.content;
        int version=showCurrent?template.version:signed.version;
        if(showCurrent&&consentVersion!=version){consentVersion=version;agreed=false;}
        String status=signed==null?"Chưa ký":showCurrent?"Cần ký lại":"Đã ký";
        rows.add("summary",Arrays.asList(title,version,status,signed==null?0:signed.signedAt),()->{
            LinearLayout card=ui.card(),top=ui.row();
            top.addView(ui.icon(R.drawable.ic_ex_shield,28,Ui.BLUE));ui.gap(top,10);
            ui.weight(top,ui.text("Hợp đồng cộng tác",14,Ui.MUTED,false));
            top.addView(ui.pill(status,11,showCurrent?Ui.ORANGE:Ui.GREEN,showCurrent?Ui.ORANGE_BG:Ui.GREEN_BG));
            ui.add(card,top);ui.space(card,14);ui.add(card,ui.text(title,17,Ui.INK,true));ui.space(card,8);
            String detail="Phiên bản "+version;
            if(signed!=null)detail+=(showCurrent?" · Bản đã ký: "+signed.version:" · Ký ngày "+ExUi.date(signed.signedAt));
            ui.add(card,ui.text(detail,12,Ui.MUTED,false));return card;
        },ui.dp(14));
        String tutorName=signed!=null&&!showCurrent?signed.name:exchange.myName();
        rows.add("parties",tutorName,()->{
            LinearLayout parties=ui.table();
            ui.tableRow(parties,R.drawable.ic_file,"Bên A","Hệ thống TMĐT kết nối gia sư");
            ui.tableRow(parties,R.drawable.ic_user,"Bên B (gia sư)",tutorName);return parties;
        },ui.dp(14));
        rows.add("terms",content,()->{
            LinearLayout card=ui.card();ui.add(card,ui.text("Nội dung hợp đồng",15,Ui.INK,true));ui.space(card,12);
            TextView text=ui.text(content,14,Ui.INK,false);text.setLineSpacing(ui.dp(5),1);text.setTextIsSelectable(true);
            ui.add(card,text);return card;
        },ui.dp(12));
        if (showCurrent) {
            rows.add("consent",Arrays.asList(version,state.busy()),()->{
                android.widget.CheckBox check=ui.check("Tôi đã đọc và đồng ý với hợp đồng cộng tác",agreed,(b,checked)->{agreed=checked;refresh();});
                check.setEnabled(!state.busy());return check;
            },ui.dp(8));
            boolean ready = agreed && !state.busy();
            rows.add("sign",Arrays.asList(version,agreed,state.busy()),()->{
                LinearLayout actions=ui.column();
                ui.addAction(actions,ui.action(state.busy()?"Đang ký…":"Ký hợp đồng",0,ready?Ui.PRIMARY:Ui.DISABLED,
                        ready?()->state.sign(exchange.myName()):null),48);return actions;
            },0);
        }
        rows.commit();
    }
    private void message(StableViews rows,String message){
        rows.add("message",message,()->ui.note(R.drawable.ic_info,message,Ui.PALE,Ui.BLUE,Ui.INK),0);rows.commit();
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out){
        super.onSaveInstanceState(out);out.putBoolean("agreed",agreed);out.putInt("consent_version",consentVersion);
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
