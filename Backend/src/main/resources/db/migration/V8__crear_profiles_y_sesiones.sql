-- ==========================================================================
-- V8 - Perfiles de usuario y sesiones de aplicacion (D6, F4.16)
-- ==========================================================================
-- Dos tablas, y las dos son de la parte "privada" del esquema: hasta ahora
-- no existia ninguna tabla que guardara algo del usuario.
--
-- PROFILES
--   El perfil es la fila que el backend usa de verdad. `user_id` es el
--   identificador que da el proveedor de identidad (Supabase `auth.users`),
--   y `role` es lo que decide que puede hacer la persona.
--
--   `role` NO se escribe desde la API publica. No hay endpoint que lo
--   cambie: se asigna por SQL o por una operacion administrativa. Si el
--   rol se pudiera leer del cuerpo de la peticion, cualquier cliente podria
--   pedirse el rol ADMIN y la autorizacion no valdria nada.
--
--   La FK a `auth.users` es intencionada: un perfil sin identidad no
--   existe. Con `ON DELETE CASCADE`, borrar el usuario en Supabase se lleva
--   por delante su perfil y sus sesiones.
--
-- APP_SESSIONS
--   La sesion vive en el servidor, no en el navegador. La cookie lleva un
--   token aleatorio y aqui solo se guarda su SHA-256: un dump de la base de
--   datos no entrega sesiones usables.
--
--   `token_hash` es VARCHAR(64) y no CHAR(64) a proposito: Hibernate valida
--   el esquema con `ddl-auto=validate` y un CHAR no es un VARCHAR (es el
--   mismo caso que arreglo la migracion V3 con `currency`).
--
--   `revoked_at` es lo que hace que el logout funcione de verdad: borrar la
--   cookie en el navegador no invalida nada, porque el token seguiria siendo
--   valido hasta que expirase.
--
--   `expires_at` es el tope absoluto de la sesion. `last_seen_at` allowa el
--   deslizamiento: mientras la sesion se use, se va empujando, pero nunca
--   mas alla de `expires_at`.
-- ==========================================================================

CREATE TABLE profiles (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL UNIQUE
        REFERENCES auth.users(id) ON DELETE CASCADE,

    email VARCHAR(320) NOT NULL,

    full_name VARCHAR(200),

    role VARCHAR(20) NOT NULL DEFAULT 'CLIENTE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_profiles_role
        CHECK (
            role IN (
                'CLIENTE',
                'EMPLEADO',
                'ADMIN'
            )
        )
);

CREATE TABLE app_sessions (
    id UUID PRIMARY KEY,

    token_hash VARCHAR(64) NOT NULL UNIQUE,

    profile_id UUID NOT NULL
        REFERENCES profiles(id) ON DELETE CASCADE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,

    user_agent VARCHAR(400),
    ip_address VARCHAR(64)
);

-- Para revocar todas las sesiones de un usuario de una vez.
CREATE INDEX idx_app_sessions_profile_id
    ON app_sessions(profile_id);

-- Para limpiar las sesiones caducadas.
CREATE INDEX idx_app_sessions_expires_at
    ON app_sessions(expires_at);
