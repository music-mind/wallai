package com.example.cucumber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // testing code
//        Map<String, String> thing1 = new HashMap<>();
//        thing1.put("NAME", "lipstick");
//        thing1.put("BRAND", "M.A.C.");
//
//        Map<String, String> thing2 = new HashMap<>();
//        thing2.put("NAME", "lipstick");
//        thing2.put("BRAND", "Too Faced");

        Map<String, String> thing3 = new HashMap<>();
        thing3.put("NAME", "foundation");
        thing3.put("BRAND", "Clinique");

//        Map<String, String> thing4 = new HashMap<>();
//        thing4.put("NAME", "eyeliner");
//        thing4.put("BRAND", "Stila");

//        addEntry("cosmetics", thing1);
//        addEntry("cosmetics", thing2);
        addEntry("cosmetics", thing3).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d("DB_ADD", "DocumentSnapshot added with ID: " + documentReference.getId());
            }
        });

        //getAllEntries("cosmetics");

        Set<String[]> filter1 = new HashSet<>();
        filter1.add(new String[]{"NAME", "lipstick"});

        filterEntries("cosmetics", filter1).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    Boolean gotResult = false;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        gotResult = true;
                        Log.d("DB_QUERY", document.getId() + " => " + document.getData());
                    }

                    if (!gotResult) Log.d("DB_QUERY_TO_ADD", "go to add screen here");
                } else {
                    Log.d("DB_QUERY", "Error getting documents: ", task.getException());
                }
            }
        });

//        filter1.add(new String[]{"BRAND", "Too Faced"});
        //filterEntries("cosmetics", filter1);

//        addEntry("cosmetics", thing4);

        //getAllEntries("cosmetics");

        setContentView(R.layout.activity_main);
    }

    // add database entry to a collection
    private Task<DocumentReference> addEntry(String collectionPath, Map<String, String> item) {
        // Add a new document with a generated ID
        Task<DocumentReference> taskRef = db.collection(collectionPath).add(item);

        taskRef.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w("DB_ADD", "Error adding document", e);
            }
        });

        return taskRef;

//  TO USE - define the addOnSuccessListener like below:
//        taskRef.addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//            @Override
//            public void onSuccess(DocumentReference documentReference) {
                  // your stuff here
//                Log.d("DB_ADD", "DocumentSnapshot added with ID: " + documentReference.getId());
//            }
//        });
    }

    // get filtered database entries from collection
    // at the moment only uses whereEqualTo query, since I think that's all we're gonna need right?
    // structure of filter: index [0] is field name, index [1] is filter value
    private Task<QuerySnapshot> filterEntries(String collectionPath, Set<String[]> filters) {
        Query query = db.collection(collectionPath);

        for (String[] filter : filters) {
            query = query.whereEqualTo(filter[0], filter[1]);
        }

        return query.get();

//  TO USE - add the addOnCompleteListener, example below:
//        query.addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                if (task.isSuccessful()) {
//                    Boolean gotResult = false;
//                    for (QueryDocumentSnapshot document : task.getResult()) {
//                        gotResult = true;
//                        Log.d("DB_QUERY", document.getId() + " => " + document.getData());
//                    }
//
//                    if (!gotResult) Log.d("DB_QUERY_TO_ADD", "go to add screen here");
//                } else {
//                    Log.d("DB_QUERY", "Error getting documents: ", task.getException());
//                }
//            }
//        });
    }

    // get database entry from collection by ID
    private void getEntryByID(String collectionPath, String name) {
        DocumentReference docRef = db.collection(collectionPath).document(name);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("DB_GET_BY_ID", "DocumentSnapshot data: " + document.getData());
                    } else {
                        Log.d("DB_GET_BY_ID", "No such document");
                    }
                } else {
                    Log.d("DB_GET_BY_ID", "get failed with ", task.getException());
                }
            }
        });
    }

    // get all database entries from a collection
    private void getAllEntries(String collectionPath) {
        db.collection(collectionPath)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("DB_GET", document.getId() + " => " + document.getData());
                        }

                    } else {
                        Log.w("DB_GET", "Error getting documents.", task.getException());
                        return;
                    }
                }
            });
    }

}
