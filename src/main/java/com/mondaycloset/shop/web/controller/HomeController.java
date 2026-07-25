package com.mondaycloset.shop.web.controller;

import com.mondaycloset.shop.service.CategoryService;
import com.mondaycloset.shop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("categories", categoryService.getAllOrdered());
        model.addAttribute("products", productService.getProductList(null, null, PageRequest.of(0, 8)).getContent());
        return "home";
    }
}
