package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class BambulabStateDTO {

    public PrintDTO getPrint() {
        return print;
    }

    public void setPrint(PrintDTO print) {
        this.print = print;
    }

    @JsonProperty("print")
    private PrintDTO print;

}