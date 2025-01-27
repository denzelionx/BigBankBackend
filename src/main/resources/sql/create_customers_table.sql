-- Drop customers table
DROP TABLE customers;

-- Create customers table
CREATE TABLE customers
(
    email         VARCHAR(255) PRIMARY KEY            NOT NULL,
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    phone         VARCHAR(255),
    address       VARCHAR(255),
    age           INTEGER,
    vip           BOOLEAN,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create the function to update the updated_date column with the current timestamp
CREATE OR REPLACE FUNCTION update_updated_date_column()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Set the updated_date column to the current timestamp
    NEW.updated_date = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create a trigger that calls the update_updated_date_column function before any update operation on the customers table
CREATE TRIGGER update_updated_date
    BEFORE UPDATE
    ON customers
    FOR EACH ROW
EXECUTE FUNCTION update_updated_date_column();