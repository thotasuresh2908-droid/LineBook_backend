-- =====================================================================================
-- Remove the demo rows that are already in your database.
--
-- This is NOT a Flyway migration and it does NOT run on deploy. Run it by hand, once,
-- after you have looked at what it will delete. It is deliberately kept out of
-- src/main/resources/db/migration so that nothing can wipe live data automatically.
--
-- Why the existing migrations were not edited: V1, V2 and V3 have already run against
-- your production database and Flyway stores a checksum of each one. Changing a file
-- that has already run makes the next startup fail with a checksum mismatch. New rows
-- are removed with statements like these instead.
--
-- HOW TO RUN
--   1. Take a backup or a snapshot of the database first.
--   2. Run section 1 on its own and read the numbers it prints.
--   3. If they look right, run section 2.
--   4. Run section 3 to confirm nothing is left.
--
-- WHAT IS AND IS NOT DELETED
--   Deleted: the 36 demo customers (ids cu-01 … cu-36) with their bills, payments and
--            receipts, and the 5 demo agents (ag-01 … ag-05), but only those agents
--            that have nothing left pointing at them.
--   Kept:    the package catalogue (Basic FTA, Family, HD Family, Sports HD, Premium
--            4K). Every customer must have a package and there is no screen for adding
--            one, so deleting these would stop you adding customers at all. Rename them
--            or change the amounts with section 4 instead.
--   Kept:    anything you created yourself. Customers added through the app get an id
--            like cu-3f9a21b4c7d8 (12 characters after the dash), so the pattern below
--            cannot match them.
-- =====================================================================================


-- ------------------------------------------------------------------ 1. look first
-- Run this on its own. Nothing is changed.

SELECT 'demo customers' AS what, COUNT(*) AS rows_affected
FROM customers WHERE id REGEXP '^cu-[0-9]{2}$'
UNION ALL
SELECT 'their bills', COUNT(*)
FROM bills WHERE customer_id REGEXP '^cu-[0-9]{2}$'
UNION ALL
SELECT 'their receipts', COUNT(*)
FROM collections WHERE customer_id REGEXP '^cu-[0-9]{2}$'
UNION ALL
SELECT 'customers you added (these are kept)', COUNT(*)
FROM customers WHERE id NOT REGEXP '^cu-[0-9]{2}$';

-- The demo agents, and whether anything of yours still points at them.
SELECT a.id,
       a.name,
       (SELECT COUNT(*) FROM customers   c WHERE c.agent_id = a.id) AS customers_total,
       (SELECT COUNT(*) FROM customers   c WHERE c.agent_id = a.id
                                            AND c.id NOT REGEXP '^cu-[0-9]{2}$') AS your_customers,
       (SELECT COUNT(*) FROM collections r WHERE r.agent_id = a.id) AS receipts_total
FROM agents a
WHERE a.id IN ('ag-01','ag-02','ag-03','ag-04','ag-05')
ORDER BY a.id;


-- ------------------------------------------------------------------ 2. delete
-- Run this only when the numbers above look right.
-- The order matters: children before parents, or the foreign keys will block it.

START TRANSACTION;

-- payments recorded against the demo bills
DELETE bp FROM bill_payments bp
JOIN bills b ON b.id = bp.bill_id
WHERE b.customer_id REGEXP '^cu-[0-9]{2}$';

-- receipts issued to the demo customers
DELETE FROM collections
WHERE customer_id REGEXP '^cu-[0-9]{2}$';

-- the demo bills
DELETE FROM bills
WHERE customer_id REGEXP '^cu-[0-9]{2}$';

-- the demo customers
DELETE FROM customers
WHERE id REGEXP '^cu-[0-9]{2}$';

-- the demo agents, and their logins if any were created for them.
-- An agent is skipped when a customer or a receipt of yours still points at them; that
-- is the same rule the Delete button uses on the Agents page.
DELETE u FROM app_users u
WHERE u.agent_id IN ('ag-01','ag-02','ag-03','ag-04','ag-05')
  AND NOT EXISTS (SELECT 1 FROM customers   c WHERE c.agent_id = u.agent_id)
  AND NOT EXISTS (SELECT 1 FROM collections r WHERE r.agent_id = u.agent_id);

DELETE a FROM agents a
WHERE a.id IN ('ag-01','ag-02','ag-03','ag-04','ag-05')
  AND NOT EXISTS (SELECT 1 FROM customers   c WHERE c.agent_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM collections r WHERE r.agent_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM app_users   u WHERE u.agent_id = a.id);

COMMIT;


-- ------------------------------------------------------------------ 3. check
-- All three counts should be 0. Any agent still listed has data of yours attached, so
-- it was kept on purpose; deactivate it from the Agents page instead.

SELECT 'demo customers left' AS what, COUNT(*) AS n FROM customers WHERE id REGEXP '^cu-[0-9]{2}$'
UNION ALL
SELECT 'demo bills left',  COUNT(*) FROM bills       WHERE customer_id REGEXP '^cu-[0-9]{2}$'
UNION ALL
SELECT 'demo receipts left', COUNT(*) FROM collections WHERE customer_id REGEXP '^cu-[0-9]{2}$';

SELECT id, name, area FROM agents ORDER BY seq;


-- ------------------------------------------------------------------ 4. your own packages
-- Optional. The five plans are the only reference data left. Rename them and set your
-- real prices here; the app has no screen for this.
--
-- UPDATE package_plans SET name = 'Your plan name', amount = 250.00 WHERE id = 'pkg-basic';
-- UPDATE package_plans SET name = '...',            amount = 350.00 WHERE id = 'pkg-family';
--
-- Changing an amount does not alter bills that are already made; it applies to customers
-- added afterwards. To add a plan of your own:
--
-- INSERT INTO package_plans (id, name, amount, sort_order)
-- VALUES ('pkg-yourname', 'Your plan', 399.00, 6);


-- ------------------------------------------------------------------ 5. starting clean
-- If this database has nothing you want to keep at all, this empties every business
-- table and leaves the packages and your login accounts alone. Use with care.
--
-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE bill_payments;
-- TRUNCATE TABLE collections;
-- TRUNCATE TABLE receipt_counters;
-- TRUNCATE TABLE bills;
-- TRUNCATE TABLE customers;
-- DELETE FROM app_users WHERE role = 'AGENT';
-- DELETE FROM agents;
-- SET FOREIGN_KEY_CHECKS = 1;
