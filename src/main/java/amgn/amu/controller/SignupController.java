package amgn.amu.controller;

import amgn.amu.dto.SignupDto;
import amgn.amu.service.SignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/signup")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    @GetMapping
    public String showSignup() {
        return "redirect:/signup.html";
    }

    @PostMapping
    public String signup(
            @Valid @ModelAttribute SignupDto dto
            , BindingResult br
            , @RequestParam(value = "agree", required = false) String agree
            , RedirectAttributes ra) {

        if (br.hasErrors()) {
            ra.addAttribute("error", "입력값을 확인해주세요.");
            return "redirect:/signup.html";
        }

        if(agree == null){
            ra.addAttribute("error", "약관에 동의해야 회원가입이 가능합니다.");
            return "redirect:/signup.html";
        }

        try {
            signupService.regist(dto);
            ra.addAttribute("message", "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/login.html";
        } catch (IllegalArgumentException e) {
            ra.addAttribute("error", e.getMessage());
            return "redirect:/signup.html";
        } catch (Exception ex) {
            throw ex;
            // ra.addAttribute("error", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            // return "redirect:/signup.html";
        }
    }
}
