package org.billi.data;

import lombok.Data;

@Data
public class WebhookSetResult {
    private Boolean ok;
    private Boolean result;
    private String description;
}
