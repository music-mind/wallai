package com.example.wallai;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private static final int SPEECH_REQUEST_CODE = 0;
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
    Context context;
    public static List<Integer> myPrices = null;
    String curBrand;
    Double curPrice;
    String collectionPath;
    Map<String, String> curItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        this.context = getApplicationContext();
        StrictMode.ThreadPolicy policy = new
                StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //azureComputerVisionApiKey = System.getenv("COMPUTER_VISION_SUBSCRIPTION_KEY");
        azureComputerVisionApiKey = "a4d18db961af49748102472e13105095";
        System.out.println("KEY: " + azureComputerVisionApiKey);
        endpointUrl = "https://westcentralus.api.cognitive.microsoft.com/vision/v2.0";
//        compVisClient = ComputerVisionManager.authenticate(azureComputerVisionApiKey).withEndpoint(endpointUrl);
        //  END - Create an authenticated Computer Vision client.

        myPrices = new ArrayList<>();

        // Firebase
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

//        Map<String, String> thing3 = new HashMap<>();
//        thing3.put("NAME", "foundation");
//        thing3.put("BRAND", "Clinique");
//        addEntry("cosmetics", thing3).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//            @Override
//            public void onSuccess(DocumentReference documentReference) {
//                Log.d("DB_ADD", "DocumentSnapshot added with ID: " + documentReference.getId());
//            }
//        });
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


    // get all database entries from a collection
    private void getAllEntries(final String collectionPath, final Map<String, String> item) {
        db.collection(collectionPath)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if((task.isSuccessful())) {
                            Boolean resultNotEmpty = false;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                resultNotEmpty = true;
                                Log.d("DB_GET", document.getId() + " => " + document.getData());
                            }
//                            if (!resultNotEmpty) { // if no items were found, i.e. task.getResult() was empty
//                                Log.d("DB_GET", "result was empty, add thing now");
//                                addEntry(collectionPath, item);
//                            } else { // if items were found
                                findPrice(collectionPath, item, task);
//                            }
                        } else {
                            Log.w("DB_GET", "Error getting documents.", task.getException());
                            return;
                        }
                    }
                });
    }

    private void findPrice(String collectionPath, Map<String, String> item, Task<QuerySnapshot> task) {
        List<Double> bestMatchPrices = new ArrayList<>();
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
                bestMatchPrices = Arrays.asList(Double.parseDouble(document.get("PRICE").toString()));
            } else if (tempCount == bestMatchCount) {
                bestMatchPrices.add(Double.parseDouble(document.get("PRICE").toString()));
            }
        }
        // get average
        int length = 0;
        double priceSum = 0;
        for (double price : bestMatchPrices) {
            ++length;
            priceSum += price;
        }

        if (length == 0) {
            Log.d("DB_GET", "result was empty, add thing now");
            //addEntry(collectionPath, item);

            displaySpeechRecognizer();
        } else {
            curPrice = priceSum/length;
            TextView tv2 = (TextView)findViewById(R.id.textView2);
            tv2.setText("$ " + Double.toString(curPrice));
        }

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
        } else if ((requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK)) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            Double num;
            try {
                num = Double.parseDouble(spokenText);
            } catch (NumberFormatException e) {
                num = 2.99;
            }
            // Do something with spokenText
            curPrice = num;
            TextView tv2 = (TextView)findViewById(R.id.textView2);
            tv2.setText("$ " + Double.toString(curPrice));
            Toast toast = Toast.makeText(context, "Price: $ " + num.toString(), Toast.LENGTH_LONG);
            toast.show();
            curItem.put("PRICE", curPrice.toString());
            addEntry(collectionPath, curItem);
        }
    }

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
// Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
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

            //System.out.println(EntityUtils.toString(response.getEntity()));
            String res = EntityUtils.toString(response.getEntity());
            Log.i("Data", res);
            try {
                JSONObject json = new JSONObject(res);
                String brand = "";
                String product = "";
                String caption = "";
                Double price = 0.00;
                JSONObject description = json.getJSONObject("description");
                JSONArray tags = description.getJSONArray("tags");
                if(tags.length() > 1) {
                    product = tags.get(0).toString() + " " + tags.get(1).toString();
                } else {
                    product = tags.get(0).toString();
                }
                Log.i("Product", product);
                JSONArray captions = description.getJSONArray("captions");
                if(captions.length() > 0) {
                    JSONObject captionObj = captions.getJSONObject(0);
                    caption = captionObj.getString("text");
                }
                JSONArray brands = json.getJSONArray("brands");
                if(brands.length() > 0) {
                    JSONObject brandObj = brands.getJSONObject(0);
                    brand = brandObj.getString("name");
                    curBrand = brand;
                } else {
                    brand = "MYSTERY";
                    curBrand = brand;
                }
                curPrice = price;
                Log.i("Caption", caption);
                Log.i("Brand", brand);
                // set item
                TextView tv = (TextView)findViewById(R.id.textView);
                tv.setText(brand.toUpperCase());

                Map<String, String> newEntry = new HashMap<String, String>();
                newEntry.put(product, brand);
                Log.d("DEBUG_FIND", "product map: " + product);
                this.collectionPath = "drinks";
                this.curItem = newEntry;
                getAllEntries("drinks", newEntry );

                Toast toast = Toast.makeText(context, brand + ": " + caption, Toast.LENGTH_LONG);
                toast.show();
//                displaySpeechRecognizer();
                // Get Estimated Price
                //
                //
            }catch (JSONException err){
                Log.d("Error", err.toString());
            }
        }catch(Exception e){
            System.out.println("ERRRRR"  + e.toString());
        }
    }
}

