-- For reasonably small nflow_workflow row count:
alter table nflow_workflow add priority smallint not null default 0;
-- For large nflow_workflow row count:
--
--   alter table nflow_workflow add priority smallint null;
--
-- followed by either:
--
--   update nflow_workflow set priority = 0, modified = modified where priority is null;
--
-- or in batches of 100k with this query, repeated until no rows are affected:
--
--   update nflow_workflow set priority = 0, modified = modified where priority is null limit 100000;
--
-- and finally:
--
--   alter table nflow_workflow modify column priority smallint not null default 0;

alter table nflow_archive_workflow add priority smallint null;

create index nflow_workflow_polling on nflow_workflow(next_activation, status, executor_id, executor_group);

drop index nflow_workflow_activation on nflow_workflow;
