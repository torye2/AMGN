package amgn.amu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MyPageController {
    @GetMapping("/myPage.html")
    public String showMyPage() {
        return "redirect:/myPage.html";
    }
}
