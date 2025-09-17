package amgn.amu.controller;

import amgn.amu.dto.LoginRequest;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.service.LoginService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @GetMapping("/login")
    public String showLogin(RedirectAttributes ra) {
        return "redirect:/login.html";
    }

    @GetMapping("/debug/session") @ResponseBody
    public Map<String,Object> session(HttpSession s) {
        return Map.of(
                "id", s.getId(),
                "attrs", Collections.list(s.getAttributeNames()),
                "loginUser", s.getAttribute("loginUser"));
    }

}
