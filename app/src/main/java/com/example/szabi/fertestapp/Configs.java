package com.example.szabi.fertestapp;

public interface Configs {
    int INPUT_SIZE = 224; // image size, which is feed to neural network
    int NO_CLASSES = 7; // the number of classes, which the neural network provides

    String INPUT_NAME = "input_1"; // the label of the input node in neural network
    String OUTPUT_NAME = "dense_1/Softmax"; // the label of the output node in neural network

    String MODEL_PATH = "frozen_fer_mobilenet.pb"; // the path to the file which contains the tensorflow model
    String LABEL_PATH = "fer_labels.txt"; // the path to the file which contains the labels for the neural network
}
