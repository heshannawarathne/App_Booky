package com.aurasoft.booky.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.aurasoft.booky.R;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private CircleImageView profileImage;
    private TextView tvName, tvEmail, tvPhone;
    private View rowName, rowEmail, rowPhone;
    private ImageView btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isGoogleUser = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI
        profileImage = view.findViewById(R.id.profileImage);
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        btnBack = view.findViewById(R.id.btnBack);

        rowName = view.findViewById(R.id.rowName);
        rowEmail = view.findViewById(R.id.rowEmail);
        rowPhone = view.findViewById(R.id.rowPhone);

        // ලොග් වෙලා ඉන්න ක්‍රමය චෙක් කරමු
        checkLoginProvider();

        // දත්ත ලෝඩ් කරමු (UI එකත් එක්කම)
        loadUserData(view);

        // Back Button
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Name Row Click
        rowName.setOnClickListener(v -> {
            if (isGoogleUser) {
                Toast.makeText(getContext(), "Google account details cannot be changed", Toast.LENGTH_SHORT).show();
            } else {
                showEditDialog("Name", tvName.getText().toString(), "name");
            }
        });

        // Email Row Click
        rowEmail.setOnClickListener(v -> {
            if (isGoogleUser) {
                Toast.makeText(getContext(), "Google account details cannot be changed", Toast.LENGTH_SHORT).show();
            } else {
                showEditDialog("Email", tvEmail.getText().toString(), "email");
            }
        });

        // Phone Row Click (දෙගොල්ලන්ටම Edit කරන්න බෑ)
        rowPhone.setOnClickListener(v ->
                Toast.makeText(getContext(), "Phone number cannot be changed", Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    private void checkLoginProvider() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                if (profile.getProviderId().equals("google.com")) {
                    isGoogleUser = true;
                    break;
                }
            }
        }
    }

    private void loadUserData(View view) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        // Google User නම් Edit Icons හංගමු
        if (isGoogleUser) {
            View editName = view.findViewById(R.id.editIconName);
            View editEmail = view.findViewById(R.id.editIconEmail);
            if (editName != null) editName.setVisibility(View.GONE);
            if (editEmail != null) editEmail.setVisibility(View.GONE);
        }

        // Firestore එකෙන් Real-time දත්ත ගනිමු
        db.collection("Users").document(uid).addSnapshotListener((documentSnapshot, error) -> {
            if (error != null || !isAdded()) return;

            if (documentSnapshot != null && documentSnapshot.exists()) {
                // දත්ත null ද කියා චෙක් කර ලෝඩ් කිරීම (Crash නොවෙන්න ප්‍රධානම හේතුව මේකයි)
                String name = documentSnapshot.getString("name");
                String email = documentSnapshot.getString("email");
                String phone = documentSnapshot.getString("phone");
                String imageUrl = documentSnapshot.getString("profileImageUrl");

                tvName.setText(name != null && !name.isEmpty() ? name : "Enter Name");
                tvEmail.setText(email != null && !email.isEmpty() ? email : "Enter Email");
                tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Not Provided");

                // පින්තූරය තිබේ නම් පමණක් පෙන්වන්න
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.img_15).into(profileImage);
                }
            }
        });
    }

    private void showEditDialog(String title, String currentValue, String fieldKey) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_field, null);
        dialog.setContentView(dialogView);

        TextView tvDialogTitle = dialogView.findViewById(R.id.dialogTitle);
        EditText etValue = dialogView.findViewById(R.id.dialogEditText);
        AppCompatButton btnSave = dialogView.findViewById(R.id.btnDialogSave);

        tvDialogTitle.setText("Edit " + title);

        // දැනට තියෙන value එක placeholder එකක් නම් ඒක EditText එකට දාන්න එපා
        if (!currentValue.equals("Enter Name") && !currentValue.equals("Enter Email")) {
            etValue.setText(currentValue);
        }

        etValue.setSelection(etValue.getText().length());

        btnSave.setOnClickListener(v -> {
            String newValue = etValue.getText().toString().trim();
            if (!newValue.isEmpty()) {
                updateSingleField(fieldKey, newValue);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Please enter a value", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void updateSingleField(String key, String value) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("Users").document(uid)
                .update(key, value)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed!", Toast.LENGTH_SHORT).show());
    }
}