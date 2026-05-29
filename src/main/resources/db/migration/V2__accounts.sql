CREATE TABLE accounts (
    id VARCHAR(50) primary key,
    recipient_id UUID NOT NULL,
    refresh_token_id UUID NOT NULL,
);
