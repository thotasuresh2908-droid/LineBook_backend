-- Each collection now remembers the billing cycle (yyyy-MM) of the bill it paid, so reports can list a cycle's
-- collections without joining the bills table. Existing rows are filled in from their bills.
ALTER TABLE collections ADD COLUMN bill_cycle VARCHAR(7) NULL;

UPDATE collections c
JOIN bills b ON b.id = c.bill_id
SET c.bill_cycle = b.cycle;

ALTER TABLE collections MODIFY COLUMN bill_cycle VARCHAR(7) NOT NULL;
ALTER TABLE collections ADD KEY idx_collections_cycle (bill_cycle);
