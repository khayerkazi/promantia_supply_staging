--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

-- remove saved grid preferences to apply grid configuration
delete
  from ad_preference p
  where property like 'OBUIAPP%'
    and exists (select 1 from ad_tab t 
                 where p.ad_window_id = t.ad_window_id
                 and ad_tab_id in ('384',
                                   '0E963C26FB264872A417046C0F7F6CFB',
                                   '296',
                                   'B5A9130A11754CE18F18A00A0AE0A164',
                                   '0723852E910440E8B99DDD2822EEC88C',
                                   '7D68FFCA597C4F84BC385DBCA7A8308C',
                                    'AB28683387C44BD5B4AA1A4714C098C3'));

-- remove saved views to apply grid configuration
delete
  from obuiapp_uipersonalization p
 where p.type = 'Window'
   and exists (select 1 from ad_tab t 
               where p.ad_window_id = t.ad_window_id
               and ad_tab_id in ('384',
                                 '0E963C26FB264872A417046C0F7F6CFB',
                                 '296',
                                 'B5A9130A11754CE18F18A00A0AE0A164',
                                 '0723852E910440E8B99DDD2822EEC88C',
                                 '7D68FFCA597C4F84BC385DBCA7A8308C',
                                 'AB28683387C44BD5B4AA1A4714C098C3'));


--
-- Data for Name: obuiapp_gc_tab; Type: TABLE DATA; Schema: public; Owner: tad
--

INSERT INTO obuiapp_gc_tab (obuiapp_gc_tab_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, ad_tab_id, filterable, sortable, text_filter_behavior, threshold_to_filter, islazyfiltering, filteronchange, allowfilterbyidentifier) VALUES ('A0008DC2A8FF48409E32F573812F19E6', '0', '0', 'Y', '2014-11-18 12:12:14.484', '100', '2014-11-18 12:15:09.01', '100', '384', 'N', 'N', 'E', NULL, 'D', 'D', 'D');
INSERT INTO obuiapp_gc_tab (obuiapp_gc_tab_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, ad_tab_id, filterable, sortable, text_filter_behavior, threshold_to_filter, islazyfiltering, filteronchange, allowfilterbyidentifier) VALUES ('5801AC23398E47568628267FBA4C45BF', '0', '0', 'Y', '2014-11-18 13:30:16.781', '100', '2014-11-18 13:30:16.781', '100', '296', 'N', 'N', 'E', NULL, 'D', 'D', 'D');
INSERT INTO obuiapp_gc_tab (obuiapp_gc_tab_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, ad_tab_id, filterable, sortable, text_filter_behavior, threshold_to_filter, islazyfiltering, filteronchange, allowfilterbyidentifier) VALUES ('DEB9D9CD0D6841C5AF7F9A0E445F027F', '0', '0', 'Y', '2014-11-18 13:41:30.974', '100', '2014-11-18 13:41:30.974', '100', 'B5A9130A11754CE18F18A00A0AE0A164', 'N', 'N', 'E', NULL, 'D', 'D', 'D');
INSERT INTO obuiapp_gc_tab (obuiapp_gc_tab_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, ad_tab_id, filterable, sortable, text_filter_behavior, threshold_to_filter, islazyfiltering, filteronchange, allowfilterbyidentifier) VALUES ('61AB3DDF02A1497AB9AF81FCF69EB568', '0', '0', 'Y', '2014-11-18 13:48:40.18', '100', '2014-11-18 13:49:00.891', '100', '0723852E910440E8B99DDD2822EEC88C', 'N', 'N', 'E', NULL, 'D', 'D', 'D');
INSERT INTO obuiapp_gc_tab (obuiapp_gc_tab_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, ad_tab_id, filterable, sortable, text_filter_behavior, threshold_to_filter, islazyfiltering, filteronchange, allowfilterbyidentifier) VALUES ('62C199D4DE2C40AA871FBB4A095EB222', '0', '0', 'Y', '2014-11-18 11:12:05.471', '100', '2014-11-18 16:13:42.297', '100', '0E963C26FB264872A417046C0F7F6CFB', 'N', 'N', 'E', NULL, 'D', 'D', 'N');
INSERT INTO obuiapp_gc_tab (obuiapp_gc_tab_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, ad_tab_id, filterable, sortable, text_filter_behavior, threshold_to_filter, islazyfiltering, filteronchange, allowfilterbyidentifier) VALUES ('91CDB0BCB79E437B8AAB873613BC2B94', '0', '0', 'Y', '2014-11-25 11:45:00.639', '100', '2014-11-25 11:47:20.194', '100', 'AB28683387C44BD5B4AA1A4714C098C3', 'N', 'N', 'E', NULL, 'D', 'D', 'D');
INSERT INTO obuiapp_gc_tab (obuiapp_gc_tab_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, ad_tab_id, filterable, sortable, text_filter_behavior, threshold_to_filter, islazyfiltering, filteronchange, allowfilterbyidentifier) VALUES ('93F11B83610146ACB288DD2FD1379A0F', '0', '0', 'Y', '2014-11-25 11:53:51.665', '100', '2014-11-25 11:53:51.665', '100', '7D68FFCA597C4F84BC385DBCA7A8308C', 'N', 'N', 'E', NULL, 'D', 'D', 'D');


--
-- Data for Name: obuiapp_gc_field; Type: TABLE DATA; Schema: public; Owner: tad
--

INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('D5047039B32E42A2B7A1F9E2BA804A09', '0', '0', 'Y', '2014-11-18 12:16:12.694', '100', '2014-11-18 12:16:16.772', '100', 'A0008DC2A8FF48409E32F573812F19E6', '4908', 'Y', 'D', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('B41F347762C945BF8F575DE2008790ED', '0', '0', 'Y', '2014-11-18 12:54:40.455', '100', '2014-11-18 12:54:40.455', '100', 'A0008DC2A8FF48409E32F573812F19E6', '4917', 'Y', 'Y', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('21070EEBE2CA40719591AC082E348EAC', '0', '0', 'Y', '2014-11-18 12:54:55.672', '100', '2014-11-18 13:00:29.837', '100', 'A0008DC2A8FF48409E32F573812F19E6', '6F8D838351C64BDBA7637317A2987CE6', 'Y', 'D', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('4DF9B05C5C774E5B8978818ACBB7BD3B', '0', '0', 'Y', '2014-11-18 12:15:37.667', '100', '2014-11-18 13:07:24.583', '100', 'A0008DC2A8FF48409E32F573812F19E6', 'DC6ECF606A29403D974386809187465F', 'Y', 'D', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('1C3A70C4290F4E05BFAC8DB8438A342A', '0', '0', 'Y', '2014-11-18 13:09:50.341', '100', '2014-11-18 13:09:50.341', '100', '62C199D4DE2C40AA871FBB4A095EB222', 'B1E68AEDE00F4C4EA4C8FAC0A0E204C7', 'Y', 'Y', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('8EE2880AB3D64E5BB16E1297973193CA', '0', '0', 'Y', '2014-11-18 13:06:36.714', '100', '2014-11-18 13:22:05.519', '100', '62C199D4DE2C40AA871FBB4A095EB222', '59111B2A0F4443DBB6676DEB4DF588B4', 'Y', 'N', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('56E09DA86664466A9892C8AE114DCF21', '0', '0', 'Y', '2014-11-18 13:25:55.437', '100', '2014-11-18 13:25:55.437', '100', '62C199D4DE2C40AA871FBB4A095EB222', '961FDCB77DB7424B8A2C6390B4F7CD10', 'Y', 'D', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('82AB7C1A4D1E4A63BF0A8B6A0742B0DC', '0', '0', 'Y', '2014-11-18 13:36:54.066', '100', '2014-11-18 13:36:54.066', '100', '5801AC23398E47568628267FBA4C45BF', '3496', 'Y', 'D', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('9DA31E2D789B43C3A61D47F39EC864E5', '0', '0', 'Y', '2014-11-18 13:37:04.408', '100', '2014-11-18 13:37:04.408', '100', '5801AC23398E47568628267FBA4C45BF', '3481', 'Y', 'Y', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('84C0AAF23BF5434E8514D1BEB4357387', '0', '0', 'Y', '2014-11-18 13:41:57.255', '100', '2014-11-18 13:41:57.255', '100', 'DEB9D9CD0D6841C5AF7F9A0E445F027F', '4A059F06CB6E468684A75B44DF0C874A', 'Y', 'D', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('D30F389CF654442EB5337D6579D84858', '0', '0', 'Y', '2014-11-18 13:42:41.439', '100', '2014-11-18 13:42:41.439', '100', 'DEB9D9CD0D6841C5AF7F9A0E445F027F', '0DB0BFE0FB8A4EEDAD3D2BF80B98E6C5', 'Y', 'D', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('9D8AC72B4BE64DA2B721905577248E1F', '0', '0', 'Y', '2014-11-18 13:42:55.889', '100', '2014-11-18 13:42:55.889', '100', 'DEB9D9CD0D6841C5AF7F9A0E445F027F', '3C01F65AD4564CC694E1677B49FF50AD', 'Y', 'Y', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('1DB953DC430746EFA49477902AA19DB6', '0', '0', 'Y', '2014-11-18 13:43:17.751', '100', '2014-11-18 13:43:17.751', '100', 'DEB9D9CD0D6841C5AF7F9A0E445F027F', '825DBBC7BA2A474391978C85A14C42D8', 'Y', 'D', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('710CD1C94F9541D58C19D319445A1913', '0', '0', 'Y', '2014-11-18 13:44:19.877', '100', '2014-11-18 13:44:19.877', '100', 'DEB9D9CD0D6841C5AF7F9A0E445F027F', '5DE85B6703C6428BB6F392096C32A360', 'Y', 'D', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('BDBA8A94B7C44729962C01DED72BDA0D', '0', '0', 'Y', '2014-11-18 13:44:37.781', '100', '2014-11-18 13:44:37.781', '100', 'DEB9D9CD0D6841C5AF7F9A0E445F027F', 'F612465ED16B4E5786895932739EEB40', 'Y', 'D', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('E1233F0DCEED4D1BA8C9832E7A520061', '0', '0', 'Y', '2014-11-18 13:48:52.278', '100', '2014-11-18 13:49:08.307', '100', '61AB3DDF02A1497AB9AF81FCF69EB568', 'EC228AF7D6064194A298DED4944E754C', 'Y', 'D', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('9DD9ABEE60A3471E8680E983B485BC48', '0', '0', 'Y', '2014-11-18 13:49:36.342', '100', '2014-11-18 13:49:36.342', '100', '61AB3DDF02A1497AB9AF81FCF69EB568', '881665F8D3934A7BBDC6BD545B9AF2FC', 'Y', 'Y', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('DF16DF5AF34F46C0BA6645C2460214EB', '0', '0', 'Y', '2014-11-18 16:10:40.924', '100', '2014-11-18 16:10:40.924', '100', '62C199D4DE2C40AA871FBB4A095EB222', '7B111576FA774472B7C17008A0386313', 'Y', 'D', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('1FAA25A40B1048E38B9D4BDFBE9136BF', '0', '0', 'Y', '2014-11-25 11:47:58.377', '100', '2014-11-25 11:47:58.377', '100', '91CDB0BCB79E437B8AAB873613BC2B94', 'EC65703BEF7D4E20A80291DFD36976A6', 'Y', 'D', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('663E2AD76A48437CAB39C24F4FB87932', '0', '0', 'Y', '2014-11-25 11:46:38.212', '100', '2014-11-25 11:48:09.198', '100', '91CDB0BCB79E437B8AAB873613BC2B94', 'C1BD396A6DBF4D45B62078AE1DC4CCAB', 'Y', 'D', 'D', 'D', NULL, 'N');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('570CC2B4824B456E9C6C7EB35F4F7694', '0', '0', 'Y', '2014-11-25 11:48:31.381', '100', '2014-11-25 11:48:31.381', '100', '91CDB0BCB79E437B8AAB873613BC2B94', '7AA63B1E7D27421E85EE66BACBCA7A1B', 'Y', 'D', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('C5ACF956F3F64105A0E7F6711DE22940', '0', '0', 'Y', '2014-11-25 11:51:04.786', '100', '2014-11-25 11:51:04.786', '100', '91CDB0BCB79E437B8AAB873613BC2B94', '6FF95D26042F4757B98A48E7E6D88B9E', 'Y', 'Y', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('E5B7B0C5AB5A499AA9BA6EC56EC228EA', '0', '0', 'Y', '2014-11-25 11:51:24.011', '100', '2014-11-25 11:51:24.011', '100', '91CDB0BCB79E437B8AAB873613BC2B94', 'F809A4887A0C434EB238F0064F38776D', 'Y', 'D', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('F9574969E6BA434BB62BE53407696C9E', '0', '0', 'Y', '2014-11-25 11:54:47.911', '100', '2014-11-25 11:54:47.911', '100', '93F11B83610146ACB288DD2FD1379A0F', '6A6AD3965A7B4C0CBB62AB9DB0056D74', 'Y', 'D', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('92BF08E783A04FD5881094F51E89F553', '0', '0', 'Y', '2014-11-25 11:54:59.662', '100', '2014-11-25 11:54:59.662', '100', '93F11B83610146ACB288DD2FD1379A0F', '53088CCABB6743A597C70176C1B0627E', 'Y', 'D', 'E', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('B67760200D49464E858B886B11E41804', '0', '0', 'Y', '2014-11-25 11:55:16.17', '100', '2014-11-25 11:55:16.17', '100', '93F11B83610146ACB288DD2FD1379A0F', '0ACA413CD4AF418BB8EC8C930BA89816', 'Y', 'Y', 'D', 'D', NULL, 'D');
INSERT INTO obuiapp_gc_field (obuiapp_gc_field_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, obuiapp_gc_tab_id, ad_field_id, filterable, sortable, text_filter_behavior, filteronchange, threshold_to_filter, allowfilterbyidentifier) VALUES ('2133FABF799044F79381985118C17F97', '0', '0', 'Y', '2014-11-25 11:55:57.116', '100', '2014-11-25 11:55:57.116', '100', '93F11B83610146ACB288DD2FD1379A0F', '741E9880124D46E299EC37CB01E3622D', 'Y', 'D', 'IC', 'D', NULL, 'D');


--
-- Data for Name: obuiapp_gc_system; Type: TABLE DATA; Schema: public; Owner: tad
--

INSERT INTO obuiapp_gc_system (obuiapp_gc_system_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, isfilterable, issortable, text_filter_behavior, threshold_to_filter, islazyfiltering, filteronchange, allowfilterbyidentifier) VALUES ('57EFA55C0300495F9F9F495F3D96D62D', '0', '0', 'Y', '2014-11-25 11:30:59.175', '100', '2014-11-25 11:30:59.175', '100', 'Y', 'Y', 'IC', 500, 'Y', 'Y', 'Y');


--
-- PostgreSQL database dump complete
--

