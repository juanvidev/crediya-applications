package co.com.crediya.api.router;

import co.com.crediya.api.docs.LoanApplicationDocs;
import co.com.crediya.api.handler.LoanApplicationHandler;
import co.com.crediya.api.util.Routes;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@Tag(name = "LoanApplications", description = "Loan Applications Management")
public class LoanApplicationRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/apply",
                    method = RequestMethod.POST,
                    beanClass = LoanApplicationDocs.class,
                    beanMethod = "createLoanApplication"
            )
    })
    public RouterFunction<ServerResponse> routerFunction(LoanApplicationHandler loanApplicationHandler) {
        return RouterFunctions
                .route()
                .path(Routes.BASE_PATH,builder -> builder
                    .POST("/apply", loanApplicationHandler::saveApplication)
                    .POST("/apply/{id}", loanApplicationHandler::listenUpdateStatusLoanApplication)
                )
                .build();
    }}
