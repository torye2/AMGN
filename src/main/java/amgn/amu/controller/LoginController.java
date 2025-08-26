package amgn.amu.controller;

import amgn.amu.dto.LoginRequest;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.service.LoginService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @GetMapping("/login")
    public String showLogin(RedirectAttributes ra) {
        ra.addFlashAttribute("loginUser", new LoginRequest());
        return "redirect:/login.html";
    }

    @PostMapping("/loginAction")
    public String login(@Valid LoginRequest req
            , BindingResult br
            , RedirectAttributes ra
            , HttpSession session) {
        if (br.hasErrors()) {
            return "redirect:/login.html";
        }
        try {
            LoginUserDto loginUser = loginService.login(req);
            session.setAttribute("loginUser", loginUser);
            return "redirect:/main.html";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("loginError", e.getMessage());
            return "redirect:/login.html";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }
}
