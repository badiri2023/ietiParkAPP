package com.ietipiko.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameScreen extends ScreenAdapter {

    private Game game;
    private GameClient cliente;
    private SpriteBatch batch;

    private OrthographicCamera camara;
    private Viewport gameViewport;

    private Texture texturaPuerta;
    private MapRender mapRender;

    private Stage stage;
    private Skin skin;

    // Inputs
    private boolean isLeftPressed = false;
    private boolean isRightPressed = false;
    private boolean isJumpPressed = false;

    // Mundo
    private float puertaX, puertaY, puertaWidth, puertaHeight;
    private List<Texture> texturasCargadas = new ArrayList<>();
    private Map<String, Map<String, Animation<TextureRegion>>> animacionesPorColor = new HashMap<>();
    private List<DatosJugador> jugadoresOnline = new ArrayList<>();

    // --- MODELO DE DATOS CON INTERPOLACIÓN ---
    private class DatosJugador {
        String id;
        String nickname;
        String color;
        float x, y;               // Posición visual (suave)
        float targetX, targetY;   // Posición real del servidor
        float stateTime = 0f;
        boolean moviendose = false;
        boolean enAire = false;
        boolean mirandoIzquierda = false;
    }

    public GameScreen(Game game, GameClient cliente) {
        this.game = game;
        this.cliente = cliente;

        if (this.cliente != null) this.cliente.setPantallaJuego(this);

        batch = new SpriteBatch();
        camara = new OrthographicCamera();
        gameViewport = new FitViewport(800, 480, camara);

        stage = new Stage(new FitViewport(800, 480));
        Gdx.input.setInputProcessor(stage);

        try {
            texturaPuerta = new Texture(Gdx.files.internal("media/door.png"));
            mapRender = new MapRender();
        } catch (Exception e) {
            Gdx.app.error("INIT", "Error cargando recursos: " + e.getMessage());
        }

        cargarAnimaciones();
        construirUI();
    }

    private void cargarAnimaciones() {
        String[] colores = {"blanco", "negro", "amarillo", "azul", "verde", "rojo", "turquesa", "violeta"};
        for (int i = 0; i < colores.length; i++) {
            try {
                Texture tex = new Texture(Gdx.files.internal("media/skeleton_color" + (i + 1) + ".png"));
                texturasCargadas.add(tex);
                TextureRegion[][] frames = TextureRegion.split(tex, 112, 186);

                Map<String, Animation<TextureRegion>> anims = new HashMap<>();

                // IDLE: Fila 0, frames 0-3
                anims.put("idle", new Animation<>(0.2f, frames[0][0], frames[0][1], frames[0][2], frames[0][3]));
                // JUMP: Fila 1, frames 0-5
                anims.put("jump", new Animation<>(0.12f, frames[1][0], frames[1][1], frames[1][2], frames[1][3], frames[1][4], frames[1][5]));
                // RUN: Fila 2, frames 0-6
                anims.put("run", new Animation<>(0.1f, frames[2][0], frames[2][1], frames[2][2], frames[2][3], frames[2][4], frames[2][5], frames[2][6]));

                for (Animation<TextureRegion> a : anims.values()) a.setPlayMode(Animation.PlayMode.LOOP);
                animacionesPorColor.put(colores[i], anims);
            } catch (Exception e) {
                Gdx.app.error("ANIM", "Error cargando skeleton " + (i+1));
            }
        }
    }

    public void actualizarEstado(JsonValue data) {
        if (data == null) return;

        JsonValue playersJson = data.get("players");
        if (playersJson != null && playersJson.isArray()) {
            for (JsonValue pJson : playersJson) {
                String id = pJson.getString("id");
                float sX = pJson.getFloat("x", 0);
                float sY = pJson.getFloat("y", 0);

                DatosJugador dj = null;
                for (DatosJugador existente : jugadoresOnline) {
                    if (existente.id.equals(id)) { dj = existente; break; }
                }

                if (dj == null) {
                    dj = new DatosJugador();
                    dj.id = id;
                    dj.x = dj.targetX = sX;
                    dj.y = dj.targetY = sY;
                    dj.color = pJson.getString("color", "blanco").toLowerCase();
                    jugadoresOnline.add(dj);
                } else {
                    // Detección de estados antes de actualizar el objetivo
                    if (sX < dj.targetX) dj.mirandoIzquierda = true;
                    else if (sX > dj.targetX) dj.mirandoIzquierda = false;

                    dj.moviendose = Math.abs(sX - dj.targetX) > 0.5f;
                    dj.enAire = Math.abs(sY - dj.targetY) > 0.5f;

                    dj.targetX = sX;
                    dj.targetY = sY;
                }
                dj.nickname = pJson.getString("nickname", "Anon");
            }
        }

        JsonValue door = data.get("door");
        if (door != null) {
            this.puertaX = door.getFloat("x", 0);
            this.puertaY = door.getFloat("y", 0);
            this.puertaWidth = door.getFloat("width", 100);
            this.puertaHeight = door.getFloat("height", 150);
        }
    }

    @Override
    public void render(float delta) {
        enviarInput();

        Gdx.gl.glClearColor(0.03f, 0.04f, 0.06f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camara.position.set(camara.viewportWidth / 2f, (camara.viewportHeight / 2f) - 80, 0);
        camara.update();

        batch.setProjectionMatrix(camara.combined);
        batch.begin();

        if (mapRender != null) mapRender.render(batch);

        // Puerta
        if (texturaPuerta != null) {
            batch.draw(texturaPuerta, puertaX, toScreenY(puertaY, puertaHeight), puertaWidth, puertaHeight);
        }

        // Jugadores
        for (DatosJugador dj : jugadoresOnline) {
            dj.stateTime += delta;

            // LERP: Suaviza el movimiento entre frames del servidor
            dj.x = MathUtils.lerp(dj.x, dj.targetX, 0.25f);
            dj.y = MathUtils.lerp(dj.y, dj.targetY, 0.25f);

            String animKey = "idle";
            if (dj.enAire) animKey = "jump";
            else if (dj.moviendose) animKey = "run";

            Map<String, Animation<TextureRegion>> animMap = animacionesPorColor.getOrDefault(dj.color, animacionesPorColor.get("blanco"));
            TextureRegion frame = animMap.get(animKey).getKeyFrame(dj.stateTime, true);

            // Orientación (Flip)
            if (dj.mirandoIzquierda && !frame.isFlipX()) frame.flip(true, false);
            else if (!dj.mirandoIzquierda && frame.isFlipX()) frame.flip(true, false);

            batch.draw(frame, dj.x, toScreenY(dj.y, frame.getRegionHeight()));
        }

        batch.end();
        stage.act(delta);
        stage.draw();
    }

    private float toScreenY(float serverY, float height) {
        if (mapRender == null) return serverY;
        return mapRender.getMapHeightPixels() - serverY - height;
    }

    private void enviarInput() {
        if (cliente != null && cliente.isOpen()) {
            String json = "{\"type\":\"INPUT\",\"left\":" + isLeftPressed + ",\"right\":" + isRightPressed + ",\"jump\":" + isJumpPressed + "}";
            cliente.send(json);
        }
    }

    private void construirUI() {
        skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        skin.add("default", new BitmapFont());

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up = skin.newDrawable("white", Color.DARK_GRAY);
        style.down = skin.newDrawable("white", Color.GRAY);
        style.font = skin.getFont("default");
        skin.add("default", style);

        Table table = new Table();
        table.setFillParent(true);
        table.bottom().padBottom(20);

        TextButton btnL = new TextButton("<", skin);
        TextButton btnR = new TextButton(">", skin);
        TextButton btnJ = new TextButton("JUMP", skin);

        btnL.addListener(new ClickListener(){
            public boolean touchDown(InputEvent e, float x, float y, int p, int b){ isLeftPressed = true; return true; }
            public void touchUp(InputEvent e, float x, float y, int p, int b){ isLeftPressed = false; }
        });
        btnR.addListener(new ClickListener(){
            public boolean touchDown(InputEvent e, float x, float y, int p, int b){ isRightPressed = true; return true; }
            public void touchUp(InputEvent e, float x, float y, int p, int b){ isRightPressed = false; }
        });
        btnJ.addListener(new ClickListener(){
            public boolean touchDown(InputEvent e, float x, float y, int p, int b){ isJumpPressed = true; return true; }
            public void touchUp(InputEvent e, float x, float y, int p, int b){ isJumpPressed = false; }
        });

        table.add(btnL).size(80);
        table.add(btnR).size(80).padLeft(10).expandX().left();
        table.add(btnJ).size(100).right().padRight(20);
        stage.addActor(table);
        pixmap.dispose();
    }

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        stage.dispose();
        if (skin != null) skin.dispose();
        if (mapRender != null) mapRender.dispose();
        for (Texture t : texturasCargadas) t.dispose();
        if (texturaPuerta != null) texturaPuerta.dispose();
    }
}
