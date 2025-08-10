package com.czagrzebski.printhelm.web.domain.bambulab;

import com.czagrzebski.printhelm.web.domain.Printer;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("BAMBULAB")
public class BambuLabPrinter extends Printer {

}
