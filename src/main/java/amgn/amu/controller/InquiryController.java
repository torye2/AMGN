package amgn.amu.controller;

import amgn.amu.dto.LoginUserDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InquiryController {

    @GetMapping("/inquiry")
    public String routeInquiry(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            Object attr = session.getAttribute("loginUser");
            if (attr instanceof LoginUserDto u && "관리자".equals(u.getUserName())) {
                return "redirect:/mypage/myPage.html#support";
            }
        }
        return "redirect:/inquiries.html";
    }

}
