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

    private Stage stage;
    private Skin skin;

    // Estados de entrada
    private boolean isLeftPressed = false;
    private boolean isRightPressed = false;
    private boolean isJumpPressed = false;

    // Entidades del Mundo (Valores por defecto según tu JSON)
    private float puertaX, puertaY;
    private float puertaWidth = 266, puertaHeight = 310;
    private float keyX, keyY;
    private float keyWidth = 32, keyHeight = 32;
    private boolean keyCollected = false;
    private String keyHolderId = null;

    private Texture texturaKey;
    private Texture texturaPuerta;
    private MapRender mapRender;

    private List<Texture> texturasCargadas = new ArrayList<>();
    private Map<String, Map<String, Animation<TextureRegion>>> animacionesPorColor = new HashMap<>();
    private List<DatosJugador> jugadoresOnline = new ArrayList<>();

    private class DatosJugador {
        String id;
        String nickname;
        float x, y;
        String color;
        float stateTime = 0f;
    }

    public GameScreen(Game game, GameClient cliente, JsonValue initialData) {
        this.game = game;
        this.cliente = cliente;

        // 1. Configuración de Cámara y Viewport (Sincronizado con el Server 400x200)
        this.camara = new OrthographicCamera();
        this.gameViewport = new FitViewport(800, 600, camara);
        this.batch = new SpriteBatch();

        // 2. Carga de Texturas
        texturaKey = new Texture(Gdx.files.internal("media/skeleton_key.png"));
        texturaPuerta = new Texture(Gdx.files.internal("media/door.png"));
        cargarAnimaciones();

        // 3. Interfaz de Usuario (Controles)
        this.stage = new Stage(new FitViewport(800, 480)); // UI con resolución más alta para botones claros
        Gdx.input.setInputProcessor(this.stage);
        construirUI();

        // 4. Inicializar Mapa y Datos
        mapRender = new MapRender();

        if (this.cliente != null) {
            this.cliente.setPantallaJuego(this);
        }

        // Si tenemos datos iniciales del mundo (WORLD_INIT), los cargamos ya
        if (initialData != null) {
            inicializarMundo(initialData);
        }
    }

    // --- PROCESAMIENTO DE DATOS ---

    public void inicializarMundo(JsonValue data) {
        if (data == null) return;

        if (data.has("door")) {
            JsonValue door = data.get("door");
            this.puertaX = door.getFloat("x", 0);
            // Inversión Y: alto_mundo(200) - y_server - alto_puerta
            this.puertaY = 200 - door.getFloat("y", 0) - door.getFloat("height", 310);
            this.puertaWidth = door.getFloat("width", 266);
            this.puertaHeight = door.getFloat("height", 310);
        }

        if (data.has("key")) {
            JsonValue key = data.get("key");
            this.keyX = key.getFloat("x", 0);
            this.keyY = 200 - key.getFloat("y", 0) - 32;
        }
    }

    public void actualizarEstado(JsonValue data) {
        if (data == null) return;

        // 1. Obtenemos las dimensiones del mundo que vienen del servidor
        // Si no vienen en el STATE_UPDATE, usa el valor de tu JSON (ej: 600)
        float worldHeightServer = 600f;
        float WORLD_HEIGHT = 600f;

        // 2. Actualizar Jugadores
        JsonValue playersJson = data.get("players");
        if (playersJson != null && playersJson.isArray()) {
            jugadoresOnline.clear();
            for (JsonValue pJson : playersJson) {
                DatosJugador dj = new DatosJugador();
                dj.id = pJson.getString("id");
                dj.nickname = pJson.getString("nickname", "Player");

                // X se queda igual
                dj.x = pJson.getFloat("x");

                // FÓRMULA MAESTRA: AltoTotal - Y_Servidor - Alto_Sprite
                // Esto convierte el (0,0) Arriba-Izquierda a (0,0) Abajo-Izquierda
                dj.y = WORLD_HEIGHT - pJson.getFloat("y") - 186;

                dj.color = pJson.getString("color", "blanco").toLowerCase();

                // Log para verificar: Ahora Y debería ser un número positivo (ej: 50.0)
                Gdx.app.log("DEBUG_PLAYER", "ID: " + dj.id + " | X: " + dj.x + " | Y: " + dj.y);

                jugadoresOnline.add(dj);
            }
        }

        // 3. Actualizar Llave
        if (data.has("key")) {
            JsonValue key = data.get("key");
            this.keyX = key.getFloat("x");
            this.keyY = worldHeightServer - key.getFloat("y") - 32; // 32 es el alto de la llave
            this.keyCollected = key.getBoolean("collected", false);
        }
        if (data.has("door")) {
            JsonValue door = data.get("door");
            if (door.has("x") && door.has("y")) {
                // Forzamos la puerta a entrar en pantalla para que la veas:
                this.puertaX = door.getFloat("x");
                // Como el log dice -390, le sumamos para subirla al suelo
                this.puertaY = WORLD_HEIGHT - door.getFloat("y") - 80;
            }

            // 4. Actualizar Puerta
        /*if (data.has("door")) {
            JsonValue door = data.get("door");
            // Solo intentamos leer X e Y si el objeto los trae
            if (door.has("x") && door.has("y")) {
                this.puertaX = door.getFloat("x");
                this.puertaY = worldHeightServer - door.getFloat("y") - 80;
            }
            // opened siempre suele estar
            this. = door.getBoolean("opened", false);
        }*/
        }
    }

    // --- RENDERIZADO ---

    @Override
    public void render(float delta) {
        enviarInput();

        Gdx.gl.glClearColor(0.03f, 0.04f, 0.06f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Centramos la cámara en el centro del mundo (200, 100)
       // camara.position.set(200, 100, 0);
        camara.position.set(400, 300, 0);
        camara.update();

        batch.setProjectionMatrix(camara.combined);
        batch.begin();

        if (mapRender != null) mapRender.render(batch);

        // 1. Dibujar Puerta
        batch.draw(texturaPuerta, puertaX, puertaY, puertaWidth, puertaHeight);

        // 2. Dibujar Jugadores
        for (DatosJugador jugador : jugadoresOnline) {
            jugador.stateTime += delta;

            // Seguridad: Si el color no existe, usamos "blanco"
            Map<String, Animation<TextureRegion>> colorAnims = animacionesPorColor.get(jugador.color);
            if (colorAnims == null) colorAnims = animacionesPorColor.get("blanco");

            Animation<TextureRegion> idle = colorAnims.get("idle");
            TextureRegion frame = idle.getKeyFrame(jugador.stateTime);
            Gdx.app.log("DEBUG_RENDER", "Dibujando puerta en X:" + puertaX + " Y:" + puertaY);

            batch.draw(frame, jugador.x, jugador.y);

            // Dibujar llave sobre el jugador que la tiene
            if (keyHolderId != null && keyHolderId.equals(jugador.id)) {
                batch.draw(texturaKey, jugador.x + 20, jugador.y + 100, 24, 24);
            }
        }

        // 3. Dibujar Llave en el suelo
        if (keyHolderId == null && !keyCollected) {
            batch.draw(texturaKey, keyX, keyY, keyWidth, keyHeight);
        }

        batch.end();

        stage.act(delta);
        stage.draw();
    }

    // --- GESTIÓN DE INPUTS Y UI ---

    private void enviarInput() {
        if (cliente != null && cliente.isOpen()) {
            String json = "{\"type\":\"INPUT\","
                + "\"left\":"  + isLeftPressed  + ","
                + "\"right\":" + isRightPressed + ","
                + "\"jump\":"  + isJumpPressed  + "}";
            cliente.send(json);
        }
    }

    private void cargarAnimaciones() {
        String[] colores = {"blanco", "negro", "amarillo", "azul", "verde", "rojo", "turquesa", "violeta"};
        for (int i = 0; i < colores.length; i++) {
            Texture tex = new Texture(Gdx.files.internal("media/skeleton_color" + (i + 1) + ".png"));
            String path = "media/skeleton_color" + (i + 1) + ".png";
            Gdx.app.log("ASSET_LOAD", "Cargando esqueleto: " + colores[i] + " desde " + path);
            texturasCargadas.add(tex);
            TextureRegion[][] frames = TextureRegion.split(tex, 112, 186);
            Animation<TextureRegion> idleAnim = new Animation<>(0.20f, frames[0][0], frames[0][1], frames[0][2], frames[0][3]);
            idleAnim.setPlayMode(Animation.PlayMode.LOOP);

            Map<String, Animation<TextureRegion>> anims = new HashMap<>();
            anims.put("idle", idleAnim);
            animacionesPorColor.put(colores[i], anims);
        }
    }

    private void construirUI() {
        skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        skin.add("default", new BitmapFont());

        TextButton.TextButtonStyle estilo = new TextButton.TextButtonStyle();
        estilo.up = skin.newDrawable("white", Color.DARK_GRAY);
        estilo.down = skin.newDrawable("white", Color.GRAY);
        estilo.font = skin.getFont("default");
        skin.add("default", estilo);

        Table tabla = new Table();
        tabla.setFillParent(true);
        tabla.bottom().padBottom(20);

        TextButton btnL = new TextButton("<", skin);
        TextButton btnR = new TextButton(">", skin);
        TextButton btnJ = new TextButton("UP", skin);

        btnL.addListener(new ClickListener() {
            public boolean touchDown(InputEvent e, float x, float y, int p, int b) { isLeftPressed = true; return true; }
            public void touchUp(InputEvent e, float x, float y, int p, int b) { isLeftPressed = false; }
        });
        btnR.addListener(new ClickListener() {
            public boolean touchDown(InputEvent e, float x, float y, int p, int b) { isRightPressed = true; return true; }
            public void touchUp(InputEvent e, float x, float y, int p, int b) { isRightPressed = false; }
        });
        btnJ.addListener(new ClickListener() {
            public boolean touchDown(InputEvent e, float x, float y, int p, int b) { isJumpPressed = true; return true; }
            public void touchUp(InputEvent e, float x, float y, int p, int b) { isJumpPressed = false; }
        });

        tabla.add(btnL).size(80);
        tabla.add(btnR).size(80).padLeft(20).expandX().left();
        tabla.add(btnJ).size(80).right().padRight(20);
        stage.addActor(tabla);
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
        skin.dispose();
        if (mapRender != null) mapRender.dispose();
        for (Texture t : texturasCargadas) t.dispose();
        texturaKey.dispose();
        texturaPuerta.dispose();
    }
}
