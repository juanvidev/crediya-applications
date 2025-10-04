package co.com.crediya.usecase.loanapplication;

import co.com.crediya.model.applicationclient.ApplicationClient;
import co.com.crediya.model.applicationclient.LoanApplicationCreator;
import co.com.crediya.model.clientrest.gateways.ClientRepository;
import co.com.crediya.model.clientrest.gateways.TokenGateway;
import co.com.crediya.model.loanapplication.LoanApplication;
import co.com.crediya.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.crediya.model.loantype.LoanType;
import co.com.crediya.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.model.notification.gateways.NotificationGateway;
import co.com.crediya.model.status.gateways.StatusRepository;
import co.com.crediya.model.exception.BusinessException;
import co.com.crediya.usecase.getapplicationsbystatus.GetApplicationsByStatusUseCase;
import co.com.crediya.usecase.utils.LoanOperations;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RequiredArgsConstructor
public class LoanApplicationUseCase {
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final StatusRepository statusRepository;
    private final ClientRepository clientRepository;
    private final TokenGateway tokenGateway;
    private final NotificationGateway notificationGateway;

    public Mono<LoanApplication> save(LoanApplication loanApplication, String loanTypeName) {

        return tokenGateway.getClaim("documentId")
                .flatMap(documentIdToken -> documentIdToken.equals(loanApplication.getDocumentId())
                        ? Mono.just(documentIdToken)
                        : Mono.error(new BusinessException("BSS_04", "You do not have permission to create a loan application for this document ID")))
                .flatMap(documentId ->
                    clientRepository.existsByEmailAndDocument(
                        loanApplication.getEmail(),
                        documentId)
                )
                .switchIfEmpty(Mono.error(new BusinessException("BSS_03", "Client does not exist")))
                .then(loanTypeRepository.findByName(loanTypeName)
                        .switchIfEmpty(Mono.error(new BusinessException("BSS_01", "Loan type not found"))))
                .zipWith(statusRepository.findByName("PENDING")
                        .switchIfEmpty(Mono.error(new BusinessException("BSS_02", "Status not found"))))
                .flatMap(tuple -> {
                    var loanType = tuple.getT1();
                    var status = tuple.getT2();

                    LoanApplication newLoanApplication = loanApplication.toBuilder()
                            .typeLoanId(loanType.getId())
                            .stateId(status.getId())
                            .build();
                    return loanApplicationRepository.save(newLoanApplication);
                });
    }

    public Mono<ApplicationClient> updateStatusLoanApplication(Integer idFromPath, String statusFromBody) {

        System.out.println("Updating status for LoanApplication with id: " + idFromPath + " to status: " + statusFromBody);

        return loanApplicationRepository.findById(idFromPath)
                .switchIfEmpty(Mono.error(new BusinessException("LAP_NOT_FOUND", "Loan application not found")))
                .flatMap(loanApplication -> statusRepository.findByName(statusFromBody)
                        .switchIfEmpty(Mono.error(new BusinessException("STATUS_NOT_FOUND", "Status not found")))
                        .flatMap(status -> {
                            LoanApplication updatedLoanApplication = loanApplication.toBuilder()
                                    .stateId(status.getId())
                                    .build();

                            return loanApplicationRepository.save(updatedLoanApplication);
                        })
                        .switchIfEmpty(
                                Mono.error(new BusinessException("LAP_NOT_UPDATED", "Failed to update loan application status"))
                        )
                        .flatMap(savedLoanApplication ->
                                loanTypeRepository.findById(savedLoanApplication.getTypeLoanId())
                                        .switchIfEmpty(Mono.error(new BusinessException("LOAN_TYPE_NOT_FOUND", "Loan type not found")))
                                        .flatMap(loanType ->
                                                clientRepository.findAllByEmail(List.of(savedLoanApplication.getEmail()))
                                                        .next()
                                                        .switchIfEmpty(Mono.error(new BusinessException("CLIENT_NOT_FOUND", "Client not found")))
                                                        .flatMap(client -> {
                                                            BigDecimal monthlyPayment = LoanOperations.calculateMonthlyPayment(
                                                                    savedLoanApplication.getAmount(),
                                                                    loanType.getTaxRate(),
                                                                    savedLoanApplication.getTerm()
                                                            );
                                                            System.out.println("Calculated monthly payment: " + monthlyPayment);

                                                            LoanApplicationCreator loanApplicationData = LoanApplicationCreator.builder()
                                                                    .id(savedLoanApplication.getId())
                                                                    .amount(savedLoanApplication.getAmount())
                                                                    .term(savedLoanApplication.getTerm())
                                                                    .email(savedLoanApplication.getEmail())
                                                                    .documentId(savedLoanApplication.getDocumentId())
                                                                    .state(statusFromBody)
                                                                    .typeLoan(loanType.getName())
                                                                    .build();

                                                            ApplicationClient applicationClient = ApplicationClient.builder()
                                                                    .loanApplicationCreator(loanApplicationData)
                                                                    .clientRest(client)
                                                                    .totalInterest(loanType.getTaxRate())
                                                                    .totalMonthlyPayment(monthlyPayment.setScale(2, RoundingMode.HALF_UP))
                                                                    .build();

                                                            return notificationGateway.send(applicationClient)
                                                                    .thenReturn(applicationClient);
                                                        })
                                        )

                        )

                );
    }
}
