CREATE TABLE categories (
    id UUID PRIMARY KEY,
    parent_id UUID NULL REFERENCES categories(id),

    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL
        REFERENCES categories(id),

    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,

    description TEXT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY,

    product_id UUID NOT NULL
        REFERENCES products(id),

    name VARCHAR(150) NOT NULL,

    sku VARCHAR(100) NOT NULL UNIQUE,

    volume_ml INTEGER,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE product_variant_prices (
    id UUID PRIMARY KEY,

    variant_id UUID NOT NULL
        REFERENCES product_variants(id),

    price NUMERIC(12,2) NOT NULL,

    currency CHAR(3) NOT NULL DEFAULT 'COP',

    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE media_assets (
    id UUID PRIMARY KEY,

    asset_type VARCHAR(30) NOT NULL,

    provider VARCHAR(30) NOT NULL,

    storage_key TEXT NOT NULL,

    public_id VARCHAR(255),

    url TEXT,

    mime_type VARCHAR(100),

    file_name VARCHAR(255),

    file_size_bytes BIGINT,

    width INTEGER,
    height INTEGER,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_media_asset_type
        CHECK (
            asset_type IN (
                'IMAGE',
                'SVG',
                'MODEL_3D'
            )
        ),

    CONSTRAINT chk_media_provider
        CHECK (
            provider IN (
                'CLOUDINARY',
                'MINIO',
                'S3'
            )
        )
);

CREATE TABLE product_variant_media (
    id UUID PRIMARY KEY,

    variant_id UUID NOT NULL
        REFERENCES product_variants(id),

    media_asset_id UUID NOT NULL
        REFERENCES media_assets(id),

    role VARCHAR(30) NOT NULL,

    sort_order INTEGER NOT NULL DEFAULT 0,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_variant_media_role
        CHECK (
            role IN (
                'PRIMARY',
                'GALLERY',
                'THUMBNAIL',
                'POSTER',
                'MODEL_3D'
            )
        )
);

CREATE TABLE variant_3d_components (
    id UUID PRIMARY KEY,

    variant_id UUID NOT NULL
        REFERENCES product_variants(id),

    name VARCHAR(100) NOT NULL,

    code VARCHAR(100) NOT NULL,

    media_asset_id UUID NOT NULL
        REFERENCES media_assets(id),

    required BOOLEAN NOT NULL DEFAULT FALSE,

    default_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (variant_id, code)
);