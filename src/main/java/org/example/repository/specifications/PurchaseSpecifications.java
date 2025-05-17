package org.example.repository.specifications;

import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.example.model.entity.Purchase;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class PurchaseSpecifications {

    public static Specification<Purchase> familyIdEqual(Long familyId) {
        log.info("Add filter by familyId: {}", familyId);
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("familyId"), familyId);
    }

    public static Specification<Purchase> userChatIdOrDescription(Long chatId, String description) {
        log.info("Add filter equality by userChatId: {} or inequality by description: {}", chatId, description);
        return (root, query, criteriaBuilder) -> {
            Predicate chatMatch = criteriaBuilder.equal(root.get("userData").get("chatId"), chatId);
            Predicate descriptionMismatch = criteriaBuilder.or(
                    criteriaBuilder.notEqual(root.get("description"), description), // description DOES NOT match
                    criteriaBuilder.isNull(root.get("description")) // or description NULL
            );
            log.debug("Predicates - chatMatch: {}, descriptionMismatch: {}", chatMatch, descriptionMismatch);

            // If chatId matches → return all records without filtering
            // If chatId DOES NOT match → filter by description
            return criteriaBuilder.or(chatMatch, descriptionMismatch);
        };
    }

    // WHERE p.timePurchase >= ':start' AND p.timePurchase <= ':end'
    public static Specification<Purchase> timeBetween(LocalDateTime start, LocalDateTime end) {
        log.info("Add filter by timePurchase: {} - {}", start, end);
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("timePurchase"), start, end);
    }

    // WHERE p.userData.chatId = :chatId
    public static Specification<Purchase> userChatIdEqual(Long chatId) {
        log.info("Add equality filter by userChatId: {}", chatId);
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("userData").get("chatId"), chatId);
    }

    // WHERE p.userData.chatId != :chatId
    public static Specification<Purchase> userChatIdNotEqual(Long chatId) {
        log.info("Add inequality filter by userChatId: {}", chatId);
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.notEqual(root.get("userData").get("chatId"), chatId);
    }

    // WHERE (p.currency IN (:currencies))
    public static Specification<Purchase> currencyIn(List<String> currencies) {
        log.info("Add filter by currency: {}", currencies);
        return (root, query, criteriaBuilder) ->
                root.get("currency").in(currencies);
    }

    // WHERE p.amount >= ':min' AND p.amount <= ':max'
    public static Specification<Purchase> amountBetween(BigDecimal min, BigDecimal max) {
        log.info("Add filter by amount: {} - {}", min, max);
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("amount"), min, max);
    }

    // ORDER BY ABS(p.amount - :targetAmount)
    public static Specification<Purchase> orderByClosestAmount(BigDecimal targetAmount) {
        log.info("Add filter by closest amount: {}", targetAmount);
        return (root, query, criteriaBuilder) -> {
            assert query != null;
            query.orderBy(criteriaBuilder.asc(criteriaBuilder.abs(
                    criteriaBuilder.diff(root.get("amount"), targetAmount)
            )));
            return null; // it's just sort
        };
    }

    public static Sort getTimePurchaseDescSort() {
        log.info("Add sort by timePurchase DESC");
        return Sort.by(Sort.Order.desc("timePurchase"));
    }
}
