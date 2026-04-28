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
        String color;
        float x, y;               // Posición visual (suave)
        float targetX, targetY;   // Posición real del servidor
        float stateTime = 0f;
        boolean moviendose = false;
        boolean enAire = false;
        boolean mirandoIzquierda = false;
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
        float worldHeightServer = 400f;
        float WORLD_HEIGHT = 400f;

        // 2. Actualizar Jugadores
        JsonValue playersJson = data.get("players");
        if (playersJson != null && playersJson.isArray()) {
            List<DatosJugador> nuevaLista = new ArrayList<>();

            for (JsonValue pJson : playersJson) {
                String id = pJson.getString("id");
                float sX = pJson.getFloat("x");
                float sY = WORLD_HEIGHT - pJson.getFloat("y") - 160;

                // Buscar si el jugador ya existía
                DatosJugador dj = null;
                for (DatosJugador existente : jugadoresOnline) {
                    if (existente.id.equals(id)) {
                        dj = existente;
                        break;
                    }
                }

                if (dj == null) {
                    dj = new DatosJugador();
                    dj.id = id;
                    dj.x = dj.targetX = sX;
                    dj.y = dj.targetY = sY;
                } else {
                    // DETECCIÓN DE MOVIMIENTO Y SALTO
                    if (sX < dj.targetX) dj.mirandoIzquierda = true;
                    else if (sX > dj.targetX) dj.mirandoIzquierda = false;

                    dj.moviendose = Math.abs(sX - dj.targetX) > 0.5f;
                    dj.enAire = Math.abs(sY - dj.targetY) > 0.5f;

                    dj.targetX = sX;
                    dj.targetY = sY;
                }

                dj.nickname = pJson.getString("nickname", "Player");
                dj.color = pJson.getString("color", "blanco").toLowerCase();
                nuevaLista.add(dj);
            }
            jugadoresOnline = nuevaLista;
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

        // Dibujar Jugadores
// Dibujar Jugadores
        for (DatosJugador jugador : jugadoresOnline) {
            jugador.stateTime += delta;

            // Movimiento suave
            jugador.x = MathUtils.lerp(jugador.x, jugador.targetX, 0.20f);
            jugador.y = MathUtils.lerp(jugador.y, jugador.targetY, 0.30f);

            // Lógica de estado
            String animKey = (jugador.enAire) ? "jump" : (jugador.moviendose ? "run" : "idle");

            // Dibujado seguro
            Animation<TextureRegion> anim = animacionesPorColor.getOrDefault(jugador.color, animacionesPorColor.get("blanco")).get(animKey);
            TextureRegion frame = anim.getKeyFrame(jugador.stateTime, true);

            // --- EL TRUCO DEL OFFSET (Compensación) ---
            float offsetY = 0f;

            if (animKey.equals("run")) {
                // Restamos píxeles para "bajar" al personaje.
                // Prueba con -10f, -15f o -20f hasta que los pies toquen el suelo.
                offsetY = -40f;
            } else if (animKey.equals("jump")) {
                // Si notas que al saltar también flota raro o se hunde, puedes ajustarlo aquí
                offsetY = 0f;
            }

            // Usamos el draw con flip incluido y le sumamos el offsetY a la posición Y
            batch.draw(frame,
                jugador.mirandoIzquierda ? jugador.x + 112 : jugador.x, // Ajuste X por el Flip
                jugador.y + offsetY,                                    // Ajuste Y para que no flote
                jugador.mirandoIzquierda ? -112 : 112,                  // Ancho (Negativo hace Flip)
                186);                                                   // Alto
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
            texturasCargadas.add(tex);
            TextureRegion[][] frames = TextureRegion.split(tex, 112, 186);

            Map<String, Animation<TextureRegion>> anims = new HashMap<>();

            // IDLE: Fila 0, frames 0-3
            anims.put("idle", new Animation<>(0.2f, frames[0][0], frames[0][1], frames[0][2], frames[0][3]));
            // JUMP: Fila 1, frames 0-5
            anims.put("jump", new Animation<>(0.12f, frames[1][0], frames[1][1], frames[1][2], frames[1][3], frames[1][4]));
            // RUN: Fila 2, frames 0-6
            anims.put("run", new Animation<>(0.1f, frames[2][0], frames[2][1], frames[2][2], frames[2][3], frames[2][4], frames[2][5], frames[2][6]));

            for (Animation<TextureRegion> a : anims.values()) a.setPlayMode(Animation.PlayMode.LOOP);
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
