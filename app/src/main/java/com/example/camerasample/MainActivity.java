package com.example.camerasample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

//import com.microsoft.azure.cognitiveservices.vision.computervision.*;
//import com.microsoft.azure.cognitiveservices.vision.computervision.models.*;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;

import java.util.ArrayList;
import java.util.List;


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
    }

//    public static void AnalyzeLocalImage(ComputerVisionClient compVisClient, Bitmap bmp) {
//        /*  Analyze a local image:
//         *
//         *  Set a string variable equal to the path of a local image. The image path below is a relative path.
//         */
//        //String pathToLocalImage = "src\\main\\resources\\starbucks.jpg";
//        //  This list defines the features to be extracted from the image.
//        List<VisualFeatureTypes> featuresToExtractFromLocalImage = new ArrayList<>();
//        featuresToExtractFromLocalImage.add(VisualFeatureTypes.DESCRIPTION);
//        featuresToExtractFromLocalImage.add(VisualFeatureTypes.TAGS);
//
//        //  Need a byte array for analyzing a local image.
//        //File rawImage = new File(pathToLocalImage);
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
//        byte[] imageByteArray = stream.toByteArray();
//        bmp.recycle();
//        try {
//            //byte[] imageByteArray = Files.readAllBytes(rawImage.toPath());
//            //  Call the Computer Vision service and tell it to analyze the loaded image.
//            ImageAnalysis analysis = compVisClient.computerVision().analyzeImageInStream()
//                    .withImage(imageByteArray)
//                    .withVisualFeatures(featuresToExtractFromLocalImage)
//                    .execute();
//
//            //  Display image captions and confidence values.
//            System.out.println("\nCaptions: ");
//            for (ImageCaption caption : analysis.description().captions()) {
//                System.out.printf("\'%s\' with confidence %f\n", caption.text(), caption.confidence());
//            }
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//            e.printStackTrace();
//        }
//
//    }

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

//    private void dispatchTakePictureIntent() {
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        // Ensure that there's a camera activity to handle the intent
//        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//            // Create the File where the photo should go
//            File photoFile = null;
//            try {
//                photoFile = createImageFile();
//            } catch (IOException ex) {
//                // Error occurred while creating the File
//            }
//            // Continue only if the File was successfully created
//            if (photoFile != null) {
//                Uri photoURI = FileProvider.getUriForFile(this,
//                        "com.example.android.camerasample",
//                        photoFile);
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
//            }
//        }
//    }

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



//    public static void detectLogos(Bitmap bmp, PrintStream out) throws Exception, IOException {
//        List<AnnotateImageRequest> requests = new ArrayList<>();
//
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
//        byte[] byteArray = stream.toByteArray();
//        bmp.recycle();
//
//
//        ByteString imgBytes = ByteString.copyFrom(byteArray);
//
//        Image img = Image.newBuilder().setContent(imgBytes).build();
//        Feature feat = Feature.newBuilder().setType(Type.LOGO_DETECTION).build();
//        AnnotateImageRequest request =
//                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
//        requests.add(request);
//
//        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
//            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
//            List<AnnotateImageResponse> responses = response.getResponsesList();
//
//            for (AnnotateImageResponse res : responses) {
//                if (res.hasError()) {
//                    out.printf("Error: %s\n", res.getError().getMessage());
//                    return;
//                }
//
//                // For full list of available annotations, see http://g.co/cloud/vision/docs
//                for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
//                    out.println(annotation.getDescription());
//                }
//            }
//        }
//    }
}
