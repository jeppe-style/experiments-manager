create table IF NOT EXISTS EXPERIMENTS
(USERNAME varchar(255) not null,
EXPERIMENT_NAME varchar(255) not null,
EXPERIMENT_NUMBER bigint not null,
PERFORMED_ON timestamp,
STATUS varchar(255) not null,
primary key(USERNAME, EXPERIMENT_NAME, EXPERIMENT_NUMBER))