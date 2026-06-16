package com.smartinventory.service;

import com.smartinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryIntelligenceService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<String> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return productRepository.findAllCategories();
        }
        return productRepository.searchCategories(keyword);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> suggestions(String productName) {
        String name = productName == null ? "" : productName.toLowerCase(Locale.ROOT);
        String suggested = "General";
        if (name.matches(".*(laptop|phone|charger|camera|monitor|keyboard).*")) suggested = "Electronics";
        else if (name.matches(".*(shirt|shoe|bag|denim|jacket).*")) suggested = "Apparel";
        else if (name.matches(".*(rice|oil|tea|coffee|snack|spice).*")) suggested = "Grocery";
        else if (name.matches(".*(drill|hammer|tool|screw|wrench).*")) suggested = "Tools";

        String finalSuggested = suggested;
        List<String> existing = search(suggested);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("suggestedCategory", existing.isEmpty() ? suggested : existing.get(0));
        result.put("existingMatches", existing);
        result.put("duplicateRisk", existing.stream().anyMatch(c -> c.equalsIgnoreCase(finalSuggested)));
        return result;
    }
}
