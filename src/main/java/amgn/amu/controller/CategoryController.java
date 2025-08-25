package amgn.amu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CategoryController {
    @GetMapping("/digital")
    public String showDigital() {
        return "redirect:/category/digital.html";
    }

    @GetMapping("/menClothes")
    public String showMenClothes() {
        return "redirect:/category/menClothes.html";
    }

    @GetMapping("/womenClothes")
    public String showWomenClothes() {
        return "redirect:/category/womenClothes.html";
    }

    @GetMapping("/menShoes")
    public String showMenShoes() {
        return "redirect:/category/menShoes.html";
    }

    @GetMapping("/womenShoes")
    public String showWomenShoes() {
        return "redirect:/category/womenShoes.html";
    }

    @GetMapping("/homeAppliances")
    public String showHomeAppliances() {
        return "redirect:/category/homeAppliances.html";
    }

    @GetMapping("/voucher")
    public String showVoucher() {
        return "redirect:/category/voucher.html";
    }

    @GetMapping("/furniture")
    public String showFurniture() {
        return "redirect:/category/furniture.html";
    }
}
