package co.com.crediya.usecase.getapplicationsbystatus;

import co.com.crediya.model.clientrest.ClientRest;
import co.com.crediya.model.clientrest.gateways.ClientRepository;
import co.com.crediya.model.loanapplication.LoanApplication;
import co.com.crediya.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.crediya.model.loantype.LoanType;
import co.com.crediya.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.model.status.Status;
import co.com.crediya.model.status.gateways.StatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetApplicationsByStatusTest {
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private StatusRepository statusRepository;
    @Mock
    private LoanTypeRepository loanTypeRepository;
    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    private GetApplicationsByStatusUseCase getApplicationsByStatusUseCase;
    private Status statusToTest;
    private LoanApplication loanApplicationToTest;
    private ClientRest clientToTest;
    private LoanType loanTypeToTest;

    @BeforeEach
    void setUp() {
        getApplicationsByStatusUseCase = new GetApplicationsByStatusUseCase(clientRepository, statusRepository, loanTypeRepository, loanApplicationRepository);
        statusToTest = new Status();
        statusToTest.setId(1);
        statusToTest.setName("PENDING");

        loanApplicationToTest = new LoanApplication();
        loanApplicationToTest.setId(100);
        loanApplicationToTest.setAmount(BigDecimal.valueOf(1000));
        loanApplicationToTest.setTerm(12);
        loanApplicationToTest.setEmail("test@mail.com");
        loanApplicationToTest.setDocumentId("123");
        loanApplicationToTest.setStateId(1);
        loanApplicationToTest.setTypeLoanId(1);

        clientToTest = new ClientRest();
        clientToTest.setId(1);
        clientToTest.setEmail("test@mail.com");
        clientToTest.setName("Juan Test");
        clientToTest.setDocumentId("241256326");
        clientToTest.setBaseSalary(BigDecimal.valueOf(3000000));

        loanTypeToTest = new LoanType();
        loanTypeToTest.setId(1);
        loanTypeToTest.setName("PERSONAL");
        loanTypeToTest.setTaxRate(BigDecimal.valueOf(10));

    }

    @Test
    @DisplayName("Should return a pageable of ApplicationClient when status exists and applications are found")
    void testGetApplicationsByStatusSuccess() {
        when(statusRepository.findByName(anyString()))
                .thenReturn(Mono.just(statusToTest));
        when(clientRepository.findAllByEmail(List.of(loanApplicationToTest.getEmail())))
                .thenReturn(Flux.just(clientToTest));
        when(statusRepository.findAllById(List.of(loanApplicationToTest.getStateId())))
                .thenReturn(Flux.just(statusToTest));
        when(loanTypeRepository.findAllById(List.of(loanApplicationToTest.getTypeLoanId())))
                .thenReturn(Flux.just(loanTypeToTest));
        when(loanApplicationRepository.findAllApplicationsByStatus(statusToTest.getId(), 0, 10))
                .thenReturn(Flux.just(loanApplicationToTest));

        StepVerifier.create(getApplicationsByStatusUseCase.getApplicationsByStatus("PENDING", 0, 10))
                .assertNext(pageable -> {
                    assertEquals(1, pageable.getContent().size());
                    assertEquals(0, pageable.getPage());
                    assertEquals(10, pageable.getSize());
                })
                .verifyComplete();
    }


}
