package co.com.crediya.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "UpdateStatusLoanApplicationRequestDTO", description = "Data required to update the status of a loan application")
public record UpdateStatusLoanApplicationRequestDTO(
    @NotBlank
    @Schema(description = "New status of the loan application", example = "APPROVED")
    String status
) {}
