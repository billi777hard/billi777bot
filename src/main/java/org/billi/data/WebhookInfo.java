package org.billi.data;

import lombok.Data;

@Data
public class WebhookInfo {
    
    private Boolean ok;
    private WebhookResult result;

    @Data
    public static class WebhookResult {
        private String url;
    }
}
