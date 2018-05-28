package com.example.szabi.fertestapp.utils;

import com.example.szabi.fertestapp.model.face.LabelsType;

import java.util.HashMap;
import java.util.Map;

public class EmojiMapper {

    private Map<LabelsType, String> emojiMap;
    ;
    private static EmojiMapper emojiMapper;

    public static EmojiMapper getInstance() {
        if (emojiMapper == null) {
            emojiMapper = new EmojiMapper();
        }
        return emojiMapper;
    }

    private EmojiMapper() {
        emojiMap = new HashMap<>();
        emojiMap.put(LabelsType.HAPPY, "\uD83D\uDE00");
        emojiMap.put(LabelsType.SAD, "\uD83D\uDE1E");
        emojiMap.put(LabelsType.SURPRISED, "\uD83D\uDE2E");
        emojiMap.put(LabelsType.NEUTRAL, "\uD83D\uDE10");
        emojiMap.put(LabelsType.FEAR, "\uD83D\uDE28");
        emojiMap.put(LabelsType.ANGRY, "\uD83D\uDE20");
        emojiMap.put(LabelsType.DISGUST, "\uD83E\uDD22");
    }

    public String getEmojiCode(LabelsType type) {
        return emojiMap.get(type);
    }

}
