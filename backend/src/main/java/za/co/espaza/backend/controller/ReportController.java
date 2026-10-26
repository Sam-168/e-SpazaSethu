package za.co.espaza.backend.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.espaza.backend.dto.response.DashboardReportResponse;
import za.co.espaza.backend.dto.response.ReportPerformanceResponse;
import za.co.espaza.backend.dto.response.SalesSummaryResponse;
import za.co.espaza.backend.services.ReportService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardReportResponse> dashboard() {
        return ResponseEntity.ok(reportService.dashboard());
    }

    @GetMapping("/summary")
    public ResponseEntity<SalesSummaryResponse> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.summary(from, to));
    }

    @GetMapping("/performance")
    public ResponseEntity<ReportPerformanceResponse> performance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.performance(from, to));
    }
}
