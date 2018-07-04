package com.example.szabi.fertestapp;

public interface Configs {
    int INPUT_SIZE = 160; // image size, which is feed to neural network

    String INPUT_NAME = "input"; // the label of the input node in neural network
    String OUTPUT_NAME = "output/Softmax"; // the label of the output node in neural network
    String MODEL_PATH = "fer_july.pb"; // the path to the file which contains the tensorflow model
    String LABEL_PATH = "fer_labels.txt"; // the path to the file which contains the labels for the neural network

    int INPUT_WIDTH = 160; //224
    int INPUT_HEIGHT = 160; //224

    // database management
    //String DB_MESSAGES = "messages";
    String DB_TESTS = "tests";
    String DB_USERS = "users";    String DB_CONVERSATIONS = "conversations";
    String DB_CONVERSATION_MESSAGES = "conversationMessages";
    String DB_GROUPS = "conversationGroups";
    String DB_UTILS = "utils";
    String DB_NUMBER_OF_CONVERSATIONS = "numberOfConversations";
    String DB_NUMBER_OF_GROUPS = "numberOfConversationGroups";
}
