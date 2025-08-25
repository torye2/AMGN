package amgn.amu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportController {
    @GetMapping("/reportForm")
    public String showReportForm() {
        return "redirect:/reportForm.html";
    }

    @GetMapping("/reportList")
    public String showReportList() {
        return "redirect:/reportList.html";
    }

    @GetMapping("/reportDetail")
    public String showReportDetail() {
        return "redirect:/reportDetail.html";
    }
}
