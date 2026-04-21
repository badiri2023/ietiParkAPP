package com.ietipiko.game;

import com.badlogic.gdx.Game;

public class Main extends Game {

    @Override
    public void create() {
        this.setScreen(new LoginScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
