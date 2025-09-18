package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class JobDTO {
    @JsonProperty("cur_stage")
    private CurStageDTO curStage;
    private List<StageDTO> stage;

    public CurStageDTO getCurStage() {
        return curStage;
    }

    public void setCurStage(CurStageDTO curStage) {
        this.curStage = curStage;
    }

    public List<StageDTO> getStage() {
        return stage;
    }

    public void setStage(List<StageDTO> stage) {
        this.stage = stage;
    }
}
