package com.aurasoft.booky.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurasoft.booky.R;
import com.aurasoft.booky.adpter.NotificationAdapter;
import com.aurasoft.booky.model.NotificationModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView tvEmpty, btnClearAll;
    private AlertDialog loadingDialog;
    private boolean isFirstLoad = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupLoadingDialog();

        recyclerView = view.findViewById(R.id.rvNotifications);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        btnClearAll = view.findViewById(R.id.btnClearAll);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        loadNotifications();

        // මෙතන තමයි අර Custom Confirmation Dialog එක එන්නේ
        btnClearAll.setOnClickListener(v -> showCustomClearDialog());

        return view;
    }

    // --- පටන් ගැන්ම: Custom Dialog Logic ---
    private void showCustomClearDialog() {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_clear_confirm); // උඹ හදපු XML එක

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnYes = dialog.findViewById(R.id.btnYes);
        Button btnNo = dialog.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(v -> {
            clearAllNotifications(); // Firestore එකෙන් මකන logic එක
            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    // --- අවසානය: Custom Dialog Logic ---

    private void setupLoadingDialog() {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        loadingDialog = builder.create();

        if (loadingDialog.getWindow() != null) {
            Window window = loadingDialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.y = 80;
            window.setAttributes(params);
        }
    }

    private void loadNotifications() {
        String currentUserId = mAuth.getUid();
        if (currentUserId == null) return;

        if (isFirstLoad && loadingDialog != null) {
            loadingDialog.show();
        }

        db.collection("Notifications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    isFirstLoad = false;

                    if (error != null) {
                        Log.e("NOTI_ERROR", "Error: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        notificationList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            NotificationModel model = doc.toObject(NotificationModel.class);
                            if (model != null) {
                                notificationList.add(model);
                            }
                        }

                        adapter.notifyDataSetChanged();
                        updateUI();

                        if (!value.isEmpty()) {
                            markAllAsRead(value.getDocuments());
                        }
                    }
                });
    }

    private void markAllAsRead(List<DocumentSnapshot> documents) {
        WriteBatch batch = db.batch();
        boolean hasUpdates = false;

        for (DocumentSnapshot doc : documents) {
            NotificationModel model = doc.toObject(NotificationModel.class);
            if (model != null && !model.isRead()) {
                batch.update(doc.getReference(), "isRead", true);
                hasUpdates = true;
            }
        }

        if (hasUpdates) {
            batch.commit().addOnFailureListener(e -> Log.e("NOTI_ERROR", "Failed to update read status"));
        }
    }

    private void updateUI() {
        if (notificationList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            btnClearAll.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            btnClearAll.setVisibility(View.VISIBLE);
        }
    }

    private void clearAllNotifications() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        if (loadingDialog != null) loadingDialog.show();

        db.collection("Notifications")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit().addOnCompleteListener(task -> {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "All notifications cleared", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to clear notifications", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    if (loadingDialog != null) loadingDialog.dismiss();
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}