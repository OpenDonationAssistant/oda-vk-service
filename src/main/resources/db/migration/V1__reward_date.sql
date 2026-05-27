CREATE TABLE reward_date (
    id VARCHAR(50) primary key,
    recipient_id UUID NOT NULL,
    refresh_token_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
);
