package co.com.crediya.model.loantype.gateways;

import co.com.crediya.model.loantype.LoanType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoanTypeRepository {
    Mono<LoanType> findByName(String name);
    Flux<LoanType> findAllById(List<Integer> id);
    Mono<LoanType> findById(Integer id);
}
