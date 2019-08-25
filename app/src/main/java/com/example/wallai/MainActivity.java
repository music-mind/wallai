package com.example.wallai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

//import com.microsoft.azure.cognitiveservices.vision.computervision.*;
//import com.microsoft.azure.cognitiveservices.vision.computervision.models.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


public class MainActivity extends AppCompatActivity {

    String currentPhotoPath;
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    String azureComputerVisionApiKey;
    String endpointUrl;
    //ComputerVisionClient compVisClient;
    Bitmap bitmap;
    public static String baseUrl = "https://westcentralus.api.cognitive.microsoft.com/vision/v2.0/analyze?visualFeatures=Categories,Description,Brands";
    public static HttpClient client = new DefaultHttpClient();
    public static HttpPost httpPost = new HttpPost(baseUrl);
    public static ArrayList<NameValuePair> myParams = new ArrayList<>(1);
    private FirebaseFirestore db = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new
                StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //azureComputerVisionApiKey = System.getenv("COMPUTER_VISION_SUBSCRIPTION_KEY");
        azureComputerVisionApiKey = "a4d18db961af49748102472e13105095";
        System.out.println("KEY: " + azureComputerVisionApiKey);
        endpointUrl = "https://westcentralus.api.cognitive.microsoft.com/vision/v2.0";
//        compVisClient = ComputerVisionManager.authenticate(azureComputerVisionApiKey).withEndpoint(endpointUrl);
        //  END - Create an authenticated Computer Vision client.

        // Firebase
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        Map<String, String> thing3 = new HashMap<>();
        thing3.put("NAME", "foundation");
        thing3.put("BRAND", "Clinique");
        addEntry("cosmetics", thing3).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d("DB_ADD", "DocumentSnapshot added with ID: " + documentReference.getId());
            }
        });
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

    private int findPrice(Map<String, String> item, Task<QuerySnapshot> task) {
        List<Integer> bestMatchPrices = new ArrayList<>();
        int bestMatchCount = 1;
        int tempCount;
        for (QueryDocumentSnapshot document : task.getResult()) {
            Log.d("DB_GET", document.getId() + " => " + document.getData());
            tempCount = 0;
            for (String key : item.keySet()) {
                if (item.get(key).equals(document.get(key))) ++tempCount;
            }
            if (tempCount > bestMatchCount) {
                bestMatchCount = tempCount;
                bestMatchPrices = Arrays.asList((Integer) document.get("price"));
            } else if (tempCount == bestMatchCount) {
                bestMatchPrices.add((Integer) document.get("price"));
            }
        }
        // get average
        int length = 0;
        int priceSum = 0;
        for (int price : bestMatchPrices) {
            ++length;
            priceSum += price;
        }
        return priceSum / length;
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void camera(View view) {
        this.dispatchTakePictureIntent();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            this.bitmap = imageBitmap;
            //new GetMethod(azureComputerVisionApiKey, imageBitmap).execute(baseUrl);
            //AnalyzeLocalImage(compVisClient, imageBitmap);
            getImage();
//            try {
//                this.detectLogos(imageBitmap, System.out);
//            } catch (IOException e) {
//
//            } catch (Exception ex) {}
            //imageView.setImageBitmap(imageBitmap);
        }
    }

    public void getImage(){
        httpPost.setHeader("Ocp-Apim-Subscription-Key", azureComputerVisionApiKey);
        httpPost.setHeader("Content-Type", "application/octet-stream");

        try{
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            this.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] imageByteArray = stream.toByteArray();
            this.bitmap.recycle();

            httpPost.setEntity(new UrlEncodedFormEntity(myParams, "UTF-8"));
            httpPost.setEntity(new ByteArrayEntity(imageByteArray));
            HttpResponse response =  client.execute(httpPost);

            System.out.println(EntityUtils.toString(response.getEntity()));
        }catch(Exception e){
            System.out.println("ERRRRR"  + e.toString());
        }
    }
}

