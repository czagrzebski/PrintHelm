package com.czagrzebski.printhelm.web.ai;

import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import com.czagrzebski.printhelm.web.service.AuditLogService;
import com.czagrzebski.printhelm.web.service.PrintQueueService;
import com.czagrzebski.printhelm.web.service.PrinterStateCache;
import org.springframework.stereotype.Component;

/**
 * Builds a fresh set of tool instances bound to one {@link ChatRequestContext}
 * per chat request, so proposed actions and status updates from concurrent
 * requests never mix.
 */
@Component
public class ChatToolFactory {

    private final PrinterStateCache printerStateCache;
    private final PrinterRepository printerRepository;
    private final PrintQueueService printQueueService;
    private final AuditLogService auditLogService;

    public ChatToolFactory(PrinterStateCache printerStateCache,
                           PrinterRepository printerRepository,
                           PrintQueueService printQueueService,
                           AuditLogService auditLogService) {
        this.printerStateCache = printerStateCache;
        this.printerRepository = printerRepository;
        this.printQueueService = printQueueService;
        this.auditLogService = auditLogService;
    }

    public Object[] createTools(ChatRequestContext ctx) {
        return new Object[] {
                new PrinterStateTool(printerStateCache, printerRepository, ctx),
                new PrinterControlTool(printerRepository, ctx, auditLogService),
                new PrintQueueTool(printQueueService, printerRepository, ctx, auditLogService),
        };
    }
}
