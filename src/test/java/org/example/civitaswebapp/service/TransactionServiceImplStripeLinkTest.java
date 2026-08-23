package org.example.civitaswebapp.service;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.domain.Union;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Guards against the success/cancel URLs regressing back to a hardcoded {@code localhost:8080}
 * literal — that broke the Stripe redirect on mobile and in production, since a member's phone
 * cannot reach the union staff's local machine. The redirect target must come from the
 * configurable {@code app.base-url} property instead.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplStripeLinkTest {

    private final TransactionServiceImpl transactionService = new TransactionServiceImpl();

    private MockedStatic<Session> sessionStatic;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transactionService, "stripeApiKey", "sk_test_dummy");
        sessionStatic = mockStatic(Session.class);
    }

    @AfterEach
    void tearDown() {
        sessionStatic.close();
    }

    private Transaction transaction() {
        Union union = Union.builder().id(UUID.randomUUID()).name("Demo").build();
        return Transaction.builder()
                .id(5L)
                .amount(25.0)
                .currency("EUR")
                .type(TransactionType.MEMBERSHIP_FEE)
                .status(TransactionStatus.PENDING)
                .union(union)
                .build();
    }

    @Test
    void buildsSuccessAndCancelUrlsFromConfiguredBaseUrl() throws Exception {
        ReflectionTestUtils.setField(transactionService, "appBaseUrl", "https://civitas.example.com");

        Session fakeSession = new Session();
        fakeSession.setUrl("https://checkout.stripe.com/fake");
        sessionStatic.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(fakeSession);

        transactionService.generateStripePaymentLink(transaction());

        ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
        sessionStatic.verify(() -> Session.create(captor.capture()));

        SessionCreateParams params = captor.getValue();
        assertThat(params.getSuccessUrl()).isEqualTo("https://civitas.example.com/payment-success?transactionId=5");
        assertThat(params.getCancelUrl()).isEqualTo("https://civitas.example.com/payment-cancel");
        assertThat(params.getSuccessUrl()).doesNotContain("localhost");
        assertThat(params.getCancelUrl()).doesNotContain("localhost");
    }
}
