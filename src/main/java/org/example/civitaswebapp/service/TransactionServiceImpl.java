package org.example.civitaswebapp.service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.example.civitaswebapp.domain.*;
import org.example.civitaswebapp.dto.transactions.TransactionCreatedDto;
import org.example.civitaswebapp.dto.transactions.TransactionStatusChangedDto;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @Autowired
    private MyUserService myUserService;

    // 👇 SECURITY HELPER
    private Union getCurrentUserUnion() {
        // Reload a fresh, request-scoped Union rather than reading it off the shared session
        // principal — the principal no longer carries a managed entity graph (see MyUserPrincipal).
        return myUserService.getLoggedInUser().getUnion();
    }

    @Transactional
    @Override
    public Transaction createTransaction(Member member, double amount, TransactionType type, MyUser createdByUser) {

        // 🛡️ Extra check: Ensure the member belongs to the same union as the creator
        if (!member.getUnion().getId().equals(createdByUser.getUnion().getId())) {
            throw new SecurityException("Cannot create transaction for a member of a different union.");
        }

        Transaction tx = Transaction.builder()
                .member(member)
                .union(createdByUser.getUnion()) // 👈 CRITICAL: Stamp the Union!
                .amount(amount)
                .currency("EUR")
                .status(TransactionStatus.PENDING)
                .type(type)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);

        TransactionCreatedDto dto = new TransactionCreatedDto(
                tx.getId(),
                tx.getMember().getId(),
                tx.getMember().getFirstName(),
                tx.getMember().getLastName(),
                tx.getAmount(),
                tx.getCreatedAt(),
                createdByUser.getId()
        );
        eventPublisher.publishEvent(dto);
        return tx;
    }

    @Transactional
    @Override
    public Transaction createSubscriptionTransaction(Member member, double amount) {
        // System context (scheduler): no logged-in user, so stamp the union from the member itself.
        LocalDateTime now = LocalDateTime.now();
        Transaction tx = Transaction.builder()
                .member(member)
                .union(member.getUnion())
                .amount(amount)
                .currency("EUR")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.MEMBERSHIP_FEE)
                .note("Automated subscription payment")
                .createdAt(now)
                .updatedAt(now)
                .build();

        transactionRepository.save(tx);

        // createdByUserId is null: a system-generated charge has no acting admin, so the
        // TransactionCreatedDto listener (which only notifies a specific creator) is a no-op here.
        eventPublisher.publishEvent(new TransactionCreatedDto(
                tx.getId(),
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                tx.getAmount(),
                tx.getCreatedAt(),
                null));
        return tx;
    }

    @Override
    public String generateStripePaymentLink(Transaction transaction) {
        Stripe.apiKey = stripeApiKey;

        try {
            if (transaction.getAmount() == null || transaction.getAmount() <= 0) {
                throw new IllegalArgumentException("Transaction amount must be greater than zero");
            }

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:8080/payment-success?transactionId=" + transaction.getId())
                    .setCancelUrl("http://localhost:8080/payment-cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount((long) (transaction.getAmount() * 100))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(transaction.getType().name())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("transactionId", String.valueOf(transaction.getId()))
                    .setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                    .putMetadata("transactionId", String.valueOf(transaction.getId()))
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            return session.getUrl();

        } catch (Exception e) {
            System.err.println("Stripe exception: " + e.getMessage());
            throw new RuntimeException("Stripe link creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void handleStripeWebhook(String payload, String sigHeader) {
        // Webhooks come from Stripe (System level), so no User Union check needed here.
        // We look up by ID, which is unique globally.
        System.out.println("Webhook received: " + payload);
    }

    @Override
    public List<Transaction> getTransactionsByMember(Member member) {
        return transactionRepository.findAllByMemberAndUnion(member, getCurrentUserUnion());
    }

    @Transactional
    @Override
    public void updateTransactionStatus(Long transactionId, TransactionStatus status) {
        Transaction transaction = transactionRepository.findByIdAndUnion(transactionId, getCurrentUserUnion())
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));

        TransactionStatus oldStatus = transaction.getStatus();
        transaction.setStatus(status);
        transactionRepository.save(transaction); // Union is already set, so safe to save

        TransactionStatusChangedDto dto = new TransactionStatusChangedDto(
                transactionId,
                transaction.getMember().getId(),
                oldStatus,
                status
        );
        eventPublisher.publishEvent(dto);
    }

    @Transactional
    @Override
    public void markTransactionAsCash(Long transactionId) {
        // Union-scoped lookup: an admin can only settle their own union's transactions.
        Transaction transaction = transactionRepository.findByIdAndUnion(transactionId, getCurrentUserUnion())
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only pending transactions can be marked as cash.");
        }

        TransactionStatus oldStatus = transaction.getStatus();
        transaction.setStatus(TransactionStatus.PAID_MANUALLY);
        transaction.setUpdatedAt(LocalDateTime.now());

        String cashNote = "Marked as cash/manual payment";
        String existingNote = transaction.getNote();
        transaction.setNote(existingNote == null || existingNote.isBlank()
                ? cashNote
                : existingNote + " | " + cashNote);

        transactionRepository.save(transaction);

        // Record the manual payment on the member (last payment date) — managed entity, flushed on commit.
        Member member = transaction.getMember();
        if (member != null) {
            member.setDateOfLastPayment(LocalDate.now());
        }

        eventPublisher.publishEvent(new TransactionStatusChangedDto(
                transactionId,
                member != null ? member.getId() : null,
                oldStatus,
                TransactionStatus.PAID_MANUALLY
        ));
    }

    @Transactional
    @Override
    public void updateTransactionStatusAsSystem(Long transactionId, TransactionStatus status) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        TransactionStatus oldStatus = transaction.getStatus();
        transaction.setStatus(status);
        transactionRepository.save(transaction);

        TransactionStatusChangedDto dto = new TransactionStatusChangedDto(
                transactionId,
                transaction.getMember().getId(),
                oldStatus,
                status
        );
        eventPublisher.publishEvent(dto);
    }

    @Override
    public Page<Transaction> getTransactions(Pageable pageable, String search,
                                             TransactionStatus status, TransactionType type) {

        Union currentUnion = getCurrentUserUnion();

        Specification<Transaction> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("union"), currentUnion));


            // Search filter
            if (search != null && !search.trim().isEmpty()) {
                String searchTerm = "%" + search.toLowerCase().trim() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("member").get("firstName")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("member").get("lastName")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("note")), searchTerm)
                );
                predicates.add(searchPredicate);
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable);
    }
}