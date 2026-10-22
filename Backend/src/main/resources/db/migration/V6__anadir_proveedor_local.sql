-- ==========================================================================
-- V6 — anadir el proveedor LOCAL a media_assets
-- ==========================================================================
--
-- Contexto: se implementa el adaptador de almacenamiento local (LocalMediaStorage),
-- que escribe los bytes en disco bajo una ruta derivada del hash del contenido
-- y deja que Caddy los sirva directamente. El enum MediaProvider de Kotlin ya
-- incluye LOCAL; sin esta migracion, ddl-auto=validate / el CHECK de la base
-- rechazarian cualquier fila con ese proveedor.
--
-- IMPORTANTE: la migracion V1 sigue pendiente de eliminarse (esta prevista como
-- V5 en el plan de Flyway). El orden real en el repositorio es V1, V2, V3, V4.
-- ==========================================================================

-- El CHECK actual solo admite CLOUDINARY, MINIO y S3. Se redefine en lugar de
-- eliminarlo: el nombre de la restriccion debe conservarse para que el
-- ON DELETE y los mensajes de error sigan igual de utiles.
ALTER TABLE media_assets DROP CONSTRAINT chk_media_provider;

ALTER TABLE media_assets
    ADD CONSTRAINT chk_media_provider
    CHECK (
        provider IN (
            'LOCAL',
            'CLOUDINARY',
            'MINIO',
            'S3'
        )
    );