package com.aurasoft.booky;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class NameEntryActivity extends AppCompatActivity {

    private EditText etName;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private AlertDialog loadingDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_name_entry);

        initLoadingDialog();


//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        etName = findViewById(R.id.etName);

        findViewById(R.id.btnFinish).setOnClickListener(v -> {
            saveUserToFirestore();
            loadingDialog.show();

        });

    }

    private void saveUserToFirestore() {
        String name = etName.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("කරුණාකර ඔබේ නම ඇතුළත් කරන්න");
            return;
        }

        // දැනට Login වී සිටින පරිශීලකයාගේ UID එක සහ Phone Number එක ගැනීම
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            String phone = mAuth.getCurrentUser().getPhoneNumber();


            Map<String, Object> user = new HashMap<>();
            user.put("name", name);
            user.put("phone", phone);
            user.put("uid", uid);
            user.put("joinedAt", System.currentTimeMillis());


            db.collection("Users").document(uid)
                    .set(user)
                    .addOnSuccessListener(aVoid -> {
                        loadingDialog.dismiss();

                        Toast.makeText(NameEntryActivity.this, "ලියාපදිංචිය සාර්ථකයි!", Toast.LENGTH_SHORT).show();


                        Intent intent = new Intent(NameEntryActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        loadingDialog.dismiss();

                        Toast.makeText(NameEntryActivity.this, "දෝෂයක් ඇතිවිය: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void initLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // ඔබේ dialog_loading.xml එක inflate කරනවා
        View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(view);
        builder.setCancelable(false);

        loadingDialog = builder.create();

        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
//            loadingDialog.getWindow().setGravity(Gravity.BOTTOM); // යටටම ගැනීමට

            WindowManager.LayoutParams layoutParams = loadingDialog.getWindow().getAttributes();
            layoutParams.y = 50;
            loadingDialog.getWindow().setAttributes(layoutParams);
        }
    }
}