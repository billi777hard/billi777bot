package org.billi.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class WebhookParams {
    private String url;

    @JsonProperty("allowed_updates")
    private List<String> allowedUpdates;

    public static WebhookParams create(String url) {
        WebhookParams p = new WebhookParams();
        p.url = url;
        p.allowedUpdates = Collections.singletonList("message");
        return p;
    }
}
