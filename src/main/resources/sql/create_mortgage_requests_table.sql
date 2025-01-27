-- Drop the mortgage_requests table
DROP TABLE mortgage_requests;

-- Create the mortgage_requests table with the corresponding columns and a foreign key constraint
CREATE TABLE mortgage_requests
(
    id                            UUID PRIMARY KEY,
    status                        VARCHAR(255),
    customer_email                VARCHAR(255),
    old_house_value               FLOAT,
    old_house_mortgage            FLOAT,
    savings                       FLOAT,
    yearly_income                 FLOAT,
    requested_amount              FLOAT,
    house_value                   FLOAT,
    self_employment               BOOLEAN,
    indefinite_contract           BOOLEAN,
    desired_interest_rate_period  INTEGER,
    base_rate                     FLOAT,
    copy_of_id                    TEXT,
    feedback                      TEXT,
    interest_rate                 FLOAT,
    interest_rate_expiration_date TEXT,
    creation_date                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_email  FOREIGN KEY (customer_email) REFERENCES customers (email)
);

-- Define a function to update the updated_date column with the current timestamp
CREATE OR REPLACE FUNCTION update_updated_date_column()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Set the updated_date column to the current timestamp
    NEW.updated_date = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create a trigger that calls the update_updated_date_column function before any update operation on the mortgage_requests table
CREATE TRIGGER update_updated_date
    BEFORE UPDATE
    ON mortgage_requests
    FOR EACH ROW
EXECUTE FUNCTION update_updated_date_column();
