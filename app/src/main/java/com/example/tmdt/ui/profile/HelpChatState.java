package com.example.tmdt.ui.profile;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shared by the floating popup and Help Center; requests survive view recreation. */
public final class HelpChatState extends ViewModel {
    static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_TURNS = 30;
    private static final int MAX_ANSWER_LENGTH = 5000;
    private static final long TIMEOUT_MS = 45000L;
    enum Kind { PENDING, AI, FAQ, FALLBACK }

    static final class Turn {
        final long id;
        final String question;
        final String answer;
        final String notice;
        final Kind kind;

        Turn(long id, String question, String answer, String notice, Kind kind) {
            this.id = id;
            this.question = question;
            this.answer = answer;
            this.notice = notice;
            this.kind = kind;
        }
    }

    private final SavedStateHandle saved;
    private final ArrayList<Turn> turns = new ArrayList<>();
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HelpAiClient client = new HelpAiClient();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseAuth.AuthStateListener authListener = ignored -> checkAccount();
    private ListenableFuture<GenerateContentResponse> pending;
    private Runnable timeout;
    private String account;
    private long nextId = 1;
    private long pendingId = -1;
    private int generation;
    private boolean cleared;

    public HelpChatState(SavedStateHandle saved) {
        this.saved = saved;
        account = currentAccount();
        if (account.equals(saved.get("help_account"))) {
            ArrayList<Bundle> restored = saved.get("help_turns");
            if (restored != null) {
                int first = Math.max(0, restored.size() - MAX_TURNS);
                for (int i = first; i < restored.size(); i++) {
                    Bundle item = restored.get(i);
                    Kind kind;
                    try { kind = Kind.valueOf(item.getString("kind", "FAQ")); }
                    catch (IllegalArgumentException error) { kind = Kind.FAQ; }
                    String question = limit(item.getString("question", ""), MAX_QUESTION_LENGTH);
                    if (question.isEmpty()) continue;
                    String answer = limit(item.getString("answer", ""), MAX_ANSWER_LENGTH);
                    String notice = item.getString("notice", "");
                    if (kind == Kind.PENDING) {
                        kind = Kind.FALLBACK;
                        answer = HelpFaq.answer(question);
                        notice = "Câu hỏi trước chưa trả lời xong. Bạn có thể thử lại.";
                    }
                    long id = item.getLong("id", nextId);
                    turns.add(new Turn(id, question, answer, notice, kind));
                    nextId = Math.max(nextId, id + 1);
                }
            }
        } else {
            saved.set("help_draft", "");
        }
        persist();
        auth.addAuthStateListener(authListener);
    }

    LiveData<Integer> changes() { return changes; }
    List<Turn> turns() { return Collections.unmodifiableList(turns); }
    boolean busy() { return pendingId != -1; }
    String draft() { return saved.get("help_draft") == null ? "" : saved.get("help_draft"); }
    void draft(String value) { saved.set("help_draft", limit(value, MAX_QUESTION_LENGTH)); }

    boolean submit(String text, boolean useFaq) {
        checkAccount();
        String question = limit(text.trim(), MAX_QUESTION_LENGTH);
        if (cleared || busy() || question.isEmpty()) return false;
        if (turns.size() == MAX_TURNS) turns.remove(0);
        long id = nextId++;
        turns.add(new Turn(id, question, useFaq ? HelpFaq.answer(question) : "", "",
                useFaq ? Kind.FAQ : Kind.PENDING));
        if (useFaq) publish();
        else request(turns.size() - 1);
        return true;
    }

    void retry(long id) {
        if (busy() || cleared) return;
        checkAccount();
        for (int i = 0; i < turns.size(); i++) {
            Turn turn = turns.get(i);
            if (turn.id != id || turn.kind != Kind.FALLBACK) continue;
            turns.set(i, new Turn(id, turn.question, "", "", Kind.PENDING));
            request(i);
            return;
        }
    }

    private void request(int index) {
        Turn turn = turns.get(index);
        pendingId = turn.id;
        int ticket = ++generation;
        List<Turn> history = new ArrayList<>(turns.subList(0, index));
        publish();
        try {
            ListenableFuture<GenerateContentResponse> response = client.ask(turn.question, history);
            pending = response;
            timeout = () -> {
                if (!current(ticket, turn.id)) return;
                complete(turn.id, HelpFaq.answer(turn.question),
                        "Trợ lý phản hồi chậm. Bạn có thể xem hướng dẫn có sẵn hoặc thử lại.", Kind.FALLBACK);
                response.cancel(true);
            };
            main.postDelayed(timeout, TIMEOUT_MS);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override public void onSuccess(GenerateContentResponse result) {
                    if (!current(ticket, turn.id)) return;
                    String answer = result == null ? "" : result.getText();
                    if (answer == null || answer.trim().isEmpty()) {
                        complete(turn.id, HelpFaq.answer(turn.question),
                                "Trợ lý chưa có câu trả lời. Bạn có thể xem hướng dẫn có sẵn hoặc thử lại.", Kind.FALLBACK);
                    } else {
                        complete(turn.id, limit(answer.trim(), MAX_ANSWER_LENGTH), "", Kind.AI);
                    }
                }

                @Override public void onFailure(@NonNull Throwable error) {
                    if (current(ticket, turn.id)) {
                        complete(turn.id, HelpFaq.answer(turn.question), HelpAiClient.failureMessage(error), Kind.FALLBACK);
                    }
                }
            }, command -> main.post(command));
        } catch (RuntimeException error) {
            complete(turn.id, HelpFaq.answer(turn.question), HelpAiClient.failureMessage(error), Kind.FALLBACK);
        }
    }

    private boolean current(int ticket, long id) {
        return !cleared && generation == ticket && pendingId == id && account.equals(currentAccount());
    }

    private void complete(long id, String answer, String notice, Kind kind) {
        if (timeout != null) main.removeCallbacks(timeout);
        timeout = null;
        pending = null;
        pendingId = -1;
        for (int i = 0; i < turns.size(); i++) {
            Turn turn = turns.get(i);
            if (turn.id == id) {
                turns.set(i, new Turn(id, turn.question, answer, notice, kind));
                break;
            }
        }
        publish();
    }

    private void checkAccount() {
        String nextAccount = currentAccount();
        if (account.equals(nextAccount)) return;
        cancelRequest();
        account = nextAccount;
        turns.clear();
        saved.set("help_draft", "");
        publish();
    }

    private String currentAccount() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? "guest" : user.getUid();
    }

    private void cancelRequest() {
        generation++;
        if (timeout != null) main.removeCallbacks(timeout);
        timeout = null;
        pendingId = -1;
        if (pending != null) pending.cancel(true);
        pending = null;
    }

    private void persist() {
        ArrayList<Bundle> history = new ArrayList<>();
        for (Turn turn : turns) {
            Bundle item = new Bundle();
            item.putLong("id", turn.id);
            item.putString("question", turn.question);
            item.putString("answer", turn.answer);
            item.putString("notice", turn.notice);
            item.putString("kind", turn.kind.name());
            history.add(item);
        }
        saved.set("help_account", account);
        saved.set("help_turns", history);
    }

    private void publish() {
        persist();
        changes.setValue(changes.getValue() + 1);
    }

    private static String limit(String value, int maximum) {
        return value.length() > maximum ? value.substring(0, maximum) : value;
    }

    @Override protected void onCleared() {
        cleared = true;
        auth.removeAuthStateListener(authListener);
        cancelRequest();
    }
}
