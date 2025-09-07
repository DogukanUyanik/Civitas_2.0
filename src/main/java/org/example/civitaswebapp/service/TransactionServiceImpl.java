package org.example.civitaswebapp.service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;


@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;


    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @Override
    public Transaction createTransaction(Member member, double amount, TransactionType type) {
        Transaction tx = Transaction.builder()
                .member(member)
                .amount(amount)
                .currency("EUR")
                .status(TransactionStatus.PENDING)
                .type(type)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return transactionRepository.save(tx);
    }

    @Override
    public String generateStripePaymentLink(Transaction transaction) {
        Stripe.apiKey = stripeApiKey;

        try {
            if (transaction.getAmount() == null || transaction.getAmount() <= 0) {
                throw new IllegalArgumentException("Transaction amount must be greater than zero");
            }

            long amountInCents = Math.round(transaction.getAmount() * 100);

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
        System.out.println("Webhook received: " + payload);

    }

    @Override
    public List<Transaction> getTransactionsByMember(Member member) {
        return transactionRepository.findAllByMember(member);
    }

    @Override
    public void updateTransactionStatus(Long transactionId, TransactionStatus status) {
        System.out.println("=== TRANSACTION UPDATE DEBUG START ===");
        System.out.println("Updating transaction ID: " + transactionId);
        System.out.println("New status: " + status);

        try {
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> {
                        System.err.println("❌ Transaction not found with ID: " + transactionId);
                        return new IllegalArgumentException("Transaction not found: " + transactionId);
                    });

            System.out.println("✅ Found transaction: " + transaction.getId());
            System.out.println("Current status: " + transaction.getStatus());
            System.out.println("Amount: " + transaction.getAmount());
            System.out.println("Member: " + (transaction.getMember() != null ? transaction.getMember().getId() : "null"));

            TransactionStatus oldStatus = transaction.getStatus();
            transaction.setStatus(status);
            transaction.setUpdatedAt(LocalDateTime.now());

            Transaction savedTransaction = transactionRepository.save(transaction);

            System.out.println("✅ Transaction updated successfully!");
            System.out.println("Status changed from " + oldStatus + " to " + savedTransaction.getStatus());
            System.out.println("Updated at: " + savedTransaction.getUpdatedAt());

        } catch (Exception e) {
            System.err.println("❌ Error updating transaction status: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw so the webhook knows there was an error
        }

        System.out.println("=== TRANSACTION UPDATE DEBUG END ===");
    }


    public Page<Transaction> getTransactions(Pageable pageable, String search,
                                             TransactionStatus status, TransactionType type) {

        Specification<Transaction> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search filter - search in member name, payment ID, and note
            if (search != null && !search.trim().isEmpty()) {
                String searchTerm = "%" + search.toLowerCase().trim() + "%";
                Predicate memberSearch = criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("member").get("firstName")),
                                searchTerm
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("member").get("lastName")),
                                searchTerm
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        criteriaBuilder.concat(
                                                criteriaBuilder.concat(root.get("member").get("firstName"), " "),
                                                root.get("member").get("lastName")
                                        )
                                ),
                                searchTerm
                        )
                );

                if (root.get("paymentId") != null) {
                    memberSearch = criteriaBuilder.or(
                            memberSearch,
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("paymentId")),
                                    searchTerm
                            )
                    );
                }

                if (root.get("note") != null) {
                    memberSearch = criteriaBuilder.or(
                            memberSearch,
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("note")),
                                    searchTerm
                            )
                    );
                }

                predicates.add(memberSearch);
            }

            // Status filter
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Type filter
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable);
    }


}
