CREATE TABLE accounts (
    id VARCHAR(50) primary key,
    username VARCHAR(100) not null,
    recipient_id UUID NOT NULL,
    refresh_token_id UUID NOT NULL
);
