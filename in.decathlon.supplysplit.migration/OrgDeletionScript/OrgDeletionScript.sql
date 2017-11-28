-- Function: p_org_deletion(character varying)

-- DROP FUNCTION p_org_deletion(character varying);

CREATE OR REPLACE FUNCTION p_org_deletion(p_orgname character varying)
  RETURNS void AS
$BODY$ DECLARE
   t ad_table.tablename%TYPE;
   c ad_column.columnname%TYPE;
   v_total numeric;
   v_name varchar(100):='';
   sql varchar:='';
   sql2 varchar:='';
   AD_ORG_ID VARCHAR:='ad_org_id';
   column_count numeric;
BEGIN
 --disable all triggers
 update pg_trigger set tgenabled='D' where tgenabled='O' and tgisinternal='f';
 --remove all constraints of ad_org
 --perform p_drop_constraints();
 
 FOR t IN ( SELECT tablename FROM ad_table where isview <> 'Y' and tablename not ilike 'AD_TABLE' 
 and tablename not ilike 'AD_ORG' and tablename not ilike 'AD_OrgInfo' and tablename not ilike 'AD_Client' and tablename not ilike 'ad_role_orgaccess' and tablename not ilike 'ad_treenode' and tablename not ilike 'AD_OrgType'  and tablename not ilike 'AD_image' and tablename not ilike 'AD_User' and tablename not ilike 'ad_user_roles'  order by tablename) 
    LOOP
	v_name := t;
      	RAISE NOTICE 'Table name is %',v_name;
	sql2:= 'SELECT count(COLUMN_NAME) FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME ILIKE '''||AD_ORG_ID||''' 
	AND TABLE_NAME ILIKE '''||v_name||''''; 
	execute sql2 into column_count;
	if column_count <> 0 then
	execute 'delete from ' || v_name || ' where ad_org_id ='''|| p_orgname ||'''';		
	RAISE notice 'table % ',v_name;		
	end if;
    END LOOP;
--enable trigger
update pg_trigger set tgenabled='O' where tgenabled='D' and tgisinternal='f';
RETURN ;
END;$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION p_org_deletion(character varying)
  OWNER TO postgres;
