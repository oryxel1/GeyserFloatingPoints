package org.oryxel.gfp.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Config(@JsonProperty("capped-value") int maxPosition) {
}
