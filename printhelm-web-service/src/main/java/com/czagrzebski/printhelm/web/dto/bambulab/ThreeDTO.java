package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ThreeDTO {
    @JsonProperty("layer_num")
    private int layerNum;
    @JsonProperty("total_layer_num")
    private int totalLayerNum;

    public int getLayerNum() {
        return layerNum;
    }

    public void setLayerNum(int layerNum) {
        this.layerNum = layerNum;
    }

    public int getTotalLayerNum() {
        return totalLayerNum;
    }

    public void setTotalLayerNum(int totalLayerNum) {
        this.totalLayerNum = totalLayerNum;
    }
}
