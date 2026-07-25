package com.mondaycloset.shop.web.controller.admin;

import com.mondaycloset.shop.service.BedrockImageEmbeddingService;
import com.mondaycloset.shop.service.CategoryService;
import com.mondaycloset.shop.service.admin.AdminProductService;
import com.mondaycloset.shop.web.dto.ProductDtos.ProductForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 관리자 상품 관리. 클래스 레벨 접근 제어는 SecurityConfig의 /admin/** -> ROLE_ADMIN 규칙으로 처리한다. */
@Controller
@RequiredArgsConstructor
public class AdminProductController {

    private static final int PAGE_SIZE = 15;

    private final AdminProductService adminProductService;
    private final CategoryService categoryService;
    private final BedrockImageEmbeddingService embeddingService;

    @GetMapping("/admin/products")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        var productPage = adminProductService.getProductPage(PageRequest.of(page, PAGE_SIZE));
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("imageSearchEnabled", embeddingService.isEnabled());
        return "admin/product-list";
    }

    @GetMapping("/admin/products/new")
    public String newForm(Model model) {
        model.addAttribute("productForm", ProductForm.empty());
        model.addAttribute("categories", categoryService.getAllOrdered());
        return "admin/product-form";
    }

    @GetMapping("/admin/products/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("productForm", adminProductService.getProductForm(id));
        model.addAttribute("categories", categoryService.getAllOrdered());
        return "admin/product-form";
    }

    @PostMapping("/admin/products")
    public String create(@Valid @ModelAttribute("productForm") ProductForm form,
                          BindingResult bindingResult, Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllOrdered());
            return "admin/product-form";
        }
        Long productId = adminProductService.create(form);
        adminProductService.embedProductImage(productId);
        redirectAttributes.addFlashAttribute("successMessage", "상품이 등록되었습니다.");
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("productForm") ProductForm form,
                          BindingResult bindingResult, Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllOrdered());
            return "admin/product-form";
        }
        adminProductService.update(id, form);
        adminProductService.embedProductImage(id);
        redirectAttributes.addFlashAttribute("successMessage", "상품이 수정되었습니다.");
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/backfill-embeddings")
    public String backfillEmbeddings(RedirectAttributes redirectAttributes) {
        int count = adminProductService.backfillEmbeddings();
        redirectAttributes.addFlashAttribute("successMessage",
                count + "개 상품의 이미지 검색용 임베딩을 새로 계산했습니다.");
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{id}/hide")
    public String hide(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminProductService.hide(id);
        redirectAttributes.addFlashAttribute("successMessage", "상품이 판매 중지(숨김) 처리되었습니다.");
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{id}/show")
    public String show(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminProductService.reactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", "상품이 다시 노출됩니다.");
        return "redirect:/admin/products";
    }
}
