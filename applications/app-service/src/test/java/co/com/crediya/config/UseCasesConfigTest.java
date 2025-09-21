package co.com.crediya.config;

import co.com.crediya.model.clientrest.gateways.ClientRepository;
import co.com.crediya.model.clientrest.gateways.TokenGateway;
import co.com.crediya.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.crediya.model.loantype.gateways.LoanTypeRepository;
import co.com.crediya.model.status.gateways.StatusRepository;
import co.com.crediya.usecase.loanapplication.LoanApplicationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

public class UseCasesConfigTest {

    @Test
    @DisplayName("Should load LoanApplicationUseCase bean in the application context")
    void testLoanApplicationUseCaseBeanLoad() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            assertNotNull(context.getBean(LoanApplicationUseCase.class), "LoanApplicationUseCase bean should be loaded in the context");
        }
    }


    @Configuration
    @Import(UseCasesConfig.class)
    static class TestConfig {
        @Bean
        LoanApplicationRepository loanApplicationRepository() {
            return mock(LoanApplicationRepository.class);
        }

        @Bean
        public LoanTypeRepository loanTypeRepository() {
            return mock(LoanTypeRepository.class);
        }

        @Bean
        public StatusRepository statusRepository() {
            return mock(StatusRepository.class);
        }

        @Bean
        public ClientRepository clientRepository(){ return mock(ClientRepository.class); }

        @Bean
        public TokenGateway tokenGateway(){ return mock(TokenGateway.class); }
    }
}