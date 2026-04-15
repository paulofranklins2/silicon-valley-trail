create table if not exists app_user (
    id varchar(255) primary key,
    login varchar(255) not null unique,
    password_hash varchar(255) not null,
    created_at timestamp(6) not null
);

create table if not exists auth_session (
    id varchar(255) primary key,
    user_id varchar(255) not null,
    created_at timestamp(6) not null,
    expires_at timestamp(6) not null,
    constraint fk_auth_session_user
        foreign key (user_id) references app_user(id)
        on delete cascade
);

alter table game_session
    add column if not exists user_id varchar(255);

alter table game_session
    add constraint fk_game_session_user
        foreign key (user_id) references app_user(id)
        on delete set null;

create index if not exists idx_game_session_user_completed_last_action
    on game_session (user_id, completed, last_action_at desc);

create index if not exists idx_auth_session_user
    on auth_session (user_id);
