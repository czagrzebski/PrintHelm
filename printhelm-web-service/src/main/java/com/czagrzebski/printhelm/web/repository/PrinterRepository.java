package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.Printer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrinterRepository extends JpaRepository<Printer, Long> {
    Printer findByPrinterName(String printerName);
    Printer findByPrinterId(Long printerId);
    Printer findByPrinterNameAndUserId(String printerName, Long userId);
    Printer findByPrinterIdAndUserId(Long printerId, Long userId);
}
