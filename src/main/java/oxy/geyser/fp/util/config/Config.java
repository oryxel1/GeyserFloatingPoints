package oxy.geyser.fp.util.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Config(@JsonProperty("show-position-by-default")
                     boolean showPositionByDefault, @JsonProperty("capped-value") int maxPosition) {
}
