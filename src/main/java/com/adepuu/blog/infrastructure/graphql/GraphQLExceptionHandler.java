package com.adepuu.blog.infrastructure.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Slf4j
@ControllerAdvice
public class GraphQLExceptionHandler {
    
    @GraphQlExceptionHandler
    public GraphQLError handleAccessDeniedException(AccessDeniedException ex, DataFetchingEnvironment env) {
        log.warn("Access denied: {}", ex.getMessage());
        return GraphqlErrorBuilder.newError()
                .message("Access denied: " + ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
    
    @GraphQlExceptionHandler
    public GraphQLError handleBadCredentialsException(BadCredentialsException ex, DataFetchingEnvironment env) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return GraphqlErrorBuilder.newError()
                .message("Authentication failed: " + ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
    
    @GraphQlExceptionHandler
    public GraphQLError handleIllegalArgumentException(IllegalArgumentException ex, DataFetchingEnvironment env) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return GraphqlErrorBuilder.newError()
                .message("Invalid input: " + ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
    
    @GraphQlExceptionHandler
    public GraphQLError handleRuntimeException(RuntimeException ex, DataFetchingEnvironment env) {
        log.error("Runtime exception in GraphQL resolver", ex);
        return GraphqlErrorBuilder.newError()
                .message("An error occurred: " + ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
}
