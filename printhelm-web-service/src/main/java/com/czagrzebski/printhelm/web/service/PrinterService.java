package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import org.springframework.stereotype.Service;

@Service
public class PrinterService {

    private final PrinterRepository printerRepository;

    public PrinterService(PrinterRepository printerRepository) {
        this.printerRepository = printerRepository;
    }

    public void createPrinter() {

    }

}
