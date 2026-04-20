package com.ietipiko.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class GameScreen extends ScreenAdapter {
    private Stage stage;
    private Skin skin;
    private Touchpad movePad;
    private Touchpad aimPad;
    private TextButton jumpButton;

    public GameScreen(Game game) {
        stage = new Stage(new FitViewport(640, 360));
        Gdx.input.setInputProcessor(stage);

// Fondo del Joystick (Círculo grande traslúcido)
        Pixmap pixmapBg = new Pixmap(200, 200, Pixmap.Format.RGBA8888);
        pixmapBg.setColor(1, 1, 1, 0.2f); // Blanco con mucha transparencia
        pixmapBg.fillCircle(100, 100, 100);
        skin.add("touchBackground", new Texture(pixmapBg));

// El palito del Joystick (Círculo pequeño más opaco)
        Pixmap pixmapKnob = new Pixmap(100, 100, Pixmap.Format.RGBA8888);
        pixmapKnob.setColor(1, 1, 1, 0.5f);
        pixmapKnob.fillCircle(50, 50, 50);
        skin.add("touchKnob", new Texture(pixmapKnob));

// Crear el estilo del Touchpad
        Touchpad.TouchpadStyle padStyle = new Touchpad.TouchpadStyle();
        padStyle.background = skin.getDrawable("touchBackground");
        padStyle.knob = skin.getDrawable("touchKnob");
        skin.add("default", padStyle);
        construirControles();
    }

    private void construirControles() {
        // Tabla principal que ocupa toda la pantalla
        Table table = new Table();
        table.setFillParent(true);
        table.bottom(); // Todo el contenido al fondo de la pantalla

        // 1. Joystick Izquierdo (Movimiento)
        movePad = new Touchpad(10, skin);

        // 2. Joystick Derecho (Apuntado/Otros)
        aimPad = new Touchpad(10, skin);

        // 3. Botón de Salto
        jumpButton = new TextButton("JUMP", skin);

        // Posicionamiento en pantalla
        // Fila inferior: [Pad Izquierdo] [Espacio] [Boton Salto] [Pad Derecho]
        table.add(movePad).size(120).pad(20).left();
        table.add().expandX(); // Este hueco empuja los otros a las esquinas
        table.add(jumpButton).size(80).padBottom(40);
        table.add(aimPad).size(120).pad(20).right();

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // LÓGICA DE LECTURA (Aquí es donde enviarías datos al servidor luego)
        float movX = movePad.getKnobPercentX();
        float movY = movePad.getKnobPercentY();

        if (jumpButton.isPressed()) {
            // Lógica de saltar
        }

        stage.act(delta);
        stage.draw();
    }
}
