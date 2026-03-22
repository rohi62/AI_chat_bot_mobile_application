package com.example.myf5ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

public class MainActivity extends AppCompatActivity {

    EditText promptInput;
    LinearLayout sendBtn;
    LinearLayout messagesContainer;
    NestedScrollView scrollView;

    GenerativeModelFutures model;
    Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        promptInput       = findViewById(R.id.promptInput);
        sendBtn           = findViewById(R.id.sendBtn);
        messagesContainer = findViewById(R.id.messagesContainer);
        scrollView        = findViewById(R.id.scrollView);

        promptInput.setHintTextColor(android.graphics.Color.parseColor("#93BDE4"));

        // Set hint color programmatically (XML hintTextColor not supported)
        promptInput.setHintTextColor(android.graphics.Color.parseColor("#93BDE4"));

        // Initialize Firebase Gemini Model
        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel("gemini-3-flash-preview");

        model = GenerativeModelFutures.from(ai);

        // Send button click
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String promptText = promptInput.getText().toString().trim();

                if (promptText.isEmpty()) {
                    return;
                }

                // Show user message bubble
                addUserMessage(promptText);
                promptInput.setText("");

                // Send to Gemini
                sendPrompt(promptText);
            }
        });
    }

    private void sendPrompt(String text) {
        // Show "Thinking..." AI bubble
        addAiMessage("Thinking...");

        Content prompt = new Content.Builder()
                .addText(text)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(prompt);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(@Nullable GenerateContentResponse result) {
                runOnUiThread(() -> {
                    // Replace the last "Thinking..." bubble with actual response
                    updateLastAiMessage(result.getText());
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    updateLastAiMessage("Error: " + t.getMessage());
                });
            }
        }, executor);
    }

    // Inflate and add a user message bubble
    private void addUserMessage(String text) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.item_message_user, messagesContainer, false);
        TextView msgText = view.findViewById(R.id.userMessageText);
        msgText.setText(text);
        messagesContainer.addView(view);
        scrollToBottom();
    }

    // Inflate and add an AI message bubble
    private void addAiMessage(String text) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.item_message_ai, messagesContainer, false);
        TextView msgText = view.findViewById(R.id.aiMessageText);
        msgText.setText(text);
        messagesContainer.addView(view);
        scrollToBottom();
    }

    // Update the text of the last AI bubble (e.g. replace "Thinking...")
    private void updateLastAiMessage(String text) {
        int childCount = messagesContainer.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = messagesContainer.getChildAt(i);
            TextView msgText = child.findViewById(R.id.aiMessageText);
            if (msgText != null) {
                msgText.setText(text);
                scrollToBottom();
                break;
            }
        }
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}