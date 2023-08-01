package com.epapers.epapers.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class KPPageNew {
    String pageid;
    String pageno;
    String imagename;
    String pagename;
    @JsonProperty("Articles")
    List<Object> articles;
}
