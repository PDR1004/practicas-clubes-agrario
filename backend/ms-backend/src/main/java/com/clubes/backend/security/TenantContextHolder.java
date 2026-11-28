package com.clubes.backend.security;

import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;

/**
 * Almacena el {@link TenantContext} asociado al hilo del request HTTP en curso.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext context) {
        CONTEXT.set(context);
    }

    public static Optional<TenantContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static TenantContext require() {
        TenantContext context = CONTEXT.get();
        if (context == null) {
            throw new AccessDeniedException("No existe contexto de tenant en el request actual");
        }
        return context;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
