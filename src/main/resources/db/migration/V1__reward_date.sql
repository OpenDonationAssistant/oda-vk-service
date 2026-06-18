CREATE TABLE reward (
    id VARCHAR(50) primary key,
    account_id UUID NOT NULL,
    widget_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL
);
