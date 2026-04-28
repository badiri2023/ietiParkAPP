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

    // Texturas creadas en constructor (con contexto GL)
    private Texture texturaPuerta;

    // UI y controles
    private Stage stage;
    private Skin skin;
    private boolean isLeftPressed = false;
    private boolean isRightPressed = false;
    private boolean isJumpPressed = false;

    // Puerta (valores recibidos del servidor)
    private float puertaX = 0, puertaY = 0, puertaWidth = 0, puertaHeight = 0;

    // MapRender
    private MapRender mapRender;

    // Animaciones: color -> (state -> Animation)
    private List<Texture> texturasCargadas = new ArrayList<>();
    private Map<String, Map<String, Animation<TextureRegion>>> animacionesPorColor = new HashMap<>();

    // Jugadores
    private List<DatosJugador> jugadoresOnline = new ArrayList<>();

    private class DatosJugador {
        String id;
        String nickname;
        float x, y;
        float prevX, prevY;
        String color;
        float stateTime = 0f;
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

        // Crear texturas que requieren GL en constructor
        try {
            texturaPuerta = new Texture(Gdx.files.internal("media/door.png"));
        } catch (Exception e) {
            System.err.println("[GameScreen] No se pudo cargar media/door.png: " + e.getMessage());
            texturaPuerta = null;
        }

        cargarAnimaciones();
        construirUI();

        try {
            mapRender = new MapRender();
        } catch (Exception e) {
            System.err.println("[GameScreen] Error inicializando MapRender: " + e.getMessage());
            mapRender = null;
        }
    }

    /**
     * Carga animaciones por color. Protege índices fuera de rango y crea fallbacks.
     */
    private void cargarAnimaciones() {
        String[] nombresColores = {"blanco", "negro", "amarillo", "azul", "verde", "rojo", "turquesa", "violeta"};
        String[] archivosPng = {
            "media/skeleton_color1.png",
            "media/skeleton_color2.png",
            "media/skeleton_color3.png",
            "media/skeleton_color4.png",
            "media/skeleton_color5.png",
            "media/skeleton_color6.png",
            "media/skeleton_color7.png",
            "media/skeleton_color8.png"
        };

        for (int i = 0; i < nombresColores.length; i++) {
            Texture tex;
            try {
                tex = new Texture(Gdx.files.internal(archivosPng[i]));
                texturasCargadas.add(tex);
            } catch (Exception e) {
                System.err.println("[GameScreen] No se pudo cargar " + archivosPng[i] + ": " + e.getMessage());
                continue;
            }

            TextureRegion[][] frames = TextureRegion.split(tex, 112, 186);

            // IDLE (frames 0..3) con fallback
            Animation<TextureRegion> idle;
            try {
                idle = new Animation<>(0.20f, frames[0][0], frames[0][1], frames[0][2], frames[0][3]);
                idle.setPlayMode(Animation.PlayMode.LOOP);
            } catch (Exception e) {
                idle = new Animation<>(0.20f, frames[0][0]);
                idle.setPlayMode(Animation.PlayMode.LOOP);
            }

            // RUN (frames 14..20) fallback a idle
            Animation<TextureRegion> run;
            try {
                run = new Animation<>(0.10f,
                    frames[0][14], frames[0][15], frames[0][16],
                    frames[0][17], frames[0][18], frames[0][19], frames[0][20]);
                run.setPlayMode(Animation.PlayMode.LOOP);
            } catch (Exception e) {
                run = idle;
            }

            // JUMP (frames 7..11) fallback a idle
            Animation<TextureRegion> jump;
            try {
                jump = new Animation<>(0.12f,
                    frames[0][7], frames[0][8], frames[0][9],
                    frames[0][10], frames[0][11]);
                jump.setPlayMode(Animation.PlayMode.NORMAL);
            } catch (Exception e) {
                jump = idle;
            }

            Map<String, Animation<TextureRegion>> anims = new HashMap<>();
            anims.put("idle", idle);
            anims.put("run", run);
            anims.put("jump", jump);

            animacionesPorColor.put(nombresColores[i], anims);
        }

        // fallback "blanco" si no existe
        if (!animacionesPorColor.containsKey("blanco") && !animacionesPorColor.isEmpty()) {
            String any = animacionesPorColor.keySet().iterator().next();
            animacionesPorColor.put("blanco", animacionesPorColor.get(any));
        }
    }

    private TextButton crearBotonCircular(String texto, Skin skin) {
        TextButton boton = new TextButton(texto, skin) {
            @Override
            public Actor hit(float x, float y, boolean touchable) {
                float radio = getWidth() / 2f;
                float centroX = radio;
                float centroY = getHeight() / 2f;
                float distancia = (float) Math.sqrt(Math.pow(x - centroX, 2) + Math.pow(y - centroY, 2));
                if (distancia <= radio) return super.hit(x, y, touchable);
                return null;
            }
        };
        boton.getColor().a = 0.5f;
        return boton;
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

        TextButton btnIzquierda = crearBotonCircular("<", skin);
        TextButton btnDerecha = crearBotonCircular(">", skin);
        TextButton btnSalto = crearBotonCircular("SALTO", skin);

        btnIzquierda.addListener(crearListenerBoton("left"));
        btnDerecha.addListener(crearListenerBoton("right"));
        btnSalto.addListener(crearListenerBoton("jump"));

        tabla.add(btnIzquierda).width(90).height(90).padRight(20);
        tabla.add(btnDerecha).width(90).height(90).expandX().left();
        tabla.add(btnSalto).width(90).height(90).right().padRight(20);

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
        if ("left".equals(accion)) isLeftPressed = valor;
        if ("right".equals(accion)) isRightPressed = valor;
        if ("jump".equals(accion)) isJumpPressed = valor;
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

    /**
     * Actualiza jugadores y puerta. Preserva prevX/prevY para detectar movimiento.
     */
    public void actualizarEstado(JsonValue data) {
        if (data == null) return;

        Map<String, DatosJugador> existing = new HashMap<>();
        for (DatosJugador d : jugadoresOnline) existing.put(d.id, d);

        List<DatosJugador> newList = new ArrayList<>();
        JsonValue playersJson = data.get("players");
        if (playersJson != null && playersJson.isArray()) {
            for (JsonValue pJson : playersJson) {
                if (!pJson.has("id")) continue;
                String id = pJson.getString("id");
                float x = pJson.getFloat("x", 0);
                float y = pJson.getFloat("y", 0);
                String color = pJson.getString("color", "blanco").toLowerCase();
                String nickname = pJson.getString("nickname", "Anon");

                DatosJugador dj = existing.get(id);
                if (dj == null) {
                    dj = new DatosJugador();
                    dj.id = id;
                    dj.nickname = nickname;
                    dj.x = x;
                    dj.y = y;
                    dj.prevX = x;
                    dj.prevY = y;
                    dj.color = color;
                    dj.stateTime = 0f;
                } else {
                    dj.prevX = dj.x;
                    dj.prevY = dj.y;
                    dj.x = x;
                    dj.y = y;
                    dj.nickname = nickname;
                    dj.color = color;
                }
                newList.add(dj);
            }
        }
        jugadoresOnline = newList;

        JsonValue door = data.get("door");
        if (door != null) {
            this.puertaX = door.getFloat("x", 0);
            this.puertaY = door.getFloat("y", 0);
            this.puertaWidth = door.getFloat("width", (texturaPuerta != null ? texturaPuerta.getWidth() : 50));
            this.puertaHeight = door.getFloat("height", (texturaPuerta != null ? texturaPuerta.getHeight() : 50));
        }
    }

    private float toScreenY(float serverY, float spriteHeight) {
        if (mapRender == null) return serverY;
        return mapRender.getMapHeightPixels() - serverY - spriteHeight;
    }

    @Override
    public void render(float delta) {
        try {
            enviarInput();

            Gdx.gl.glClearColor(0.03f, 0.04f, 0.06f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            camara.position.set(camara.viewportWidth / 2f, (camara.viewportHeight / 2f) - 80, 0);
            camara.update();

            batch.setProjectionMatrix(camara.combined);

            batch.begin();

            if (mapRender != null) mapRender.render(batch);

            if (texturaPuerta != null && puertaWidth > 0 && puertaHeight > 0) {
                float drawDoorY = toScreenY(puertaY, puertaHeight);
                batch.draw(texturaPuerta, puertaX, drawDoorY, puertaWidth, puertaHeight);
            }

            for (DatosJugador jugador : jugadoresOnline) {
                jugador.stateTime += delta;

                float dx = jugador.x - jugador.prevX;
                float dy = jugador.y - jugador.prevY;

                String anim = "idle";
                if (Math.abs(dy) > 1f) anim = "jump";
                else if (Math.abs(dx) > 1f) anim = "run";

                Map<String, Animation<TextureRegion>> animMap = animacionesPorColor.get(jugador.color);
                if (animMap == null) animMap = animacionesPorColor.get("blanco");
                if (animMap == null) continue;

                Animation<TextureRegion> animation = animMap.get(anim);
                if (animation == null) animation = animMap.get("idle");
                if (animation == null) continue;

                TextureRegion frame = animation.getKeyFrame(jugador.stateTime, true);

                float drawY = toScreenY(jugador.y, frame.getRegionHeight());
                batch.draw(frame, jugador.x, drawY);

                jugador.prevX = jugador.x;
                jugador.prevY = jugador.y;
            }

            batch.end();

            stage.act(delta);
            stage.draw();
        } catch (Exception e) {
            System.err.println("[GameScreen] Exception in render: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        try { batch.dispose(); } catch (Exception ignored) {}
        try { stage.dispose(); } catch (Exception ignored) {}
        if (skin != null) skin.dispose();
        if (mapRender != null) mapRender.dispose();
        if (texturaPuerta != null) try { texturaPuerta.dispose(); } catch (Exception ignored) {}
        for (Texture tex : texturasCargadas) {
            try { tex.dispose(); } catch (Exception ignored) {}
        }
    }
}
