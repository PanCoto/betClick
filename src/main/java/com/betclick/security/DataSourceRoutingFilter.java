package com.betclick.security;

import com.betclick.config.DataSourceContextHolder;
import jakarta.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class DataSourceRoutingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceRoutingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                boolean isEmployee = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MODERATOR"));
                
                if (isEmployee) {
                    DataSourceContextHolder.set(DataSourceContextHolder.DataSourceType.EMPLOYEE);
                    logger.debug("Routing datasource to EMPLOYEE for user: {}", auth.getName());
                } else {
                    DataSourceContextHolder.set(DataSourceContextHolder.DataSourceType.RUNTIME);
                    logger.debug("Routing datasource to RUNTIME for user: {}", auth.getName());
                }
            } else {
                DataSourceContextHolder.set(DataSourceContextHolder.DataSourceType.RUNTIME);
            }
            chain.doFilter(request, response);
        } finally {

            DataSourceContextHolder.clear();
        }
    }
}
