select bg_bug_id , bg_user_74 , bg_user_11 from td.BUG where BG_USER_74 is null and BG_USER_11 not in ('Code merge', 'Review')


SELECT     NNER.BG_BUG_ID AS QC_ID, [td.audit_property].AP_FIELD_NAME, [td.audit_property].AP_OLD_VALUE AS TFS_OLD_Value, 
                      [td.audit_property].AP_NEW_VALUE AS TFS_NEW_VALUE, [td.audit_log].AU_TIME AS TFSmodifieddate
FROM         td.BUG AS NNER INNER JOIN
                      td.AUDIT_LOG AS [td.audit_log] ON NNER.BG_BUG_ID = [td.audit_log].AU_ENTITY_ID INNER JOIN
                      td.AUDIT_PROPERTIES AS [td.audit_property] ON [td.audit_log].AU_ACTION_ID = [td.audit_property].AP_ACTION_ID
WHERE     ([td.audit_log].AU_ENTITY_TYPE = 'BUG') AND ([td.audit_property].AP_FIELD_NAME = 'BG_user_98') AND ([td.audit_log].AU_TIME >=
                          (SELECT     DATEADD(dd, DATEDIFF(d, 0, GETDATE()),0) AS Expr1)) and [td.audit_property].AP_OLD_VALUE is not null
