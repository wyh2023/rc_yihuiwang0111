create table if not exists notification_tasks (
    id varchar(40) not null primary key,
    target_url varchar(2048) not null,
    method varchar(16) not null,
    headers_json text not null,
    body_json text not null,
    status varchar(32) not null,
    attempt_count int not null,
    max_attempts int not null,
    next_retry_at datetime(6) not null,
    last_error varchar(2000),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    version bigint not null,
    index idx_notification_status_retry (status, next_retry_at)
);
