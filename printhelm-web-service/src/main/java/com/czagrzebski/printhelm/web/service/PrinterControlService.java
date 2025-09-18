package com.czagrzebski.printhelm.web.service;

public interface PrinterControlService {
    void startPrint(String printerId, Long jobId);
    void pausePrint(String printerId);
    void resumePrint(String printerId);
    void cancelPrint(String printerId);
    void stopPrint(String printerId);
}
