package de.vyacheslav.kushchenko.sales.funnel.web.security.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme

@OpenAPIDefinition
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT токен для аутентификации. Получить токен можно через /api/auth/sign-in"
)
class SpringBearerAuth
