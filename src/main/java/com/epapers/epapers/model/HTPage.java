package com.epapers.epapers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
public class HTPage {
    @JsonProperty("HighResolution")
    String highResolution;
    @JsonProperty("LowResolution")
    String lowResolution;
}
