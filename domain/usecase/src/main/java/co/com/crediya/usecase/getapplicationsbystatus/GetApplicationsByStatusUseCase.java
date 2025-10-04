package co.com.crediya.usecase.getapplicationsbystatus;

import co.com.crediya.model.applicationclient.ApplicationClient;
import co.com.crediya.model.applicationclient.LoanApplicationCreator;
import co.com.crediya.model.applicationclient.Pageable;
import co.com.crediya.model.clientrest.ClientRest;
import co.com.crediya.model.clientrest.gateways.ClientRepository;
import co.com.crediya.model.exception.BusinessException;
import co.com.crediya.model.loanapplication.LoanApplication;
import co.com.crediya.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.crediya.model.loantype.LoanType;
import co.com.crediya.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.model.status.Status;
import co.com.crediya.model.status.gateways.StatusRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetApplicationsByStatusUseCase {

    private final ClientRepository clientRepository;
    private final StatusRepository statusRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    public Mono<Pageable<ApplicationClient>> getApplicationsByStatus(String status, int page, int size) {
        return statusRepository.findByName(status)
                .doOnNext(res -> System.out.println("Fetched Status: " + res.getName()))
                .switchIfEmpty(Mono.error(new BusinessException("STATUS_NOT_FOUND", "Status not found")))
                .flatMap(state -> loanApplicationRepository.findAllApplicationsByStatus(state.getId(), page, size)
                        .collectList()
                        .doOnNext(res -> System.out.println("Fetched Loan Application: " + res.size()))
                        .flatMap(loans -> {

                            if(loans.isEmpty()) return Mono.just(List.<ApplicationClient>of());

                            List<String> clientEmails = loans.stream()
                                    .map(LoanApplication::getEmail)
                                    .distinct()
                                    .toList();

                            List<Integer> statusIds = loans.stream()
                                    .map(LoanApplication::getStateId)
                                    .distinct()
                                    .toList();

                            List<Integer> loanTypeIds = loans.stream()
                                    .map(LoanApplication::getTypeLoanId)
                                    .distinct()
                                    .toList();

                            Mono<Map<String, ClientRest>> clientsMapMono = clientRepository.findAllByEmail(clientEmails)
                                    .collectMap(ClientRest::getEmail);

                            Mono<Map<Integer, String>> statusesMapMono = statusRepository.findAllById(statusIds)
                                    .collectMap(Status::getId, Status::getName);

                            Mono<Map<Integer, LoanType>> loanTypesMapMono = loanTypeRepository.findAllById(loanTypeIds)
                                    .collectMap(LoanType::getId);

                            return Mono.zip(clientsMapMono, statusesMapMono, loanTypesMapMono)
                                    .map(tuple -> map(loans, tuple.getT1(), tuple.getT2(), tuple.getT3()));
                        })
                        .map(applications ->
                                Pageable.<ApplicationClient>builder()
                                        .page(page)
                                        .size(size)
                                        .totalElements(applications.size())
                                        .totalPages((int) Math.ceil((double) applications.size() / size))
                                        .content(applications)
                                        .build()
                        )
                );

    }

    public List<ApplicationClient> map(List<LoanApplication> loans,
                                       Map<String, ClientRest> clients,
                                       Map<Integer, String> statuses,
                                       Map<Integer, LoanType> loanTypes) {

        return loans.stream().map(loan -> {
            LoanType loanType = loanTypes.get(loan.getTypeLoanId());

            LoanApplicationCreator data = LoanApplicationCreator.builder()
                    .id(loan.getId())
                    .amount(loan.getAmount())
                    .term(loan.getTerm())
                    .email(loan.getEmail())
                    .documentId(loan.getDocumentId())
                    .state(statuses.get(loan.getStateId()))
                    .typeLoan(loanType.getName())
                    .build();
            ClientRest clientData = clients.get(loan.getEmail());
            System.out.println("Mapping Loan Application ID: " + loan.getId() + " for Client: " + clientData.getName());

            return ApplicationClient.builder()
                    .loanApplicationCreator(data)
                    .clientRest(clients.get(loan.getEmail()))
                    .totalInterest(loanType.getTaxRate())
                    .totalMonthlyPayment(calculateMonthlyPayment(
                            loan.getAmount(), loanType.getTaxRate(), loan.getTerm()))
                    .build();
        }).collect(Collectors.toList());
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal numerator = monthlyRate.multiply((monthlyRate.add(BigDecimal.ONE)).pow(termMonths));
        BigDecimal denominator = ((monthlyRate.add(BigDecimal.ONE)).pow(termMonths)).subtract(BigDecimal.ONE);

        return principal.multiply(numerator.divide(denominator, 10, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
