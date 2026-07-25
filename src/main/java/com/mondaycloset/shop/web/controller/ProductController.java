package com.mondaycloset.shop.web.controller;

import com.mondaycloset.shop.service.CategoryService;
import com.mondaycloset.shop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private static final int PAGE_SIZE = 12;

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/products")
    public String list(@RequestParam(required = false) Long category,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<?> productPage = productService.getProductList(category, keyword, PageRequest.of(page, PAGE_SIZE));

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("categories", categoryService.getAllOrdered());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("keyword", keyword);
        return "product/list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getProductDetail(id));
        return "product/detail";
    }
}
