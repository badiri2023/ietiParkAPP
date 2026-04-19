package com.ietipiko.game;

import com.badlogic.gdx.Game;

public class Main extends Game {

    @Override
    public void create() {
        // Arrancamos el juego cargando la pantalla de Login
        this.setScreen(new LoginScreen(this));
    }

    @Override
    public void render() {
        // MUY IMPORTANTE: Esto le dice a LibGDX "Dibuja la pantalla que esté activa ahora mismo"
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
