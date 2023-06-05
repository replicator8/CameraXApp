package com.example.emotionclassification;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.emotionclassification.ml.Model22;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ImageAnalyzer implements ImageAnalysis.Analyzer {
    private RecognitionListener listener;
    private Model22 model;

    public ImageAnalyzer(Context context, RecognitionListener listener) {
        try {
            this.listener = listener;
            this.model = Model22.newInstance(context);
        } catch (IOException e) {
            Log.e("ImageAnalyzer", "Error: " + e.getMessage());
        }
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {

    }

//    @Override
//    public void analyze(@NonNull ImageProxy image) {
//        TensorImage tensorImage = TensorImage.fromBitmap(image.toBitmap());
//
//        int inputTensorWidth = 224;
//        int inputTensorHeight = 224;
//        int inputTensorChannels = 1;
//
//        TensorBuffer inputBuffer = TensorBuffer.createFixedSize(
//                new int[]{1, inputTensorWidth, inputTensorHeight, inputTensorChannels},
//                DataType.FLOAT32
//        );
//
//        tensorImage.loadToTensorBuffer(inputBuffer);
//
//        Model22.Outputs outputs = model.process(inputBuffer);
//
//        List<Category> sortedOutputs = outputs.getProbabilityAsCategoryList()
//                .stream()
//                .sorted((item1, item2) -> Float.compare(item1.getScore(), item2.getScore()))
//                .collect(Collectors.toList());
//
//        listener.onResult(sortedOutputs.get(sortedOutputs.size() - 1));
//        image.close();
//    }
}