package com.example.tmdt.ui.groupclass;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import com.example.tmdt.GroupAssignment;
import com.example.tmdt.GroupAssignmentSubmission;
import com.example.tmdt.GroupAssignmentsState;
import com.example.tmdt.R;
import com.example.tmdt.databinding.DialogGroupAssignmentsBinding;
import com.example.tmdt.databinding.ItemGroupAssignmentBinding;
import com.example.tmdt.databinding.ItemGroupSubmissionBinding;
import com.example.tmdt.ui.common.Motion;
import com.example.tmdt.ui.common.ThemeColors;
import com.google.firebase.auth.FirebaseAuth;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Private assignments for the tutor and accepted members of an online group class. */
public final class GroupAssignmentsDialog extends DialogFragment {
    private static final String TAG = "group_assignments";
    private static final String[] FILE_TYPES = {
            "image/jpeg", "image/png", "image/webp", "image/gif", "image/heic", "image/heif",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"
    };
    private DialogGroupAssignmentsBinding binding;
    private GroupAssignmentsState state;
    private boolean renderingEditors, publishing, driveAuthorizing;
    private final AssignmentDriveAuthorization driveAuthorization = new AssignmentDriveAuthorization(this);
    private String listKey = "", submissionKey = "";
    private String fileTarget = "", fileAssignmentId = "", fileOwner = "";
    private AlertDialog reviewDialog;
    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::fileSelected);

    public GroupAssignmentsDialog() { super(R.layout.dialog_group_assignments); }
    public static GroupAssignmentsDialog newInstance(String classId) {
        GroupAssignmentsDialog dialog = new GroupAssignmentsDialog();
        Bundle args = new Bundle(); args.putString("class_id", classId); dialog.setArguments(args);
        return dialog;
    }
    public static void open(Fragment host, String classId) {
        if (!host.isAdded() || classId == null || classId.isEmpty()) return;
        FragmentManager manager = host.getParentFragmentManager();
        if (manager.isStateSaved() || manager.findFragmentByTag(TAG) != null) return;
        newInstance(classId).showNow(manager, TAG);
    }
    @Override public void onCreate(@Nullable Bundle savedState) {
        super.onCreate(savedState); setStyle(STYLE_NO_TITLE, 0);
        if (savedState != null) {
            publishing = savedState.getBoolean("publishing");
            fileTarget = savedState.getString("file_target", "");
            fileAssignmentId = savedState.getString("file_assignment", "");
            fileOwner = savedState.getString("file_owner", "");
        }
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedState) {
        super.onViewCreated(view, savedState);
        binding = DialogGroupAssignmentsBinding.bind(view);
        state = new ViewModelProvider(this).get(GroupAssignmentsState.class);
        state.watch(requireArguments().getString("class_id", ""));
        watch(binding.assignmentTitleInput, "new.title");
        watch(binding.assignmentDescriptionInput, "new.description");
        watch(binding.submissionContentInput, "answer.content");
        binding.assignmentsClose.setOnClickListener(v -> { if (!uiBusy()) dismiss(); });
        binding.assignmentsRetry.setOnClickListener(v -> state.retry());
        binding.assignmentsDriveSync.setOnClickListener(v -> {
            if (state.canSyncDrive() && !uiBusy()) authorizeDrive(state::syncDrivePermissions);
        });
        binding.assignmentsCreateNew.setOnClickListener(v -> {
            state.draft("new.composing", "1");
            if (state.draft("new.due").isEmpty()) {
                Calendar due = Calendar.getInstance(); due.add(Calendar.DAY_OF_YEAR, 7);
                due.set(Calendar.HOUR_OF_DAY, 23); due.set(Calendar.MINUTE, 59);
                due.set(Calendar.SECOND, 0); due.set(Calendar.MILLISECOND, 0);
                state.draft("new.due", Long.toString(due.getTimeInMillis()));
            }
            render(); binding.assignmentTitleInput.requestFocus();
        });
        binding.assignmentCancelCreate.setOnClickListener(v -> { state.draft("new.composing", ""); hideKeyboard(); render(); });
        binding.assignmentDueButton.setOnClickListener(v -> chooseDue());
        binding.assignmentPickFile.setOnClickListener(v -> chooseFile("new"));
        binding.assignmentRemoveFile.setOnClickListener(v -> { state.draft("new.file", ""); renderFiles(); });
        binding.assignmentPublish.setOnClickListener(v -> publish());
        binding.assignmentBack.setOnClickListener(v -> {
            if (uiBusy()) return;
            hideKeyboard(); state.unselect(); submissionKey = "";
            binding.assignmentsScroll.scrollTo(0, 0);
        });
        binding.assignmentToggleClosed.setOnClickListener(v -> toggleClosed());
        binding.submissionPickFile.setOnClickListener(v -> chooseFile("answer"));
        binding.submissionRemoveFile.setOnClickListener(v -> {
            state.draft("answer.file", ""); state.draft("answer.remove", "1");
            state.draft("answer.initialized", "1"); renderFiles();
        });
        binding.submissionSend.setOnClickListener(v -> submit());
        state.changes().observe(getViewLifecycleOwner(), ignored -> render());
    }
    @Override public void onStart() {
        super.onStart(); Window window = requireDialog().getWindow();
        if (window != null) {
            int width = getResources().getDisplayMetrics().widthPixels;
            int maxWidth = Math.round(680 * getResources().getDisplayMetrics().density);
            int margin = Math.round(20 * getResources().getDisplayMetrics().density);
            window.setLayout(Math.min(width - margin, maxWidth), Math.round(getResources().getDisplayMetrics().heightPixels * .92f));
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }
    private void watch(EditText editor, String key) {
        editor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (renderingEditors || state == null) return;
                state.draft(key, s.toString());
                if (key.startsWith("answer.")) state.draft("answer.initialized", "1");
                editor.setError(null);
            }
            @Override public void afterTextChanged(Editable value) {}
        });
    }
    private void render() {
        if (binding == null) return;
        boolean available = state.available(), busy = uiBusy();
        GroupAssignment selected = state.selected(); boolean detail = available && selected != null;
        boolean driveMaterials = state.assignments().stream().anyMatch(a -> a.attachment != null && a.attachment.drive());
        visible(binding.assignmentsDriveSync, available && state.teacher() && driveMaterials);
        binding.assignmentsDriveSync.setEnabled(state.canSyncDrive() && !busy);
        String title = state.classTitle();
        binding.assignmentsClassTitle.setText(title.isEmpty() ? getString(R.string.assignments_class_unknown) : title);
        boolean loading = state.loading() || (detail && state.submissionsLoading());
        visible(binding.assignmentsProgress, loading || busy);
        binding.assignmentsProgress.setIndeterminate(!busy || state.progress() <= 0 || state.progress() >= 100);
        if (!binding.assignmentsProgress.isIndeterminate()) binding.assignmentsProgress.setProgress(state.progress());
        binding.assignmentsProgressText.setText(busy && state.progress() > 0 && state.progress() < 100
                ? getString(R.string.assignments_upload_progress, state.progress()) : getString(R.string.assignments_working));
        if (driveAuthorizing) binding.assignmentsProgressText.setText(R.string.assignments_drive_authorizing);
        visible(binding.assignmentsProgressText, busy); visible(binding.assignmentsOffline, available && state.offline());
        String error = state.error();
        if (detail && error.isEmpty()) error = state.submissionsError();
        if (!available && !state.loading() && error.isEmpty()) error = getString(R.string.assignments_unavailable);
        status(binding.assignmentsError, error); status(binding.assignmentsNotice, state.notice());
        visible(binding.assignmentsRetry, !error.isEmpty() || state.offline());
        binding.assignmentsRetry.setEnabled(!busy); binding.assignmentsClose.setEnabled(!busy);
        setCancelable(!busy); if (getDialog() != null) getDialog().setCanceledOnTouchOutside(!busy);
        visible(binding.assignmentsListPanel, available && !detail); visible(binding.assignmentDetailPanel, detail);
        binding.assignmentBack.setEnabled(!busy);
        if (reviewDialog != null && (!available || !state.teacher() || selected == null)) { reviewDialog.dismiss(); reviewDialog = null; }
        if (publishing && !busy) {
            if (error.isEmpty() && state.draft("new.title").isEmpty()) state.draft("new.composing", "");
            publishing = false;
        }
        if (!available) {
            binding.assignmentsList.removeAllViews(); binding.submissionsList.removeAllViews(); listKey = ""; submissionKey = "";
        } else { renderList(); if (detail) renderDetail(selected); }
        ThemeColors.apply(binding.getRoot()); Motion.bindControls(binding.getRoot());
    }
    private void renderList() {
        boolean composing = "1".equals(state.draft("new.composing"));
        visible(binding.assignmentsCreateNew, state.teacher() && !composing);
        binding.assignmentsCreateNew.setEnabled(state.canCreate() && !uiBusy());
        visible(binding.assignmentCreatePanel, state.teacher() && composing);
        visible(binding.assignmentsReadOnly, "ENDED".equals(state.classStatus()) || "CANCELLED".equals(state.classStatus()));
        renderingEditors = true;
        text(binding.assignmentTitleInput, state.draft("new.title")); text(binding.assignmentDescriptionInput, state.draft("new.description"));
        renderingEditors = false;
        boolean edit = !uiBusy() && state.canCreate();
        binding.assignmentTitleInput.setEnabled(edit); binding.assignmentDescriptionInput.setEnabled(edit);
        binding.assignmentDueButton.setEnabled(edit); binding.assignmentPickFile.setEnabled(edit); binding.assignmentRemoveFile.setEnabled(edit);
        binding.assignmentCancelCreate.setEnabled(!uiBusy()); binding.assignmentPublish.setEnabled(edit);
        long due = dueMillis(); binding.assignmentDueButton.setText(due > 0 ? date(due) : getString(R.string.assignments_due_choose));
        renderFiles(); List<GroupAssignment> assignments = state.assignments();
        visible(binding.assignmentsEmpty, !state.loading() && assignments.isEmpty());
        StringBuilder signature = new StringBuilder().append(uiBusy());
        for (GroupAssignment a : assignments) signature.append('|').append(a.id).append(a.title).append(a.description).append(a.status).append(a.dueAtMillis).append(a.updatedAtMillis);
        if (signature.toString().equals(listKey)) return;
        listKey = signature.toString(); int scrollY = binding.assignmentsScroll.getScrollY();
        binding.assignmentsList.removeAllViews();
        for (GroupAssignment assignment : assignments) {
            ItemGroupAssignmentBinding row = ItemGroupAssignmentBinding.inflate(getLayoutInflater(), binding.assignmentsList, false);
            row.assignmentRowTitle.setText(assignment.title); row.assignmentRowStatus.setText(statusLabel(assignment));
            row.assignmentRowDue.setText(getString(R.string.assignments_due_value, date(assignment.dueAtMillis)));
            row.assignmentRowDescription.setText(assignment.description); row.assignmentOpen.setEnabled(!uiBusy());
            row.assignmentOpen.setOnClickListener(v -> {
                if (uiBusy()) return;
                hideKeyboard(); submissionKey = ""; state.select(assignment.id);
                if (binding != null) binding.assignmentsScroll.scrollTo(0, 0);
            });
            ThemeColors.apply(row.getRoot()); Motion.bindControls(row.getRoot()); binding.assignmentsList.addView(row.getRoot());
        }
        restoreScroll(scrollY);
    }
    private void renderDetail(GroupAssignment assignment) {
        binding.assignmentDetailTitle.setText(assignment.title);
        binding.assignmentDetailStatus.setText(statusLabel(assignment));
        binding.assignmentDetailDue.setText(getString(R.string.assignments_due_value, date(assignment.dueAtMillis)));
        binding.assignmentDetailDescription.setText(assignment.description);
        attachment(binding.assignmentOpenAttachment, assignment.attachment);
        visible(binding.assignmentToggleClosed, state.teacher());
        binding.assignmentToggleClosed.setText("CLOSED".equals(assignment.status) ? R.string.assignments_reopen_task : R.string.assignments_close_task);
        binding.assignmentToggleClosed.setEnabled(state.canCreate() && !uiBusy());
        visible(binding.submissionStudentPanel, !state.teacher());
        visible(binding.submissionsTeacherPanel, state.teacher());
        if (state.teacher()) renderSubmissions(); else renderAnswer(assignment);
    }
    private void renderAnswer(GroupAssignment assignment) {
        GroupAssignmentSubmission mine = state.mine();
        if (!state.submissionsLoading() && !"1".equals(state.draft("answer.initialized"))) {
            state.draft("answer.content", mine == null ? "" : mine.content);
            state.draft("answer.initialized", "1");
        }
        renderingEditors = true;
        text(binding.submissionContentInput, mine != null && mine.reviewed() ? mine.content : state.draft("answer.content"));
        renderingEditors = false;
        boolean reviewed = mine != null && "REVIEWED".equals(mine.status);
        binding.submissionOwnStatus.setText(mine == null ? R.string.assignments_not_submitted
                : reviewed ? R.string.assignments_reviewed : R.string.assignments_submitted);
        status(binding.submissionOwnTime, mine == null ? "" : getString(
                mine.updatedAtMillis > mine.submittedAtMillis ? R.string.assignments_submission_updated : R.string.assignments_submission_time,
                date(Math.max(mine.submittedAtMillis, mine.updatedAtMillis))));
        String lock = "";
        if (reviewed) lock = getString(R.string.assignments_reviewed_lock);
        else if ("ENDED".equals(state.classStatus()) || "CANCELLED".equals(state.classStatus())) lock = getString(R.string.assignments_read_only);
        else if ("CLOSED".equals(assignment.status) || assignment.dueAtMillis <= System.currentTimeMillis()) lock = getString(R.string.assignments_deadline_lock);
        status(binding.submissionLockReason, lock);
        boolean edit = state.canSubmit() && !state.submissionsLoading() && !uiBusy();
        binding.submissionContentInput.setEnabled(edit);
        binding.submissionPickFile.setEnabled(edit); binding.submissionRemoveFile.setEnabled(edit);
        binding.submissionSend.setEnabled(edit);
        binding.submissionSend.setText(mine == null ? R.string.assignments_submit : R.string.assignments_update_submission);
        attachment(binding.submissionOpenOwnAttachment, mine == null ? null : mine.attachment);
        visible(binding.submissionFeedbackPanel, reviewed);
        if (reviewed) {
            binding.submissionOwnScore.setText(mine.score == null ? getString(R.string.assignments_no_score)
                    : getString(R.string.assignments_score_value, score(mine.score)));
            binding.submissionOwnFeedback.setText(mine.feedback.isEmpty() ? getString(R.string.assignments_feedback_empty) : mine.feedback);
        }
        renderFiles();
    }
    private void renderSubmissions() {
        List<GroupAssignmentSubmission> submissions = state.submissions();
        binding.submissionsHeading.setText(getString(R.string.assignments_submissions_count, submissions.size()));
        visible(binding.submissionsProgress, state.submissionsLoading());
        status(binding.submissionsError, state.submissionsError());
        visible(binding.submissionsEmpty, !state.submissionsLoading() && submissions.isEmpty());
        StringBuilder signature = new StringBuilder(state.selectedId()).append(state.canReview()).append(uiBusy());
        for (GroupAssignmentSubmission s : submissions) signature.append('|').append(s.studentId).append(s.studentName)
                .append(s.content).append(s.link).append(s.status).append(s.feedback).append(s.score).append(s.updatedAtMillis)
                .append(s.reviewedAtMillis).append(s.attachment == null ? "" : s.attachment.path);
        if (signature.toString().equals(submissionKey)) return;
        submissionKey = signature.toString(); int scrollY = binding.assignmentsScroll.getScrollY();
        binding.submissionsList.removeAllViews();
        for (GroupAssignmentSubmission submission : submissions) {
            ItemGroupSubmissionBinding row = ItemGroupSubmissionBinding.inflate(getLayoutInflater(), binding.submissionsList, false);
            boolean reviewed = "REVIEWED".equals(submission.status);
            row.submissionRowName.setText(submission.studentName);
            row.submissionRowStatus.setText(reviewed ? R.string.assignments_reviewed : R.string.assignments_submitted);
            row.submissionRowTime.setText(getString(R.string.assignments_submission_updated,
                    date(Math.max(submission.submittedAtMillis, submission.updatedAtMillis))));
            row.submissionRowContent.setText(submission.content.isEmpty() ? getString(R.string.assignments_submission_empty_content) : submission.content);
            visible(row.submissionOpenLink, !submission.link.isEmpty());
            row.submissionOpenLink.setEnabled(!uiBusy());
            row.submissionOpenLink.setOnClickListener(v -> AssignmentAttachmentOpener.openLink(this, submission.link));
            attachment(row.submissionOpenAttachment, submission.attachment);
            status(row.submissionRowScore, submission.score == null ? "" : getString(R.string.assignments_score_value, score(submission.score)));
            status(row.submissionRowFeedback, submission.feedback);
            row.submissionReview.setEnabled(state.canReview() && !uiBusy());
            row.submissionReview.setText(reviewed ? R.string.assignments_edit_review : R.string.assignments_review);
            row.submissionReview.setOnClickListener(v -> review(submission));
            ThemeColors.apply(row.getRoot()); Motion.bindControls(row.getRoot()); binding.submissionsList.addView(row.getRoot());
        }
        restoreScroll(scrollY);
    }
    private void renderFiles() {
        if (binding == null) return;
        Uri newFile = uri(state.draft("new.file"));
        binding.assignmentFileName.setText(newFile == null ? getString(R.string.assignments_no_file) : filename(newFile));
        visible(binding.assignmentRemoveFile, newFile != null);
        GroupAssignmentSubmission mine = state.mine();
        boolean reviewed = mine != null && mine.reviewed();
        Uri answerFile = reviewed ? null : uri(state.draft("answer.file"));
        boolean removed = !reviewed && "1".equals(state.draft("answer.remove"));
        GroupAssignment.Attachment existing = mine == null ? null : mine.attachment;
        String label = answerFile != null ? filename(answerFile) : removed ? getString(R.string.assignments_attachment_removed)
                : existing != null ? getString(R.string.assignments_existing_file, existing.name) : getString(R.string.assignments_no_file);
        binding.submissionFileName.setText(label);
        visible(binding.submissionRemoveFile, answerFile != null || (!removed && existing != null));
    }
    private void attachment(com.google.android.material.button.MaterialButton button, GroupAssignment.Attachment attachment) {
        visible(button, attachment != null);
        button.setEnabled(!uiBusy());
        if (attachment != null) {
            button.setText(getString(R.string.assignments_open_file, attachment.name));
            button.setOnClickListener(v -> AssignmentAttachmentOpener.open(this, attachment));
        } else button.setOnClickListener(null);
    }
    private void publish() {
        if (!state.canCreate() || uiBusy()) return;
        String title = binding.assignmentTitleInput.getText().toString().trim();
        String description = binding.assignmentDescriptionInput.getText().toString().trim();
        if (title.isEmpty() || title.length() > 120) { invalid(binding.assignmentTitleInput, R.string.assignments_title_required); return; }
        if (description.isEmpty() || description.length() > 6000) { invalid(binding.assignmentDescriptionInput, R.string.assignments_description_required); return; }
        long due = dueMillis();
        if (due <= System.currentTimeMillis()) { status(binding.assignmentsError, getString(R.string.assignments_due_invalid)); return; }
        hideKeyboard(); Uri file = uri(state.draft("new.file"));
        java.util.function.Consumer<String> create = token -> {
            if (!state.canCreate()) return;
            publishing = true; state.create(title, description, due, file, token);
        };
        if (file == null) create.accept(""); else authorizeDrive(create);
    }
    private void submit() {
        if (!state.canSubmit() || state.submissionsLoading() || uiBusy()) return;
        String content = binding.submissionContentInput.getText().toString().trim();
        // Keep links from older submissions; new submissions use content and attachments.
        String link = state.mine() == null ? "" : state.mine().link;
        if (content.length() > 6000) { invalid(binding.submissionContentInput, R.string.assignments_content_too_long); return; }
        Uri file = uri(state.draft("answer.file"));
        boolean remove = "1".equals(state.draft("answer.remove"));
        boolean existing = state.mine() != null && state.mine().attachment != null && !remove;
        if (content.isEmpty() && file == null && !existing) {
            invalid(binding.submissionContentInput, R.string.assignments_answer_required); return;
        }
        hideKeyboard(); java.util.function.Consumer<String> submit = token -> {
            if (state.canSubmit()) state.submit(content, link, file, remove, token);
        };
        if (file == null) submit.accept(""); else authorizeDrive(submit);
    }
    private boolean uiBusy() { return state != null && (state.busy() || driveAuthorizing); }
    private void authorizeDrive(java.util.function.Consumer<String> action) {
        if (binding == null || uiBusy() || driveAuthorization.busy()) return;
        final View ownerView = binding.getRoot();
        final String actor = uid(), assignmentId = state.selectedId();
        driveAuthorizing = true; render();
        driveAuthorization.authorize(new AssignmentDriveAuthorization.Callback() {
            private boolean current() {
                return binding != null && binding.getRoot() == ownerView && actor.equals(uid())
                        && assignmentId.equals(state.selectedId()) && state.available();
            }
            public void authorized(String token) {
                if (binding == null || binding.getRoot() != ownerView) return;
                boolean allowed = current(); driveAuthorizing = false;
                if (allowed) action.accept(token);
                render();
            }
            public void failed(String message) {
                if (binding == null || binding.getRoot() != ownerView) return;
                driveAuthorizing = false; render();
                if (actor.equals(uid()) && message != null && !message.isEmpty()) status(binding.assignmentsError, message);
            }
        });
    }
    private void toggleClosed() {
        GroupAssignment assignment = state.selected(); if (assignment == null || !state.canCreate()) return;
        boolean close = !"CLOSED".equals(assignment.status);
        new AlertDialog.Builder(requireContext())
                .setTitle(close ? R.string.assignments_close_task : R.string.assignments_reopen_task)
                .setMessage(close ? R.string.assignments_close_confirm : R.string.assignments_reopen_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.assignments_confirm, (dialog, which) -> {
                    if (state.canCreate() && assignment.id.equals(state.selectedId())) state.setClosed(assignment.id, close);
                }).show();
    }
    private void chooseDue() {
        if (!state.canCreate()) return;
        Calendar chosen = Calendar.getInstance(); long due = dueMillis();
        if (due > 0) chosen.setTimeInMillis(due);
        DatePickerDialog dateDialog = new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            chosen.set(Calendar.YEAR, year); chosen.set(Calendar.MONTH, month); chosen.set(Calendar.DAY_OF_MONTH, day);
            new TimePickerDialog(requireContext(), (time, hour, minute) -> {
                chosen.set(Calendar.HOUR_OF_DAY, hour); chosen.set(Calendar.MINUTE, minute);
                chosen.set(Calendar.SECOND, 0); chosen.set(Calendar.MILLISECOND, 0);
                if (binding != null && state.canCreate()) { state.draft("new.due", Long.toString(chosen.getTimeInMillis())); render(); }
            }, chosen.get(Calendar.HOUR_OF_DAY), chosen.get(Calendar.MINUTE), true).show();
        }, chosen.get(Calendar.YEAR), chosen.get(Calendar.MONTH), chosen.get(Calendar.DAY_OF_MONTH));
        dateDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); dateDialog.show();
    }
    private void chooseFile(String target) {
        if (uiBusy() || ("new".equals(target) ? !state.canCreate() : !state.canSubmit())) return;
        fileTarget = target; fileAssignmentId = state.selectedId(); fileOwner = uid();
        filePicker.launch(FILE_TYPES);
    }
    private void fileSelected(Uri selectedUri) {
        if (selectedUri == null || binding == null || state == null || !state.available() || uiBusy()) return;
        if (!fileOwner.equals(uid()) || ("answer".equals(fileTarget) && !fileAssignmentId.equals(state.selectedId()))) return;
        try { requireContext().getContentResolver().takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (SecurityException ignored) { /* Providers may grant the read permission for this activity only. */ }
        if (!"new".equals(fileTarget) && !"answer".equals(fileTarget)) return;
        state.draft(fileTarget + ".file", selectedUri.toString());
        if ("answer".equals(fileTarget)) { state.draft("answer.remove", ""); state.draft("answer.initialized", "1"); }
        renderFiles();
    }
    private String filename(Uri value) {
        try (Cursor cursor = requireContext().getContentResolver().query(value,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0); if (name != null && !name.isEmpty()) return name;
            }
        } catch (RuntimeException ignored) { }
        return getString(R.string.assignments_file_unavailable);
    }
    private void review(GroupAssignmentSubmission submission) {
        if (!state.canReview() || state.selected() == null) return;
        final String assignmentId = state.selectedId();
        LinearLayout form = new LinearLayout(requireContext()); form.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        form.setPadding(padding, padding / 2, padding, padding / 2);
        EditText grade = new AppCompatEditText(requireContext()); grade.setId(R.id.assignment_review_score);
        grade.setHint(R.string.assignments_review_score_hint);
        grade.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        grade.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density)); grade.setSingleLine(true);
        if (submission.score != null) grade.setText(score(submission.score));
        EditText feedback = new AppCompatEditText(requireContext()); feedback.setId(R.id.assignment_review_feedback);
        feedback.setHint(R.string.assignments_review_feedback_hint);
        feedback.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        feedback.setMinLines(3); feedback.setMaxLines(6); feedback.setGravity(android.view.Gravity.TOP);
        feedback.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2000)});
        feedback.setText(submission.feedback);
        form.addView(grade, new LinearLayout.LayoutParams(-1, -2)); form.addView(feedback, new LinearLayout.LayoutParams(-1, -2));
        reviewDialog = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.assignments_review_title, submission.studentName)).setView(form)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.assignments_review_save, null).create();
        reviewDialog.setOnShowListener(dialog -> {
            ThemeColors.apply(form); Motion.bindControls(form);
            reviewDialog.getButton(AlertDialog.BUTTON_POSITIVE).setId(R.id.assignment_review_save);
            reviewDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (!state.canReview() || !assignmentId.equals(state.selectedId())) return;
                String number = grade.getText().toString().trim().replace(',', '.'); Double value = null;
                if (!number.isEmpty()) {
                    try { value = Double.parseDouble(number); }
                    catch (NumberFormatException invalidNumber) { invalid(grade, R.string.assignments_score_invalid); return; }
                    if (value.isNaN() || value.isInfinite() || value < 0 || value > 10) { invalid(grade, R.string.assignments_score_invalid); return; }
                }
                String message = feedback.getText().toString().trim();
                if (value == null && message.isEmpty()) { invalid(feedback, R.string.assignments_review_required); return; }
                if (message.length() > 2000) { invalid(feedback, R.string.assignments_feedback_too_long); return; }
                state.review(submission.studentId, value, message);
                if (reviewDialog != null) { reviewDialog.dismiss(); reviewDialog = null; }
            });
        });
        reviewDialog.show();
    }
    private String statusLabel(GroupAssignment assignment) {
        if ("CLOSED".equals(assignment.status)) return getString(R.string.assignments_closed);
        return getString(assignment.dueAtMillis <= System.currentTimeMillis() ? R.string.assignments_overdue : R.string.assignments_open);
    }
    private String uid() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? "" : user.getUid();
    }
    private long dueMillis() { try { return Long.parseLong(state.draft("new.due")); } catch (NumberFormatException invalidDate) { return 0; } }
    private static Uri uri(String value) { return value == null || value.isEmpty() ? null : Uri.parse(value); }
    private String date(long millis) { return millis <= 0 ? "—" : new SimpleDateFormat("HH:mm · dd/MM/yyyy", new Locale("vi", "VN")).format(new Date(millis)); }
    private String score(double value) { return new DecimalFormat("0.##").format(value); }
    private static void visible(View view, boolean show) { view.setVisibility(show ? View.VISIBLE : View.GONE); }
    private static void status(TextView view, String value) { view.setText(value); visible(view, value != null && !value.isEmpty()); }
    private static void text(EditText view, String value) {
        String text = value == null ? "" : value;
        if (!view.getText().toString().equals(text)) { view.setText(text); view.setSelection(text.length()); }
    }
    private void invalid(EditText view, int message) { view.setError(getString(message)); view.requestFocus(); }
    private void hideKeyboard() {
        if (binding == null) return;
        InputMethodManager manager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        binding.getRoot().clearFocus();
    }
    private void restoreScroll(int scrollY) {
        final DialogGroupAssignmentsBinding view = binding;
        view.assignmentsScroll.post(() -> { if (binding == view) view.assignmentsScroll.scrollTo(0, scrollY); });
    }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState); outState.putBoolean("publishing", publishing);
        outState.putString("file_target", fileTarget); outState.putString("file_assignment", fileAssignmentId); outState.putString("file_owner", fileOwner);
    }
    @Override public void onDestroyView() {
        driveAuthorization.cancel(); driveAuthorizing = false;
        if (reviewDialog != null) { reviewDialog.dismiss(); reviewDialog = null; }
        binding = null; listKey = ""; submissionKey = ""; super.onDestroyView();
    }
}
