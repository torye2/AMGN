package amgn.amu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportController {
    @GetMapping("/reportForm")
    public String showReportForm() {
        return "redirect:/report/reportForm.html";
    }

    @GetMapping("/reportList")
    public String showReportList() {
        return "redirect:/report/reportList.html";
    }

    @GetMapping("/reportDetail")
    public String showReportDetail() {
        return "redirect:/report/reportDetail.html";
    }
}
