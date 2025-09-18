package org.oryxel.gfp.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Config(@JsonProperty("show-position-by-default") boolean showPositionByDefault, @JsonProperty("capped-value") int maxPosition) {
}
