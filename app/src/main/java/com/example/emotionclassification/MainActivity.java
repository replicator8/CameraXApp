package com.example.emotionclassification;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.emotionclassification.databinding.ActivityMainBinding;
import com.example.emotionclassification.ml.Model22;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements RecognitionListener {
    private ActivityMainBinding binding;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ActivityResultLauncher<PickVisualMediaRequest> pickVisualLauncher;
    private ImageCapture imageCapture;
    private final String TAG = "MainActivity";
    private TextView result;
    private TextView str;
    private PreviewView previewView;

    class CNNQuery extends AsyncTask<Bitmap, Void, String> {

        @Override
        protected void onPreExecute() {
            str.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Bitmap... image) {
            String res = null;
            try {
                Model22 model = Model22.newInstance(getApplicationContext());

                // Creates inputs for reference.
                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224}, DataType.FLOAT32);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224);
                byteBuffer.order(ByteOrder.nativeOrder());

                int[] values = new int[224 * 224];
                image[0].getPixels(values, 0, image[0].getWidth(), 0, 0, image[0].getWidth(), image[0].getHeight());

                int pixel = 0;
                for (int i = 0; i < 224; i++) {
                    for (int j = 0; j < 224; j++) {
                        int val = values[pixel++];
                        byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                        byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                        byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                    }
                }

                inputFeature0.loadBuffer(byteBuffer);

                // Runs model inference and gets result.
                Model22.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                float[] confidences = outputFeature0.getFloatArray();
                int maxPos = 0;
                float maxConfidence = 0;
                for (int i = 0; i < confidences.length; i++) {
                    if (confidences[i] > maxConfidence) {
                        maxConfidence = confidences[i];
                        maxPos = i;
                    }
                }

                String[] emotions = {"anger", "happiness", "sadness", "surprise"};
                res = "It's a " + emotions[maxPos];

                // Releases model resources if no longer used.
                model.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            return res;
        }

        @Override
        protected void onPostExecute(String res) {
            result.setText("");
            result.setText(res);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        previewView = findViewById(R.id.cameraView);
        result = findViewById(R.id.classified);
        str = findViewById(R.id.its);

        openCamera();
        registerActivityForPickImage();

        binding.takePicture.setOnClickListener(v -> {
            String name = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(System.currentTimeMillis());

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM);

            ImageCapture.OutputFileOptions outputOptions =
                    new ImageCapture.OutputFileOptions.Builder(getContentResolver(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues).build();

            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    Log.d(TAG, "Image saved successfully!");
                    Bitmap image = (Bitmap) previewView.getBitmap();
                    if (image != null) {
                        int dimension = Math.min(image.getWidth(), image.getHeight());
                        image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                        image = Bitmap.createScaledBitmap(image, 100, 100, false);

                        new CNNQuery().execute(image);
                    }
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    String text = "Error: " + exception.getMessage();
                    Toast.makeText(getBaseContext(), text, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, text);
                }
            });
        });

        binding.galleryView.setOnClickListener(v -> {
            pickVisualLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
    }

    public void classifyImage(Bitmap image) {

    }

    //Permission for OpenGallery
    private void registerActivityForPickImage() {
        pickVisualLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: " + uri);
            } else {
                Log.d("PhotoPicker", "No media selected");
            }
        });
    }

    //Open Camera and check permissions
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityResultLauncher<String[]> launcher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                result.forEach((permission, res) -> {
                    if (permission.equals(Manifest.permission.CAMERA)) {
                        openCamera();
                    }
                });
            });
            launcher.launch(new String[]{Manifest.permission.CAMERA});
        } else {
            bindPreview();
        }
    }

    private void bindPreview() {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = new ImageCapture.Builder().build();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                preview.setSurfaceProvider(binding.cameraView.getSurfaceProvider());

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onResult(Category category) {
        Log.w(TAG, category.getLabel());
    }
}
