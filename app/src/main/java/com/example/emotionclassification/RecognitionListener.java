package com.example.emotionclassification;

import org.tensorflow.lite.support.label.Category;

public interface RecognitionListener {
    void onResult(Category category);
}