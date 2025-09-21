package co.com.crediya.r2dbc;

import co.com.crediya.model.TransactionalOperatorGateway;
import co.com.crediya.model.loanapplication.LoanApplication;
import co.com.crediya.r2dbc.entity.LoanApplicationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationReactiveRepositoryAdapterTest {

    @Mock
    LoanApplicationReactiveRepositoryAdapter repositoryAdapter;

    @Mock
    TransactionalOperatorGateway transactionalOperatorGateway;

    @Mock
    LoanApplicationReactiveRepository repository;

    @Mock
    ObjectMapper mapper;

    private LoanApplication domain;
    private LoanApplicationEntity entity;

    @BeforeEach
    void setup() {

        repositoryAdapter = new LoanApplicationReactiveRepositoryAdapter(repository, mapper, transactionalOperatorGateway);

        domain = LoanApplication.builder()
                .amount(new BigDecimal("2500.22"))
                .term(25)
                .email("test@test.com")
                .typeLoanId(1)
                .build();

        entity = new LoanApplicationEntity(
                domain.getId(),
                domain.getAmount(),
                domain.getTerm(),
                domain.getEmail(),
                domain.getDocumentId(),
                domain.getStateId(),
                domain.getTypeLoanId()
        );

    }

    @Test
    @DisplayName("Should register loanApply successfully")
    void testRegisterLoanApplicationSuccessfully() {
        when(mapper.map(domain, LoanApplicationEntity.class)).thenReturn(entity);
        when(mapper.map(entity, LoanApplication.class)).thenReturn(domain);

        when(transactionalOperatorGateway.executeOne(any()))
                .thenAnswer(invocation -> ((Supplier<Mono<LoanApplication>>) invocation.getArgument(0)).get());
        when(repository.save(entity)).thenReturn(Mono.just(entity));


        StepVerifier.create(repositoryAdapter.save(domain))
                .expectNextMatches(loanApply->loanApply.getEmail().equals("test@test.com") &&
                        loanApply.getAmount().equals(new BigDecimal("2500.22")))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fail to sign up loanApply when repository throws error")
    void testSignUpShouldFail() {
        when(mapper.map(domain, LoanApplicationEntity.class)).thenReturn(entity);
        when(transactionalOperatorGateway.executeOne(any()))
                .thenAnswer(invocation -> ((Supplier<Mono<LoanApplication>>) invocation.getArgument(0)).get());

        when(repository.save(entity)).thenReturn(Mono.error(new RuntimeException("Error registering loanApply")));

        StepVerifier.create(repositoryAdapter.save(domain))
                .expectErrorMessage("Error registering loanApply")
                .verify();
    }

    @Test
    @DisplayName("Should fail to sign up loanApply when entity is empty")
    void testSignUpShouldFailWithEmptyValue() {
        when(mapper.map(domain, LoanApplicationEntity.class)).thenReturn(entity);

        when(transactionalOperatorGateway.executeOne(any()))
                .thenAnswer(invocation -> ((Supplier<Mono<LoanApplication>>) invocation.getArgument(0)).get());

        when(repository.save(entity)).thenReturn(Mono.error(new RuntimeException("Entity is empty")));

        StepVerifier.create(repositoryAdapter.save(domain))
                .expectErrorMessage("Entity is empty")
                .verify();
    }

    @Test
    @DisplayName("Should fail to sign up loanApply when entity is null")
    void testSignUpShouldFailWithNull() {
        when(mapper.map(domain, LoanApplicationEntity.class)).thenReturn(null);
        when(transactionalOperatorGateway.executeOne(any()))
                .thenAnswer(invocation -> ((Supplier<Mono<LoanApplication>>) invocation.getArgument(0)).get());

        when(repository.save(null)).thenReturn(Mono.error(new RuntimeException("Entity is null")));

        StepVerifier.create(repositoryAdapter.save(domain))
                .expectErrorMessage("Entity is null")
                .verify();
    }

}