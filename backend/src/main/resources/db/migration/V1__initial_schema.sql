CREATE TABLE staff (
 id UUID PRIMARY KEY, name VARCHAR(120) NOT NULL, username VARCHAR(60) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL CHECK (role IN ('ADMINISTRADOR','ATENDENTE','TRIAGEM')),
 phone VARCHAR(25) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE auth_session (
 token_hash VARCHAR(64) PRIMARY KEY, staff_id UUID NOT NULL REFERENCES staff(id),
 expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX session_staff_idx ON auth_session(staff_id);
CREATE TABLE login_bucket (
 bucket_key VARCHAR(64) PRIMARY KEY, attempts INTEGER NOT NULL, expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE category (id UUID PRIMARY KEY, name VARCHAR(120) NOT NULL UNIQUE, description VARCHAR(500) NOT NULL);
CREATE TABLE customer (
 id UUID PRIMARY KEY, name VARCHAR(120) NOT NULL, phone VARCHAR(25) NOT NULL,
 email VARCHAR(254) NOT NULL, notes VARCHAR(1000) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE donation (
 id UUID PRIMARY KEY, origin VARCHAR(160) NOT NULL, notes VARCHAR(1000) NOT NULL,
 received_at TIMESTAMP WITH TIME ZONE NOT NULL, received_by UUID NOT NULL REFERENCES staff(id)
);
CREATE TABLE product (
 id UUID PRIMARY KEY, donation_id UUID NOT NULL REFERENCES donation(id),
 name VARCHAR(120) NOT NULL, description VARCHAR(1000) NOT NULL, size VARCHAR(30) NOT NULL,
 brand VARCHAR(80) NOT NULL, condition VARCHAR(30) NOT NULL,
 category_id UUID REFERENCES category(id), price NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (price >= 0),
 status VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE','DISPONIVEL','DESCARTADO','VENDIDO')),
 location VARCHAR(120) NOT NULL, notes VARCHAR(1000) NOT NULL,
 CHECK (status NOT IN ('DISPONIVEL','VENDIDO') OR (category_id IS NOT NULL AND price > 0))
);
CREATE INDEX product_status_idx ON product(status, id);
CREATE INDEX product_donation_idx ON product(donation_id);
CREATE TABLE sale (
 id UUID PRIMARY KEY, customer_id UUID NOT NULL REFERENCES customer(id), staff_id UUID NOT NULL REFERENCES staff(id),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL, total NUMERIC(12,2) NOT NULL CHECK (total > 0),
 request_key UUID NOT NULL UNIQUE, request_hash VARCHAR(64) NOT NULL
);
CREATE INDEX sale_date_idx ON sale(created_at);
CREATE INDEX sale_customer_idx ON sale(customer_id, created_at);
CREATE TABLE sale_item (
 sale_id UUID NOT NULL REFERENCES sale(id), product_id UUID NOT NULL UNIQUE REFERENCES product(id),
 original_price NUMERIC(12,2) NOT NULL CHECK (original_price > 0),
 unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0 AND unit_price <= original_price),
 PRIMARY KEY(sale_id, product_id)
);
CREATE TABLE payment (
 sale_id UUID PRIMARY KEY REFERENCES sale(id), method VARCHAR(12) NOT NULL CHECK(method IN ('PIX','DINHEIRO','CARTAO')),
 amount NUMERIC(12,2) NOT NULL CHECK(amount > 0), paid_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE audit_event (
 id UUID PRIMARY KEY, actor_id UUID NOT NULL REFERENCES staff(id), action VARCHAR(40) NOT NULL,
 entity_id UUID NOT NULL, occurred_at TIMESTAMP WITH TIME ZONE NOT NULL, detail VARCHAR(1000) NOT NULL
);
CREATE INDEX audit_date_idx ON audit_event(occurred_at);

CREATE TABLE management_lock (id INTEGER PRIMARY KEY CHECK(id=1));
INSERT INTO management_lock VALUES (1);
