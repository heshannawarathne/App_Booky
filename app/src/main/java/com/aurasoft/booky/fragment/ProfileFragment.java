package com.aurasoft.booky.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import androidx.lifecycle.ViewModelProvider;

import com.aurasoft.booky.R;
import com.aurasoft.booky.viewmodel.ProfileViewModel;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.*;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private CircleImageView profileImage;
    private TextView tvName, tvEmail, tvPhone;
    private View rowName, rowEmail, rowPhone;
    private ImageView btnBack;
    private FloatingActionButton btnEditPhoto;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private FirebaseAuth mAuth;
    private ProfileViewModel viewModel; // Variable name එක මෙතන තියෙන්න ඕනේ
    private boolean isGoogleUser = false;
    private android.app.AlertDialog loadingDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();

        // ViewModel එක නිවැරදිව initialize කිරීම
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        initViews(view);
        setupLoadingDialog();
        checkLoginProvider();

        // Data වෙනස් වන විට UI එක ඉබේම update වීමට observer එකක් භාවිතා කරයි
        if (mAuth.getUid() != null) {
            viewModel.getUserData(mAuth.getUid()).observe(getViewLifecycleOwner(), data -> {
                if (data != null) {
                    updateUI(data);
                }
            });
        }

        setupClickListeners();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        uploadImageToImgBB(result.getData().getData());
                    }
                }
        );

        return view;
    }

    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        btnBack = view.findViewById(R.id.btnBack);
        btnEditPhoto = view.findViewById(R.id.btnEditPhoto);
        rowName = view.findViewById(R.id.rowName);
        rowEmail = view.findViewById(R.id.rowEmail);
        rowPhone = view.findViewById(R.id.rowPhone);
    }

    private void updateUI(Map<String, Object> data) {
        tvName.setText(String.valueOf(data.getOrDefault("name", "Enter Name")));
        tvEmail.setText(String.valueOf(data.getOrDefault("email", "Enter Email")));
        tvPhone.setText(String.valueOf(data.getOrDefault("phone", "Not Provided")));

        String imageUrl = (String) data.get("profileImageUrl");
        if (imageUrl != null && !imageUrl.isEmpty() && isAdded()) {
            Glide.with(this).load(imageUrl).placeholder(R.drawable.img_15).into(profileImage);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        btnEditPhoto.setOnClickListener(v -> {
            if (isGoogleUser) Toast.makeText(getContext(), "Google photo cannot be changed", Toast.LENGTH_SHORT).show();
            else openGallery();
        });

        rowName.setOnClickListener(v -> {
            if (!isGoogleUser) showEditDialog("Name", tvName.getText().toString(), "name");
        });

        rowEmail.setOnClickListener(v -> {
            if (!isGoogleUser) showEditDialog("Email", tvEmail.getText().toString(), "email");
        });
    }

    private void checkLoginProvider() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                if (profile.getProviderId().equals("google.com")) isGoogleUser = true;
            }
        }
    }

    private void showEditDialog(String title, String currentValue, String fieldKey) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_field, null);
        dialog.setContentView(dialogView);

        EditText etValue = dialogView.findViewById(R.id.dialogEditText);
        AppCompatButton btnSave = dialogView.findViewById(R.id.btnDialogSave);
        ((TextView)dialogView.findViewById(R.id.dialogTitle)).setText("Edit " + title);

        etValue.setText(currentValue);
        btnSave.setOnClickListener(v -> {
            String newValue = etValue.getText().toString().trim();
            if (!newValue.isEmpty()) {
                viewModel.updateField(mAuth.getUid(), fieldKey, newValue);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToImgBB(Uri uri) {
        loadingDialog.show();
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            byte[] bytes = getBytes(inputStream);
            String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);

            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", base64Image)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload?key=8625ab9894cfa71abff199fea140673d")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        loadingDialog.dismiss();
                        Toast.makeText(getContext(), "Upload Failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String url = new JSONObject(response.body().string()).getJSONObject("data").getString("url");
                            viewModel.updateField(mAuth.getUid(), "profileImageUrl", url);
                            requireActivity().runOnUiThread(() -> loadingDialog.dismiss());
                        } catch (Exception e) { Log.e("JSON_ERR", e.getMessage()); }
                    }
                }
            });
        } catch (Exception e) { loadingDialog.dismiss(); }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) byteBuffer.write(buffer, 0, len);
        return byteBuffer.toByteArray();
    }

    private void setupLoadingDialog() {
        View loadingView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        loadingDialog = new android.app.AlertDialog.Builder(requireContext()).setView(loadingView).setCancelable(false).create();
        if (loadingDialog.getWindow() != null) loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}