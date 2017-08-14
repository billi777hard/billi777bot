package org.billi.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamsResponse {
    List<TwitchStream> streams;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TwitchStream {
        private String game;
        @JsonProperty("stream_type")
        private String streamType;  // need "live"
        private TwitchChannel channel;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TwitchChannel {
        private String status;  // description
    }
}
