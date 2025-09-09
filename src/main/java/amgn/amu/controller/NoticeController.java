package amgn.amu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NoticeController {

    @GetMapping("/notice")
    public String showNoticeList() {
        return "redirect:/notice-l.html";
    }

    @GetMapping("/notice-v")
    public String showNoticeV() {
        return "redirect:/notice-v.html";
    }

    @GetMapping("/notice-w")
    public String showNoticeW() {
        return "redirect:/notice-w.html";
    }
}
