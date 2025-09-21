package co.com.crediya.usecase;

import co.com.crediya.model.clientrest.ClientRest;
import co.com.crediya.model.clientrest.gateways.ClientRepository;
import co.com.crediya.model.clientrest.gateways.TokenGateway;
import co.com.crediya.model.exception.BusinessException;
import co.com.crediya.model.loanapplication.LoanApplication;
import co.com.crediya.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.crediya.model.loantype.LoanType;
import co.com.crediya.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.model.status.Status;
import co.com.crediya.model.status.gateways.StatusRepository;
import co.com.crediya.usecase.loanapplication.LoanApplicationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationUseCaseTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;
    @Mock
    private LoanTypeRepository loanTypeRepositoryMock;
    @Mock
    private StatusRepository statusRepositoryMock;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private TokenGateway tokenGateway;

    private LoanApplicationUseCase loanApplicationUseCaseMock;
    private LoanApplication loanApplyToTest;
    private String loanTypeName;
    private LoanType loanType;
    private Status status;


    @BeforeEach
    void setUp() {
        
        loanApplicationUseCaseMock = new LoanApplicationUseCase(loanApplicationRepository, loanTypeRepositoryMock, statusRepositoryMock, clientRepository, tokenGateway);

        loanTypeName = "Basic";

        loanApplyToTest = LoanApplication.builder()
                .id(1)
                .amount(new BigDecimal("2500.22"))
                .term(25)
                .email("test@test.com")
                .documentId("1234567891")
                .typeLoanId(1)
                .build();

        loanType = LoanType.builder()
                .id(1)
                .name(loanTypeName)
                .maxAmount(new BigDecimal("5000.00"))
                .minAmount(new BigDecimal("1000.00"))
                .build();

        status = Status.builder()
                .id(1)
                .name("PENDING")
                .description("Loan application is pending review")
                .build();

    }


    @Test
    @DisplayName("Should create loan application successfully when data is valid")
    void CreateLoanApplicationSuccesfully() {
        when(loanTypeRepositoryMock.findByName("Basic")).thenReturn(Mono.just(loanType));
        when(statusRepositoryMock.findByName("PENDING")).thenReturn(Mono.just(status));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenReturn(Mono.just(loanApplyToTest));
        when(tokenGateway.getClaim("documentId")).thenReturn(Mono.just("1234567891"));
        when(clientRepository.existsByEmailAndDocument(loanApplyToTest.getEmail(), loanApplyToTest.getDocumentId())).thenReturn(Mono.just(new ClientRest()));

        StepVerifier.create(loanApplicationUseCaseMock.save(loanApplyToTest, loanTypeName))
                .expectNextMatches(loanApply ->
                        loanApply.getId() != null &&
                                loanApply.getAmount().equals(loanApplyToTest.getAmount()) &&
                                loanApply.getEmail().equals(loanApplyToTest.getEmail())
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw StatusNotFoundException when status not found")
    void testStatusNotFound() {
        when(loanTypeRepositoryMock.findByName("Basic")).thenReturn(Mono.just(new LoanType()));
        when(tokenGateway.getClaim("documentId")).thenReturn(Mono.just("1234567891"));
        when(clientRepository.existsByEmailAndDocument(loanApplyToTest.getEmail(), loanApplyToTest.getDocumentId())).thenReturn(Mono.just(new ClientRest()));
        when(statusRepositoryMock.findByName("PENDING")).thenReturn(Mono.error(new BusinessException("BSS_02", "Status not found")));
        StepVerifier.create(loanApplicationUseCaseMock.save(loanApplyToTest, loanTypeName))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw LoanTypeNotFoundException when loan type does not exist")
    void testLoanTypeNotFound() {
        when(tokenGateway.getClaim("documentId")).thenReturn(Mono.just("1234567891"));
        when(clientRepository.existsByEmailAndDocument(loanApplyToTest.getEmail(), loanApplyToTest.getDocumentId())).thenReturn(Mono.just(new ClientRest()));
        when(loanTypeRepositoryMock.findByName("InvalidType")).thenReturn(Mono.error(new BusinessException("BSS_01", "Loan type not found")));
        when(statusRepositoryMock.findByName("PENDING")).thenReturn(Mono.just(status));

        StepVerifier.create(loanApplicationUseCaseMock.save(loanApplyToTest, "InvalidType"))
                .expectError(BusinessException.class)
                .verify();
    }



}

