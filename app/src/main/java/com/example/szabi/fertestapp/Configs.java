package com.example.szabi.fertestapp;

public interface Configs {
    int INPUT_SIZE = 160; // image size, which is feed to neural network
    int NO_CLASSES = 7; // the number of classes, which the neural network provides

    String INPUT_NAME = "input"; // the label of the input node in neural network
    String OUTPUT_NAME = "output/Softmax"; // the label of the output node in neural network
    String MODEL_PATH = "frozen_fer.pb"; // the path to the file which contains the tensorflow model

    //String INPUT_NAME = "input"; // the label of the input node in neural network
    //String OUTPUT_NAME = "output/Softmax"; // the label of the output node in neural network
    //String MODEL_PATH = "fernet.pb"; // the path to the file which contains the tensorflow model

    String LABEL_PATH = "fer_labels.txt"; // the path to the file which contains the labels for the neural network

    int INPUT_WIDTH = 160; //224
    int INPUT_HEIGHT = 160; //224
}
