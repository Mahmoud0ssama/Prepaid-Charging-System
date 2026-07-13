
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    msisdn VARCHAR(15) UNIQUE NOT NULL,
    balance NUMERIC(10, 2) DEFAULT 0.00
);

CREATE TABLE IF NOT EXISTS cdrs (
    id SERIAL PRIMARY KEY,
    msisdn VARCHAR(15) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration INTEGER NOT NULL,
    call_result VARCHAR(50) NOT NULL,
    call_cost NUMERIC(10, 2) NOT NULL,
    balance_after_call NUMERIC(10, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_msisdn ON users(msisdn);
CREATE INDEX IF NOT EXISTS idx_cdrs_msisdn ON cdrs(msisdn);
