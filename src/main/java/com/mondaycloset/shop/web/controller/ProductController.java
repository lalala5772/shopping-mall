package com.mondaycloset.shop.web.controller;

import com.mondaycloset.shop.service.BedrockImageEmbeddingService;
import com.mondaycloset.shop.service.CategoryService;
import com.mondaycloset.shop.service.ProductService;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ProductController {

    private static final int PAGE_SIZE = 12;

    private final ProductService productService;
    private final CategoryService categoryService;
    private final BedrockImageEmbeddingService embeddingService;

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
        model.addAttribute("imageSearchEnabled", embeddingService.isEnabled());
        return "product/list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getProductDetail(id));
        return "product/detail";
    }

    /** 업로드한 이미지와 시각적으로 유사한 상품을 찾아 목록 화면에 그대로 재사용해 보여준다. */
    @PostMapping("/products/search/image")
    public String searchByImage(@RequestParam("image") MultipartFile image, Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!embeddingService.isEnabled()) {
            return "redirect:/products";
        }
        if (image.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "검색할 이미지를 선택해 주세요.");
            return "redirect:/products";
        }
        List<?> results;
        try {
            results = productService.searchBySimilarImage(image.getBytes());
        } catch (IOException e) {
            log.warn("[ProductController] 이미지 검색 업로드 파일 읽기 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "이미지를 읽는 중 오류가 발생했습니다.");
            return "redirect:/products";
        }

        model.addAttribute("products", results);
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", 1);
        model.addAttribute("categories", categoryService.getAllOrdered());
        model.addAttribute("selectedCategory", null);
        model.addAttribute("keyword", null);
        model.addAttribute("imageSearchEnabled", true);
        model.addAttribute("imageSearchActive", true);
        if (results.isEmpty()) {
            model.addAttribute("errorMessage", "업로드하신 이미지와 유사한 상품을 찾지 못했습니다.");
        }
        return "product/list";
    }
}
