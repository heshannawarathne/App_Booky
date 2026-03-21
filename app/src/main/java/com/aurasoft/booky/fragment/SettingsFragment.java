package com.aurasoft.booky.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.aurasoft.booky.LoginActivity;
import com.aurasoft.booky.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.WindowManager;
public class SettingsFragment extends Fragment {

    private SwitchMaterial switchAutoBrightness, switchNotifications, switchCallAccess, switchLocation, switchDarkMode;
    private TextView tvPrivacyPolicy, tvTerms, tvLegalContent, btnLogout;
    private View cvProfile, cvDeveloper;
    private ImageView btnBack;
    private LinearLayout settingsMenuLayout;

    private static final String PREFS_NAME = "SettingsPrefs";

    public SettingsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // --- 1. Initialize Views ---
        settingsMenuLayout = view.findViewById(R.id.settingsMenuLayout);
        tvLegalContent = view.findViewById(R.id.tvLegalContent);
        tvPrivacyPolicy = view.findViewById(R.id.tvPrivacyPolicy);
        tvTerms = view.findViewById(R.id.tvTerms);
        cvProfile = view.findViewById(R.id.cvProfile);
        cvDeveloper = view.findViewById(R.id.cvDeveloper);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnBack = view.findViewById(R.id.btnBack);

        btnBack.setVisibility(View.GONE);

        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchCallAccess = view.findViewById(R.id.switchCallAccess);
        switchLocation = view.findViewById(R.id.switchLocation);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);

        // --- 2. Logout Logic with Confirmation ---
        if (btnLogout != null) {
            btnLogout.setPaintFlags(btnLogout.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            btnLogout.setOnClickListener(v -> {
                showLogoutConfirmation(); // මෙතනින් Dialog එකට යනවා
            });
        }

        // --- 3. Load Saved Settings ---
        loadSettings();

        // --- 4. Click Listeners ---
        tvPrivacyPolicy.setOnClickListener(v -> showLegalContent(getPrivacyPolicyText()));
        tvTerms.setOnClickListener(v -> showLegalContent(getTermsOfServiceText()));

        if (cvDeveloper != null) {
            cvDeveloper.setOnClickListener(v -> showLegalContent(getDeveloperInfoText()));
        }

        if (cvProfile != null) {
            cvProfile.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Back Button Logic
        btnBack.setOnClickListener(v -> {
            if (tvLegalContent.getVisibility() == View.VISIBLE) {
                tvLegalContent.setVisibility(View.GONE);
                settingsMenuLayout.setVisibility(View.VISIBLE);
                btnBack.setVisibility(View.GONE);
            }
        });

        // --- 5. Switch Listeners (Permissions & Settings) ---
        // Notification Switch
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // මේ Check එක ඉතා වැදගත්! Listener එක trigger වුණේ ඇඟිල්ලෙන් එබුවම විතරද බලනවා
            if (buttonView.isPressed()) {
                saveSetting("notifications", isChecked);
                Toast.makeText(getContext(), isChecked ? "Notifications Enabled" : "Notifications Disabled", Toast.LENGTH_SHORT).show();
            }
        });

// Call Access Switch
        switchCallAccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                if (isChecked) {
                    // Active කරන්න හදද්දී Permission නැත්නම් ඉල්ලනවා
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        // තාවකාලිකව Off කරමු Permission ලැබෙනකම්
                        switchCallAccess.setChecked(false);
                        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 101);
                    } else {
                        saveSetting("call_access", true);
                    }
                } else {
                    // Deactive කරද්දී කෙලින්ම False විදිහට Save කරනවා
                    saveSetting("call_access", false);
                    Toast.makeText(getContext(), "Call Access Deactivated in App", Toast.LENGTH_SHORT).show();
                }
            }
        });

// Location Switch
        switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                if (isChecked) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        switchLocation.setChecked(false);
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 102);
                    } else {
                        saveSetting("location_access", true);
                    }
                } else {
                    saveSetting("location_access", false);
                    Toast.makeText(getContext(), "Location Access Deactivated in App", Toast.LENGTH_SHORT).show();
                }
            }
        });

// Dark Mode Switch
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                saveSetting("dark_mode", isChecked);
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });

        switchAutoBrightness = view.findViewById(R.id.switchAutoBrightness);

// Auto Brightness Switch එකේ Listener එක
        switchAutoBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // යූසර් Click කරොත් විතරක් වැඩ කරන්න
                saveSetting("auto_brightness", isChecked);

                if (isChecked) {
                    Toast.makeText(getContext(), "Auto Brightness Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    // Disable කරපු ගමන් Brightness එක සාමාන්‍ය තත්වයට පත් කරනවා
                    if (getActivity() != null) {
                        android.view.WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
                        lp.screenBrightness = -1f; // -1f කියන්නේ System Default අගය
                        getActivity().getWindow().setAttributes(lp);
                    }
                    Toast.makeText(getContext(), "Auto Brightness Disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    // onCreateView එකෙන් එළියට මේක දාන්න අමතක කරන්න එපා!
    private void showLogoutConfirmation() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout_confirm, null);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView btnConfirm = dialogView.findViewById(R.id.btnConfirmLogout);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }


    private String getPrivacyPolicyText() {
        return "<b>Last Updated: March 2026</b><br><br>" +
                "Welcome to <b>Booky</b>! We value your privacy and are committed to protecting your personal data.<br><br>" +
                "<b>1. Information We Collect</b><br>" +
                "• Profile Information: When you log in via Google, we collect your name, email, and profile picture.<br>" +
                "• Contact Information: For mobile logins, we collect your phone number to verify your account.<br>" +
                "• Booking Data: We store information about your bus seat bookings to provide you with tickets and history.<br><br>" +
                "<b>2. How We Use Your Data</b><br>" +
                "• To provide and maintain our service.<br>" +
                "• To notify you about changes to your bookings.<br>" +
                "• To provide customer support.<br><br>" +
                "<b>3. Data Storage</b><br>" +
                "Your data is securely stored in <b>Firebase (Google Cloud)</b>. We do not sell your personal information to third parties.<br><br>" +
                "<b>4. Security</b><br>" +
                "The security of your data is important to us, but remember that no method of transmission over the Internet is 100% secure.<br><br>" +
                "<b>5. Contact Us</b><br>" +
                "If you have any questions about this Privacy Policy, please contact us at <u>support@aurasoft.com</u>.";
    }

    private String getTermsOfServiceText() {
        return "By using the <b>Booky</b> mobile application, you agree to the following terms and conditions:<br><br>" +
                "<b>1. Acceptance of Terms</b><br>" +
                "By accessing this app, you acknowledge that you have read, understood, and agreed to be bound by these terms.<br><br>" +
                "<b>2. Booking and Payments</b><br>" +
                "• All bookings made through the app are subject to seat availability.<br>" +
                "• Ticket prices are determined by the bus operators and are subject to change.<br>" +
                "• Users must provide accurate information when booking.<br><br>" +
                "<b>3. Cancellation and Refunds</b><br>" +
                "• Cancellation policies vary by bus operator. Please check before booking.<br>" +
                "• Refund processing may take 3-5 working days depending on your payment method.<br><br>" +
                "<b>4. User Responsibilities</b><br>" +
                "• You are responsible for maintaining the confidentiality of your account.<br>" +
                "• You agree not to use the app for any fraudulent or illegal activities.<br><br>" +
                "<b>5. Limitation of Liability</b><br>" +
                "Booky is a platform to connect passengers with bus services. We are not responsible for bus delays, breakdowns, or service issues caused by the transport operators.";
    }

    private String getDeveloperInfoText() {
        return "About the Developer<br><br>" +
                "Hi, I'm Heshan Nawarathna, the developer behind Booky.<br><br>" +
                "Booky was created with the mission to make bus travel in Sri Lanka more convenient and accessible for everyone.<br><br>" +
                "<b>Developer:</b> Heshan Nawarathna<br>" +
                "<b>Company:</b> AuraSoft Systems<br>" +
                "<b>Website:</b> <u>www.aurasoft.com</u><br>" +
                "<b>Contact:</b> <u>support@aurasoft.com</u><br><br>" +
                "Thank you for supporting local independent developers!";
    }

    private void showLegalContent(String content) {
        settingsMenuLayout.setVisibility(View.GONE);
        tvLegalContent.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvLegalContent.setText(android.text.Html.fromHtml(content, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvLegalContent.setText(android.text.Html.fromHtml(content));
        }

        // *** මෙන්න මේ පේළිය අලුතින් එකතු කරන්න ***
        // මේකෙන් 'setting_cart_bg' වල තියෙන පාට ඉබේම අකුරු වලට වැටෙනවා
        tvLegalContent.setTextColor(ContextCompat.getColor(requireContext(), R.color.white_02));

        tvLegalContent.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    private void saveSetting(String key, boolean value) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void loadSettings() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 1. සිස්ටම් එකේ ඇත්තටම Permission තියෙනවද බලනවා (Null Safety සඳහා)
        if (getContext() == null) return;
        boolean hasCallPerm = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
        boolean hasLocPerm = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // 2. Switch එක Check වෙන්න නම් කරුණු දෙකක් සම්පූර්ණ වෙන්න ඕනේ:
        //    a) යූසර් ඇප් එක ඇතුළේ Switch එක On කරලා තියෙන්න ඕනේ.
        //    b) ඇන්ඩ්‍රොයිඩ් සිස්ටම් එකේ Permission එක Allowed වෙලා තියෙන්න ඕනේ.
        switchCallAccess.setChecked(sharedPreferences.getBoolean("call_access", false) && hasCallPerm);
        switchLocation.setChecked(sharedPreferences.getBoolean("location_access", false) && hasLocPerm);

        // මේවාට System Permissions ඕනේ නැහැ
        switchNotifications.setChecked(sharedPreferences.getBoolean("notifications", true));

        // Dark mode එකට SharedPreferences එක විතරක් බලමු
        switchDarkMode.setChecked(sharedPreferences.getBoolean("dark_mode", false));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (requestCode == 101) {
            if (granted) {
                saveSetting("call_access", true);
                switchCallAccess.setChecked(true); // දැන් Permission තියෙන නිසා On කරනවා
                Toast.makeText(getContext(), "Call Access Activated", Toast.LENGTH_SHORT).show();
            } else {
                saveSetting("call_access", false);
                switchCallAccess.setChecked(false);
                Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 102) {
            if (granted) {
                saveSetting("location_access", true);
                switchLocation.setChecked(true);
                Toast.makeText(getContext(), "Location Access Activated", Toast.LENGTH_SHORT).show();
            } else {
                saveSetting("location_access", false);
                switchLocation.setChecked(false);
                Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}