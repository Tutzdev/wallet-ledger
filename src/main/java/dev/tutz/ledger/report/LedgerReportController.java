package dev.tutz.ledger.report;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerReportController {

    private final LedgerReportService reports;

    public LedgerReportController(LedgerReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/integrity")
    public IntegrityReport integrity() {
        return reports.integrity();
    }

    @GetMapping("/summary")
    public LedgerSummary summary() {
        return reports.summary();
    }
}
