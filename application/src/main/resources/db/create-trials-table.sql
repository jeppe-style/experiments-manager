create table IF NOT EXISTS TRIALS
(USERNAME varchar(255) not null,
EXPERIMENT_NAME varchar(255) not null,
EXPERIMENT_NUMBER bigint not null,
TRIAL_NUMBER integer not null,
FABAN_RUN_ID varchar(255),
PERFORMED_ON timestamp,
STATUS varchar(255) not null,
primary key (USERNAME, EXPERIMENT_NAME, EXPERIMENT_NUMBER, TRIAL_NUMBER))