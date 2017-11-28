-- Function: p_drop_constraints()

-- DROP FUNCTION p_drop_constraints();


CREATE OR REPLACE FUNCTION  p_drop_constraints()
  RETURNS void AS
$BODY$ DECLARE
DECLARE r RECORD;
 
BEGIN

for r in
 (SELECT
    tc.constraint_name, tc.table_name,tc.constraint_type
FROM 
    information_schema.table_constraints AS tc 
    JOIN information_schema.key_column_usage AS kcu
      ON tc.constraint_name = kcu.constraint_name
    JOIN information_schema.constraint_column_usage AS ccu
      ON ccu.constraint_name = tc.constraint_name
      WHERE tc.constraint_type='FOREIGN KEY' )

loop

RAISE NOTICE 'cons is % of table %',r.constraint_name,r.table_name;
	BEGIN
		execute 'alter table public.'||r.table_name ||' drop constraint ' || r.constraint_name;
	EXCEPTION WHEN others THEN
		RAISE NOTICE 'constraint % of relation % does not exist',r.constraint_name,r.table_name;
       END;
end loop;

RETURN ;
END;$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION p_drop_constraints()
  OWNER TO postgres;
