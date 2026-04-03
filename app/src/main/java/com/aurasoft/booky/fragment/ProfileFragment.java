package com.aurasoft.booky.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.aurasoft.booky.R;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileFragment extends Fragment {

    private CircleImageView profileImage;
    private TextView tvName, tvEmail, tvPhone;
    private View rowName, rowEmail, rowPhone;
    private ImageView btnBack;

    private FloatingActionButton btnEditPhoto;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri imageUri;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isGoogleUser = false;



    private android.app.AlertDialog loadingDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        profileImage = view.findViewById(R.id.profileImage);
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        btnBack = view.findViewById(R.id.btnBack);

        btnEditPhoto = view.findViewById(R.id.btnEditPhoto);

        rowName = view.findViewById(R.id.rowName);
        rowEmail = view.findViewById(R.id.rowEmail);
        rowPhone = view.findViewById(R.id.rowPhone);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        profileImage.setImageURI(imageUri);
                        uploadImageToImgBB(imageUri);
                    }
                }
        );

        checkLoginProvider();
        loadUserData(view);

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        btnEditPhoto.setOnClickListener(v -> {
            if (isGoogleUser) {
                Toast.makeText(getContext(), "Google profile photo cannot be changed", Toast.LENGTH_SHORT).show();
            } else {
                openGallery();
            }
        });

        rowName.setOnClickListener(v -> {
            if (isGoogleUser) {
                Toast.makeText(getContext(), "Google account details cannot be changed", Toast.LENGTH_SHORT).show();
            } else {
                showEditDialog("Name", tvName.getText().toString(), "name");
            }
        });

        rowEmail.setOnClickListener(v -> {
            if (isGoogleUser) {
                Toast.makeText(getContext(), "Google account details cannot be changed", Toast.LENGTH_SHORT).show();
            } else {
                showEditDialog("Email", tvEmail.getText().toString(), "email");
            }
        });

        rowPhone.setOnClickListener(v ->
                Toast.makeText(getContext(), "Phone number cannot be changed", Toast.LENGTH_SHORT).show()
        );

        View loadingView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        loadingDialog = new android.app.AlertDialog.Builder(requireContext())
                .setView(loadingView)
                .setCancelable(false)
                .create();
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

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

        if (isGoogleUser) {
            View editName = view.findViewById(R.id.editIconName);
            View editEmail = view.findViewById(R.id.editIconEmail);
            if (editName != null) editName.setVisibility(View.GONE);
            if (editEmail != null) editEmail.setVisibility(View.GONE);
        }

        db.collection("Users").document(uid).addSnapshotListener((documentSnapshot, error) -> {
            if (error != null || !isAdded()) return;

            if (documentSnapshot != null && documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String email = documentSnapshot.getString("email");
                String phone = documentSnapshot.getString("phone");
                String imageUrl = documentSnapshot.getString("profileImageUrl");

                tvName.setText(name != null && !name.isEmpty() ? name : "Enter Name");
                tvEmail.setText(email != null && !email.isEmpty() ? email : "Enter Email");
                tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Not Provided");

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

    // --- ImgBB Image Upload Methods ---

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToImgBB(Uri uri) {
        if (loadingDialog != null) loadingDialog.show();

        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            byte[] bytes = getBytes(inputStream);

            String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);

            OkHttpClient client = new OkHttpClient();
            String apiKey = "8625ab9894cfa71abff199fea140673d";

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", base64Image)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload?key=" + apiKey)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("UPLOAD_ERROR", "Network Failure: " + e.getMessage());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (loadingDialog != null) loadingDialog.dismiss();
                            Toast.makeText(getContext(), "Network Error!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            String uploadedUrl = jsonResponse.getJSONObject("data").getString("url");

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> updateProfileImageUrl(uploadedUrl));
                            }
                        } catch (Exception e) {
                            Log.e("UPLOAD_ERROR", "JSON Error: " + e.getMessage());
                            if (getActivity() != null) getActivity().runOnUiThread(() -> { if (loadingDialog != null) loadingDialog.dismiss(); });
                        }
                    } else {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> { if (loadingDialog != null) loadingDialog.dismiss(); });
                    }
                }
            });
        } catch (Exception e) {
            if (loadingDialog != null) loadingDialog.dismiss();
        }
    }

    private void updateProfileImageUrl(String url) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("Users").document(uid)
                .update("profileImageUrl", url)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        if (loadingDialog != null) loadingDialog.dismiss();
                        Toast.makeText(getContext(), "Firestore update failed!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}