package com.ietipiko.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class GameScreen extends ScreenAdapter {

    private Game game;
    private GameClient cliente;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private Stage stage;
    private Skin skin;
    private boolean isLeftPressed = false;
    private boolean isRightPressed = false;
    private boolean isJumpPressed = false;

    private Texture[] texturasEsqueletos;
    private TextureRegion[] animacionesIdle;

    private float spawnX = 100f;
    private float spawnY = 64f; // Justo encima del suelo

    public GameScreen(Game game, GameClient cliente) {
        this.game = game;
        this.cliente = cliente;
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();

        this.stage = new Stage(new FitViewport(800, 480));
        Gdx.input.setInputProcessor(this.stage);

        cargarAnimaciones();
        construirUI();
    }

    private void cargarAnimaciones() {
        String[] archivosColores = {
            "media/skeleton_color1.png",
            "media/skeleton_color5.png",
            "media/skeleton_color6.png"
        };

        texturasEsqueletos = new Texture[archivosColores.length];
        animacionesIdle = new TextureRegion[archivosColores.length];

        for (int i = 0; i < archivosColores.length; i++) {
            texturasEsqueletos[i] = new Texture(Gdx.files.internal(archivosColores[i]));
            TextureRegion[][] frames = TextureRegion.split(texturasEsqueletos[i], 112, 186);
            animacionesIdle[i] = frames[0][0];
        }
    }

    private void construirUI() {
        skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        skin.add("default", new BitmapFont());

        TextButton.TextButtonStyle estiloBoton = new TextButton.TextButtonStyle();
        estiloBoton.up = skin.newDrawable("white", Color.DARK_GRAY);
        estiloBoton.down = skin.newDrawable("white", Color.GRAY);
        estiloBoton.font = skin.getFont("default");
        skin.add("default", estiloBoton);

        Table tabla = new Table();
        tabla.setFillParent(true);
        tabla.bottom().padBottom(20);

        TextButton btnIzquierda = new TextButton("<", skin);
        TextButton btnDerecha = new TextButton(">", skin);
        TextButton btnSalto = new TextButton("SALTO", skin);

        btnIzquierda.addListener(crearListenerBoton("left"));
        btnDerecha.addListener(crearListenerBoton("right"));
        btnSalto.addListener(crearListenerBoton("jump"));

        tabla.add(btnIzquierda).width(100).height(80).padRight(20);
        tabla.add(btnDerecha).width(100).height(80).expandX().left();
        tabla.add(btnSalto).width(120).height(80).right().padRight(20);

        stage.addActor(tabla);
        pixmap.dispose();
    }

    private ClickListener crearListenerBoton(final String accion) {
        return new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                setAccion(accion, true);
                return true;
            }
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                setAccion(accion, false);
            }
        };
    }

    private void setAccion(String accion, boolean valor) {
        if (accion.equals("left")) isLeftPressed = valor;
        if (accion.equals("right")) isRightPressed = valor;
        if (accion.equals("jump")) isJumpPressed = valor;
    }

    private void enviarInput() {
        if (cliente != null && cliente.isOpen()) {
            String json = "{\"type\":\"INPUT\","
                + "\"left\":"  + isLeftPressed  + ","
                + "\"right\":" + isRightPressed + ","
                + "\"jump\":"  + isJumpPressed  + "}";
            cliente.send(json);
        }
    }

    @Override
    public void render(float delta) {
        enviarInput();

        Gdx.gl.glClearColor(0.03f, 0.04f, 0.06f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), 64);
        shapeRenderer.end();

        batch.begin();
        if (animacionesIdle.length > 0) {
            batch.draw(animacionesIdle[0], spawnX, spawnY);
        }
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void actualizarEstado(JsonValue playersJson) {
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        stage.dispose();
        if (skin != null) skin.dispose();

        if (texturasEsqueletos != null) {
            for (Texture tex : texturasEsqueletos) {
                if (tex != null) tex.dispose();
            }
        }
    }
}
