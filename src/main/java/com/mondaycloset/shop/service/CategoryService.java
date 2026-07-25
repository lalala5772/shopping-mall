package com.mondaycloset.shop.service;

import com.mondaycloset.shop.domain.category.Category;
import com.mondaycloset.shop.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAllOrdered() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc();
    }
}
