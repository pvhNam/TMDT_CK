package com.example.tmdt.ui.exchange;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.tmdt.AgreementState;
import com.example.tmdt.AgreementTerm;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.Ui;
import java.util.Arrays;

/**
 * Hợp đồng cộng tác giữa gia sư và hệ thống (UC23). Gia sư đã được duyệt đọc mẫu GIA_SU_HE_THONG của quản trị viên
 * rồi ký 1 hoặc 2 năm; phải còn hiệu lực mới gửi được hợp đồng dạy cho học viên. Trong thời hạn, bản đã ký giữ nguyên
 * (mẫu mới chỉ áp dụng khi gia hạn); 30 ngày cuối được gia hạn; bị chấm dứt do vi phạm thì 30 ngày sau mới ký lại.
 */
public final class AgreementFragment extends ScreenFragment {
    private boolean agreed;
    private int consentVersion=-1;
    private int years=1;
    private LinearLayout body;

    @Override public void onCreate(@Nullable Bundle state){
        super.onCreate(state);
        if(state!=null){agreed=state.getBoolean("agreed");consentVersion=state.getInt("consent_version",-1);years=state.getInt("years",1);}
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
                state.agreement(),state.phase(),years,state.templateLoaded(AgreementState.TUTOR_PLATFORM),state.agreementLoaded(),state.busy(),state.loadError());
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
        long now=System.currentTimeMillis();
        AgreementTerm.Phase phase=state.phase(now);
        boolean canSign=AgreementTerm.canSign(phase)&&template!=null, renewal=AgreementTerm.renewal(phase);
        // Trong thời hạn (hoặc đang chờ được ký lại) thì hiện bản đã ký; tới lúc ký mới/gia hạn thì hiện mẫu hiện hành sẽ ký.
        boolean showTemplate=signed==null||canSign;
        String title=showTemplate?template.title:signed.title, content=showTemplate?template.content:signed.content;
        int version=showTemplate?template.version:signed.version;
        if(canSign&&template!=null&&consentVersion!=template.version){consentVersion=template.version;agreed=false;}
        java.time.ZoneId zone=java.time.ZoneId.systemDefault();

        String status;int color,fill;
        switch(phase){
            case ACTIVE:status="Đang hiệu lực";color=Ui.GREEN;fill=Ui.GREEN_BG;break;
            case RENEWED:status="Đã gia hạn";color=Ui.GREEN;fill=Ui.GREEN_BG;break;
            case EXPIRING:status="Sắp hết hạn";color=Ui.ORANGE;fill=Ui.ORANGE_BG;break;
            case EXPIRED:status="Đã hết hạn";color=Ui.RED;fill=0xFFFFF0F0;break;
            case TERMINATED_WAIT:case TERMINATED_CAN_SIGN:status="Đã chấm dứt";color=Ui.RED;fill=0xFFFFF0F0;break;
            case LEGACY:status="Cần ký theo thời hạn";color=Ui.ORANGE;fill=Ui.ORANGE_BG;break;
            default:status="Chưa ký";color=Ui.ORANGE;fill=Ui.ORANGE_BG;
        }
        final String statusText=status;final int statusColor=color,statusFill=fill;
        rows.add("summary",Arrays.asList(title,version,status,signed==null?0:signed.end),()->{
            LinearLayout card=ui.card(),top=ui.row();
            top.addView(ui.icon(R.drawable.ic_ex_shield,28,Ui.BLUE));ui.gap(top,10);
            ui.weight(top,ui.text("Hợp đồng cộng tác",14,Ui.MUTED,false));
            top.addView(ui.pill(statusText,11,statusColor,statusFill));
            ui.add(card,top);ui.space(card,14);ui.add(card,ui.text(title,17,Ui.INK,true));ui.space(card,8);
            ui.add(card,ui.text("Phiên bản "+version+(signed!=null&&!showTemplate?" · Ký ngày "+ExUi.date(signed.signedAt):""),12,Ui.MUTED,false));
            return card;
        },ui.dp(14));

        String note=phaseNote(phase,signed,now,zone,state.newerTemplate()&&!showTemplate?template:null);
        if(!note.isEmpty())rows.add("phase-note",note,()->ui.note(R.drawable.ic_info,note,
                AgreementTerm.active(phase)?Ui.PALE:Ui.ORANGE_BG,AgreementTerm.active(phase)?Ui.BLUE:Ui.ORANGE,Ui.INK),ui.dp(14));

        String tutorName=signed!=null&&!showTemplate?signed.name:exchange.myName();
        boolean termKnown=signed!=null&&signed.end>0&&!showTemplate;
        rows.add("parties",Arrays.asList(tutorName,termKnown,signed==null?0:signed.start,signed==null?0:signed.end,signed==null?0:signed.renewals),()->{
            LinearLayout parties=ui.table();
            ui.tableRow(parties,R.drawable.ic_file,"Bên A","Hệ thống TMĐT kết nối gia sư");
            ui.tableRow(parties,R.drawable.ic_user,"Bên B (gia sư)",tutorName);
            if(termKnown){
                ui.tableRow(parties,R.drawable.ic_calendar,"Thời hạn",signed.years+" năm");
                ui.tableRow(parties,R.drawable.ic_calendar,"Hiệu lực",AgreementTerm.date(signed.start,zone)+" – "+AgreementTerm.date(signed.end-1,zone));
                if(signed.renewals>0)ui.tableRow(parties,R.drawable.ic_ex_shield,"Đã gia hạn",signed.renewals+" lần");
            }
            return parties;
        },ui.dp(14));
        rows.add("terms",content,()->{
            LinearLayout card=ui.card();ui.add(card,ui.text("Nội dung hợp đồng",15,Ui.INK,true));ui.space(card,12);
            TextView text=ui.text(content,14,Ui.INK,false);text.setLineSpacing(ui.dp(5),1);text.setTextIsSelectable(true);
            ui.add(card,text);return card;
        },ui.dp(12));

        // Điều khoản chung: bản đã ký nguyên văn, hoặc bản xem trước theo số năm đang chọn.
        String clauses;
        if(canSign){
            long start=AgreementTerm.newStart(phase,now,signed==null?0:signed.end);
            clauses=AgreementTerm.clauses(years,start,AgreementTerm.end(start,years),zone);
        }else clauses=signed==null?"":signed.clauses;
        if(!clauses.isEmpty())rows.add("clauses",clauses,()->{
            LinearLayout card=ui.card();ui.add(card,ui.text("Điều khoản chung: thời hạn, vi phạm, gia hạn",15,Ui.INK,true));ui.space(card,12);
            TextView text=ui.text(clauses,14,Ui.INK,false);text.setLineSpacing(ui.dp(5),1);text.setTextIsSelectable(true);
            ui.add(card,text);return card;
        },ui.dp(12));

        if (canSign) {
            rows.add("years",Arrays.asList(years,state.busy()),()->{
                LinearLayout box=ui.column();
                ui.add(box,ui.text(renewal?"Gia hạn thêm":"Thời hạn hợp đồng",14,Ui.INK,true));ui.space(box,8);
                LinearLayout chips=ui.row();
                chips.addView(ui.choice("1 năm",years==1,()->{if(!state.busy()){years=1;refresh();}}));ui.gap(chips,8);
                chips.addView(ui.choice("2 năm",years==2,()->{if(!state.busy()){years=2;refresh();}}));
                ui.add(box,chips);return box;
            },ui.dp(12));
            rows.add("consent",Arrays.asList(version,state.busy(),years),()->{
                android.widget.CheckBox check=ui.check("Tôi đã đọc và đồng ý với hợp đồng cộng tác và điều khoản chung",agreed,(b,checked)->{agreed=checked;refresh();});
                check.setEnabled(!state.busy());return check;
            },ui.dp(8));
            boolean ready = agreed && !state.busy();
            String label=state.busy()?"Đang ký…":renewal?"Gia hạn thêm "+years+" năm":"Ký hợp đồng "+years+" năm";
            rows.add("sign",Arrays.asList(version,agreed,state.busy(),years,renewal),()->{
                LinearLayout actions=ui.column();
                ui.addAction(actions,ui.action(label,0,ready?Ui.PRIMARY:Ui.DISABLED,
                        ready?()->state.sign(exchange.myName(),years):null),48);return actions;
            },0);
        }
        rows.commit();
    }
    /** Một câu giải thích tình trạng hiện tại và việc gia sư cần làm. */
    private static String phaseNote(AgreementTerm.Phase phase,AgreementState.Agreement a,long now,java.time.ZoneId zone,AgreementState.Template newer){
        String extra=newer==null?"":" Hệ thống đã có mẫu phiên bản "+newer.version+"; bản bạn đã ký giữ nguyên tới hết thời hạn, mẫu mới áp dụng khi gia hạn.";
        switch(phase){
            case ACTIVE:return "Còn "+AgreementTerm.daysLeft(now,a.end)+" ngày. Được gia hạn từ ngày "
                    +AgreementTerm.date(a.end-AgreementTerm.RENEW_WINDOW_DAYS*AgreementTerm.DAY,zone)+"."+extra;
            case EXPIRING:return "Hợp đồng hết hạn sau "+AgreementTerm.daysLeft(now,a.end)+" ngày ("+AgreementTerm.date(a.end-1,zone)
                    +"). Gia hạn bên dưới: kỳ mới bắt đầu ngay khi kỳ này kết thúc, theo mẫu hiện hành.";
            case RENEWED:return "Đã gia hạn. Kỳ mới có hiệu lực từ "+AgreementTerm.date(a.start,zone)+"; tới lúc đó kỳ trước vẫn còn hiệu lực.";
            case EXPIRED:return "Hợp đồng đã hết hạn ngày "+AgreementTerm.date(a.end-1,zone)+". Bạn không gửi được hợp đồng dạy mới cho tới khi ký lại.";
            case TERMINATED_WAIT:return "Hệ thống đã chấm dứt hợp đồng ngày "+AgreementTerm.date(a.terminatedAt,zone)+" do vi phạm: "
                    +a.terminateReason+". Bạn được ký hợp đồng mới từ ngày "+AgreementTerm.date(AgreementTerm.resignAllowedAt(a.terminatedAt),zone)+".";
            case TERMINATED_CAN_SIGN:return "Hợp đồng trước bị chấm dứt do vi phạm: "+a.terminateReason+". Bạn có thể ký hợp đồng mới bên dưới.";
            case LEGACY:return "Hợp đồng bạn ký trước đây chưa có thời hạn. Ký lại 1 hoặc 2 năm để tiếp tục gửi hợp đồng dạy.";
            default:return "";
        }
    }
    private void message(StableViews rows,String message){
        rows.add("message",message,()->ui.note(R.drawable.ic_info,message,Ui.PALE,Ui.BLUE,Ui.INK),0);rows.commit();
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out){
        super.onSaveInstanceState(out);out.putBoolean("agreed",agreed);out.putInt("consent_version",consentVersion);out.putInt("years",years);
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
