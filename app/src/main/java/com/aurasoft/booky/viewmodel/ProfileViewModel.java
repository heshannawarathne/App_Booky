package com.aurasoft.booky.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;

public class ProfileViewModel extends ViewModel {
    private final MutableLiveData<Map<String, Object>> userData = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<Map<String, Object>> getUserData(String uid) {
        db.collection("Users").document(uid).addSnapshotListener((snapshot, error) -> {
            if (error == null && snapshot != null && snapshot.exists()) {
                userData.setValue(snapshot.getData());
            }
        });
        return userData;
    }

    public void updateField(String uid, String key, String value) {
        db.collection("Users").document(uid).update(key, value);
    }
}