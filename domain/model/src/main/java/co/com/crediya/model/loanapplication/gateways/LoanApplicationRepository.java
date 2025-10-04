package co.com.crediya.model.loanapplication.gateways;

import co.com.crediya.model.loanapplication.LoanApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoanApplicationRepository {
    Mono<LoanApplication> save(LoanApplication loanApplication);
    Flux<LoanApplication> findAllApplicationsByStatus(int statusId, int page, int size);
    Mono<LoanApplication> findById(Integer id);
}
