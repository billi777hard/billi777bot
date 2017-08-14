package org.billi.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SendMessage {
    private String method;

    @JsonProperty("chat_id")
    private String chatId;

    private String text;

    public static SendMessage send(String chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setMethod("sendMessage");
        msg.setChatId(chatId);
        msg.setText(text);
        return msg;
    }
}
