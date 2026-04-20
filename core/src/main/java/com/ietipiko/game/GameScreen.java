package com.ietipiko.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class GameScreen extends ScreenAdapter {
    private Game game;
    private GameClient cliente;
    private Stage stage;
    private Skin skin;

    // Estados de los botones
    private boolean isLeftPressed = false;
    private boolean isRightPressed = false;
    private boolean isJumpPressed = false;

    public GameScreen(Game game, GameClient cliente) {
        this.game = game;
        this.cliente = cliente;
        this.stage = new Stage(new FitViewport(800, 480));
        Gdx.input.setInputProcessor(stage);

        prepararSkin();
        construirInterfaz();
    }

    private void prepararSkin() {
        skin = new Skin();
        skin.add("default", new BitmapFont());

        // Textura simple para botones
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.2f, 0.2f, 0.2f, 0.8f));
        pixmap.fill();
        skin.add("btnFondo", new Texture(pixmap));

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up = skin.getDrawable("btnFondo");
        style.font = skin.getFont("default");
        style.fontColor = Color.WHITE;
        skin.add("default", style);
    }

    private void construirInterfaz() {
        Table table = new Table();
        table.setFillParent(true);
        table.bottom(); // Alineamos todo al fondo

        // Botón de SALTO (A la izquierda)
        TextButton btnJump = new TextButton("JUMP", skin);
        configurarBoton(btnJump, "jump");

        // Botones IZQUIERDA y DERECHA (A la derecha)
        TextButton btnLeft = new TextButton("<", skin);
        configurarBoton(btnLeft, "left");

        TextButton btnRight = new TextButton(">", skin);
        configurarBoton(btnRight, "right");

        // --- LAYOUT ---
        // Añadimos SALTO a la izquierda
        table.add(btnJump).size(100, 80).left().pad(20);

        // Espacio expandible en medio para empujar los otros botones a la derecha
        table.add().expandX();

        // Añadimos IZQUIERDA y DERECHA juntos a la derecha
        table.add(btnLeft).size(80, 80).pad(10);
        table.add(btnRight).size(80, 80).pad(10).padRight(20);

        stage.addActor(table);
    }

    private void configurarBoton(TextButton boton, final String accion) {
        boton.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                setAccion(accion, true);
                enviarInput();
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                setAccion(accion, false);
                enviarInput();
            }
        });
    }

    private void setAccion(String accion, boolean valor) {
        if (accion.equals("left")) isLeftPressed = valor;
        if (accion.equals("right")) isRightPressed = valor;
        if (accion.equals("jump")) isJumpPressed = valor;
    }

    private void enviarInput() {
        if (cliente != null && cliente.isOpen()) {
            // Enviamos el JSON que espera tu server.js
            String json = "{\"type\":\"INPUT\", \"left\":" + isLeftPressed +
                ", \"right\":" + isRightPressed +
                ", \"jump\":" + isJumpPressed + "}";
            cliente.send(json);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
