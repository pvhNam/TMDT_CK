package com.example.tmdt.ui.exchange;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.tmdt.AgreementState;
import com.example.tmdt.Exchange;
import com.example.tmdt.MainActivity;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;

/**
 * Quản trị viên soạn mẫu hợp đồng (UC23): mẫu học viên – gia sư (điều khoản chung của hợp đồng dạy) và
 * mẫu gia sư – hệ thống (hợp đồng cộng tác). Mỗi lần lưu là một phiên bản mới; hợp đồng đã gửi/đã ký giữ bản cũ.
 */
public final class ContractTemplatesFragment extends ScreenFragment {
    private static final String[] KINDS = {AgreementState.STUDENT_TUTOR, AgreementState.TUTOR_PLATFORM};
    private int tab;
    /** Nội dung đang gõ theo từng mẫu, giữ khi màn được dựng lại. */
    private final String[] titles = new String[2], contents = new String[2];

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) {
            tab = state.getInt("tab");
            for (int i = 0; i < 2; i++) { titles[i] = state.getString("title" + i); contents[i] = state.getString("content" + i); }
        }
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out); out.putInt("tab", tab);
        for (int i = 0; i < 2; i++) { out.putString("title" + i, titles[i]); out.putString("content" + i, contents[i]); }
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        Exchange exchange = ((MainActivity) requireActivity()).exchange;
        exchange.agreements.changes().observe(getViewLifecycleOwner(), ignored -> {
            String notice = exchange.agreements.takeNotice();
            if (!notice.isEmpty()) { classroom.notice(notice); titles[tab] = null; contents[tab] = null; }
            refresh();
        });
        exchange.agreements.start();
    }

    @Override protected View build() {
        Exchange exchange = ((MainActivity) requireActivity()).exchange;
        AgreementState state = exchange.agreements;
        LinearLayout root = ui.column(); ui.header(root, "Mẫu hợp đồng", exchange::back, null);
        ui.tabs(root, new String[]{"Học viên – Gia sư", "Gia sư – Hệ thống"}, tab, index -> { tab = index; refresh(); });
        LinearLayout body = ui.page(root);
        String kind = KINDS[tab];
        if (!state.templateLoaded(kind)) {
            ui.add(body, ui.note(R.drawable.ic_clock, state.loadError().isEmpty() ? "Đang tải mẫu…" : state.loadError(), Ui.PALE, Ui.BLUE, Ui.INK));
            return root;
        }
        AgreementState.Template current = state.template(kind);
        boolean studentTutor = tab == 0;
        ui.add(body, ui.note(R.drawable.ic_info, studentTutor
                ? "Điều khoản chung gắn vào mọi hợp đồng dạy gia sư gửi cho học viên. Gia sư chỉ điền số buổi, học phí và thỏa thuận thêm."
                : "Hợp đồng gia sư ký với hệ thống sau khi được duyệt hồ sơ. Chưa ký thì gia sư chưa gửi được hợp đồng dạy.", Ui.PALE, Ui.BLUE, Ui.INK));
        ui.space(body, 10);
        ui.add(body, ui.text(current == null ? "Chưa có mẫu. Nội dung gợi ý bên dưới, sửa rồi bấm Lưu để tạo phiên bản 1."
                : "Phiên bản " + current.version + " · cập nhật " + ExUi.dateTime(current.updatedAt), 14, Ui.MUTED, false));
        ui.space(body, 12);

        if (titles[tab] == null) titles[tab] = current != null ? current.title : getString(studentTutor ? R.string.ex_template_student_title : R.string.ex_template_tutor_title);
        if (contents[tab] == null) contents[tab] = current != null ? current.content : getString(studentTutor ? R.string.ex_terms_base : R.string.ex_template_tutor_content);

        ui.label(body, "Tiêu đề");
        EditText title = ui.entry("Tiêu đề hợp đồng", false); title.setText(titles[tab]);
        title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        final int index = tab;
        title.addTextChangedListener(watch(value -> titles[index] = value));
        ui.add(body, title); ui.space(body, 12);

        ui.label(body, "Nội dung điều khoản");
        EditText content = ui.entry("Nội dung hợp đồng", true); content.setText(contents[tab]); content.setMinLines(10);
        content.setFilters(new InputFilter[]{new InputFilter.LengthFilter(AgreementState.MAX_CONTENT)});
        content.addTextChangedListener(watch(value -> contents[index] = value));
        ui.add(body, content); ui.space(body, 16);

        boolean ready = !state.busy();
        ui.addAction(body, ui.action(state.busy() ? "Đang lưu…" : current == null ? "Lưu mẫu (phiên bản 1)" : "Lưu phiên bản " + (current.version + 1),
                0, ready ? Ui.PRIMARY : Ui.DISABLED, ready ? () -> save(state, kind) : null), 48);
        ui.space(body, 8);
        ui.add(body, ui.text("Hợp đồng đã gửi và hợp đồng cộng tác đã ký giữ nguyên nội dung cũ. Gia sư sẽ được nhắc ký lại khi mẫu cộng tác có phiên bản mới.", 13, Ui.MUTED, false));
        return root;
    }

    private void save(AgreementState state, String kind) {
        String title = titles[tab] == null ? "" : titles[tab].trim(), content = contents[tab] == null ? "" : contents[tab].trim();
        if (title.length() < 2) { classroom.dialog("Thiếu tiêu đề", "Nhập tiêu đề hợp đồng (ít nhất 2 ký tự)."); return; }
        if (content.length() < 20) { classroom.dialog("Nội dung quá ngắn", "Nội dung điều khoản cần ít nhất 20 ký tự."); return; }
        classroom.hideKeyboard();
        state.saveTemplate(kind, title, content);
    }

    private static TextWatcher watch(java.util.function.Consumer<String> change) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { change.accept(s.toString()); }
        };
    }
}
