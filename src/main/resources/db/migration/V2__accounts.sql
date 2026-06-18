CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    vk_id VARCHAR(50) not null,
    channel_url varchar(100) not null,
    username VARCHAR(100) not null,
    recipient_id VARCHAR(100) NOT NULL,
    refresh_token_id UUID NOT NULL
);
