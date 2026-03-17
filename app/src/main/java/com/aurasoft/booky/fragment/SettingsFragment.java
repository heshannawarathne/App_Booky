package com.aurasoft.booky.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.aurasoft.booky.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    // Switches
    private SwitchMaterial switchNotifications, switchCallAccess, switchLocation, switchDarkMode;

    // Clickable Items
    private TextView tvPrivacyPolicy, tvTerms, tvLegalContent;
    private View cvProfile; // XML එකේ Profile එක තියෙන්නේ CardView එකක් ඇතුළේ නිසා
    private ImageView btnBack;

    // Layouts
    private LinearLayout settingsMenuLayout;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Layout එක inflate කරනවා
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 1. UI Elements initialize කරනවා
        settingsMenuLayout = view.findViewById(R.id.settingsMenuLayout);
        tvLegalContent = view.findViewById(R.id.tvLegalContent);

        tvPrivacyPolicy = view.findViewById(R.id.tvPrivacyPolicy);
        tvTerms = view.findViewById(R.id.tvTerms);
        cvProfile = view.findViewById(R.id.cvProfile);
        btnBack = view.findViewById(R.id.btnBack);

        // Switches
        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchCallAccess = view.findViewById(R.id.switchCallAccess);
        switchLocation = view.findViewById(R.id.switchLocation);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);

        // 2. Privacy Policy එක පෙන්වන Logic එක
        tvPrivacyPolicy.setOnClickListener(v -> {
            showLegalContent(getString(R.string.privacy_policy_text));
        });

        // 3. Terms of Service එක පෙන්වන Logic එක
        tvTerms.setOnClickListener(v -> {
            showLegalContent(getString(R.string.terms_of_service_text));
        });

        // 4. Profile Fragment එකට යාම
        cvProfile.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 5. Back Button එකේ වැදගත් Logic එක
        btnBack.setOnClickListener(v -> {
            // පරිශීලකයා Privacy/Terms බලන ගමන් නම් ඉන්නේ, ආයේ Settings මෙනු එකට එන්න
            if (tvLegalContent.getVisibility() == View.VISIBLE) {
                tvLegalContent.setVisibility(View.GONE);
                settingsMenuLayout.setVisibility(View.VISIBLE);
            } else {
                // නැත්නම් කලින් හිටපු Fragment (Home) එකට යන්න
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            }
        });

        return view;
    }

    // මෙනු එක හංගලා නීතිමය කරුණු පෙන්නන්න පාවිච්චි කරන method එක
    private void showLegalContent(String text) {
        settingsMenuLayout.setVisibility(View.GONE);
        tvLegalContent.setVisibility(View.VISIBLE);
        tvLegalContent.setText(text);
    }
}