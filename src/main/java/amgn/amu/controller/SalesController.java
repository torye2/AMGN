package amgn.amu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SalesController {
    @GetMapping("/salesList")
    public String showSalesList() {
        return "redirect:/salesList.html";
    }

    @GetMapping("/salesRegistration")
    public String showSalesRegistration() {
        return "redirect:/salesRegistration.html";
    }
}
