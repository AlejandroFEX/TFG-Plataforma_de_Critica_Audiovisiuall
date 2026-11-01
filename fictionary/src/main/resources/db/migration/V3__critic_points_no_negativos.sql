UPDATE app_user SET critic_points = 0 WHERE critic_points < 0;
ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_critic_points_non_negative CHECK (critic_points >= 0);