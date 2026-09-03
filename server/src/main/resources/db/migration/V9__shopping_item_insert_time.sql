-- Give each shopping item its own created_at so a bulk add keeps its order.
--
-- now() is the transaction timestamp, identical for every statement in the transaction. Adding a
-- recipe's ingredients to the list inserts them all in one transaction, so every row got the same
-- created_at and the list's ORDER BY fell through to its id tiebreak — a random uuid, which
-- scrambled the batch. clock_timestamp() reads the clock per row, so insertion order survives.
--
-- Rows already inserted keep their shared timestamps; their relative order stays arbitrary, but
-- it is at least stable. Nothing else reads this column.
ALTER TABLE shopping_items
    ALTER COLUMN created_at SET DEFAULT clock_timestamp();
