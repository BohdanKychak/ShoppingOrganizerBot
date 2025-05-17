package org.example.service.support;

import lombok.extern.slf4j.Slf4j;
import org.example.model.entity.Purchase;
import org.example.model.session.UserSession;
import org.example.model.session.purchase.PurchaseLastHistory;
import org.example.repository.PurchaseRepository;
import org.example.service.response.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.example.enums.Currency.getCurrencyByStringUsed;
import static org.example.util.Constants.TRANSACTIONS_LIST_LIMIT;
import static org.example.util.Constants.TRANSACTIONS_TABLE_LIMIT;

@Slf4j
@Component
public class TransactionCacheService {

    private static final long CACHE_DURATION_MS = 60 * 60 * 1000; // 1 hour

    private static final Map<Long, List<Purchase>> PURCHASES_DATA = new ConcurrentHashMap<>();
    private static final Map<Long, ScheduledFuture<?>> CLEANUP_TASKS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private AccountService accountService;

    public List<Purchase> getPurchasesTableData(UserSession session) {
        log.info("Fetching purchases table data for familyId: {}", session.getFamilyId());
        if (session.getTransactionSession().isWasNewTransactionAdded()) {
            log.debug("New transaction was added. Invalidating cache for familyId: {}", session.getFamilyId());
            removeFromCache(session.getFamilyId());
            session.getTransactionSession().tableUpdated();
        }
        List<Purchase> purchases = checkAvailabilityOfDataAndReturn(session.getFamilyId());

        if (purchases == null || purchases.isEmpty()) {
            log.debug("No purchases found for familyId: {}", session.getFamilyId());
            return new ArrayList<>();
        }
        return new ArrayList<>(purchases.subList(0, Math.min(TRANSACTIONS_TABLE_LIMIT, purchases.size())));
    }

    public Purchase getLatestPurchase(UserSession session) {
        log.info("Fetching latest purchase for familyId: {}", session.getFamilyId());
        List<Purchase> purchases = checkAvailabilityOfDataAndReturn(session.getFamilyId());

        if (purchases == null || purchases.isEmpty()) {
            log.debug("No latest purchase found for familyId: {}", session.getFamilyId());
            return null;
        }
        return purchases.get(0);
    }

    public PurchaseLastHistory getPurchaseById(Long familyId, Long purchaseId) {
        log.info("Searching purchase by ID: {} for familyId: {}", purchaseId, familyId);
        List<Purchase> purchases = checkAvailabilityOfDataAndReturn(familyId);

        for (int i = 0; i < purchases.size(); i++) {
            Purchase purchase = purchases.get(i);
            if (purchase.getId().equals(purchaseId)) {
                log.info("Purchase found for familyId: {}", familyId);
                return new PurchaseLastHistory(purchase, i, purchases.size());
            }
        }
        log.warn("Purchase with ID: {} not found for familyId: {}", purchaseId, familyId);
        return null;
    }

    public void toNextPurchase(UserSession session) {
        log.info("Scrolling to next purchase for familyId: {}", session.getFamilyId());
        List<Purchase> purchases = checkAvailabilityOfDataAndReturn(session.getFamilyId());
        int index = session.getTransactionSession().getPurchaseLastHistory().getIndex() + 1;
        session.getTransactionSession().getPurchaseLastHistory().scroll(purchases.get(index), index);
    }

    public void toPreviousPurchase(UserSession session) {
        log.info("Scrolling to previous purchase for familyId: {}", session.getFamilyId());
        List<Purchase> purchases = checkAvailabilityOfDataAndReturn(session.getFamilyId());
        int index = session.getTransactionSession().getPurchaseLastHistory().getIndex() - 1;
        session.getTransactionSession().getPurchaseLastHistory().scroll(purchases.get(index), index);
    }

    public void removeFromCache(Long familyId) {
        log.info("Removing purchases from cache for familyId: {}", familyId);
        PURCHASES_DATA.remove(familyId);
        ScheduledFuture<?> task = CLEANUP_TASKS.remove(familyId);
        if (task != null) {
            task.cancel(false);
            log.debug("Scheduled cleanup task cancelled for familyId: {}", familyId);
        }
    }

    public void setPhoto(UserSession session, String photo) {
        log.info("Setting photo for latest purchase for familyId: {}", session.getFamilyId());
        PurchaseLastHistory currentPurchase = session.getTransactionSession().getPurchaseLastHistory();

        purchaseRepository.updateReceiptPhotoById(
                currentPurchase.getPurchase().getId(), photo);
        checkAvailabilityOfDataAndReturn(session.getFamilyId())
                .get(currentPurchase.getIndex()).setReceiptPhotoId(photo);
    }

    public void setDescription(UserSession session, String description) {
        log.info("Updating description for latest purchase for familyId: {}", session.getFamilyId());
        PurchaseLastHistory currentPurchase = session.getTransactionSession().getPurchaseLastHistory();

        purchaseRepository.updateDescriptionById(
                currentPurchase.getPurchase().getId(), description, false);
        Purchase purchase = checkAvailabilityOfDataAndReturn(session.getFamilyId())
                .get(currentPurchase.getIndex());
        purchase.setDescription(description);
        purchase.setSimpleDescription(false);
    }

    public void deletePurchase(UserSession session) {
        Purchase purchase = session.getTransactionSession().getPurchaseLastHistory().getPurchase();
        log.info("Deleting purchase with ID: {} for familyId: {}", purchase.getId(), session.getFamilyId());
        accountService.addMoneyToAccount(purchase.getAmount().doubleValue(),
                getCurrencyByStringUsed(purchase.getCurrency()), session.getFamilyId());
        purchaseRepository.delete(purchase);
        removeFromCache(session.getFamilyId());
    }

    private synchronized List<Purchase> checkAvailabilityOfDataAndReturn(Long familyId) {
        if (!PURCHASES_DATA.containsKey(familyId)) {
            log.debug("Cache miss for familyId: {}. Fetching from DB.", familyId);
            List<Purchase> purchasesData = purchaseRepository
                    .getListPurchaseByFamilyIdSortByTimeDESCWithLimit(familyId, TRANSACTIONS_LIST_LIMIT);

            PURCHASES_DATA.put(familyId, purchasesData);
            scheduleCacheEviction(familyId);
            return purchasesData;
        }
        log.debug("Cache hit for familyId: {}", familyId);
        return PURCHASES_DATA.getOrDefault(familyId, new ArrayList<>());
    }

    private void scheduleCacheEviction(Long familyId) {
        log.debug("Scheduling cache eviction for familyId: {} in {} ms", familyId, CACHE_DURATION_MS);
        ScheduledFuture<?> task = SCHEDULER.schedule(() -> {
            log.info("Evicting cache for familyId: {} due to timeout", familyId);
            PURCHASES_DATA.remove(familyId);
            CLEANUP_TASKS.remove(familyId);
        }, CACHE_DURATION_MS, TimeUnit.MILLISECONDS);

        ScheduledFuture<?> oldTask = CLEANUP_TASKS.put(familyId, task);
        if (oldTask != null) {
            oldTask.cancel(false);
            log.debug("Replaced old eviction task for familyId: {}", familyId);
        }
    }
}