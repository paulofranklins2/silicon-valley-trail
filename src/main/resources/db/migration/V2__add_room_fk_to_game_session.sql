alter table game_session
    add constraint fk_game_session_room
    foreign key (room_id) references room(id)
    on delete cascade;
