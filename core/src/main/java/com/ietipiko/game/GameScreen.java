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
    private float globalTime = 0f;
    // --- VARIABLES DE LA PUERTA ANIMADA ---
    private Texture texturaPuertaSheet;
    private Animation<TextureRegion> animacionPuerta;
    private float doorStateTime = 0f;
    private boolean puertaAbierta = false;

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
        cargarAnimaciones(); // Aquí dentro cargamos a los jugadores y la puerta

        // 3. Interfaz de Usuario (Controles)
        this.stage = new Stage(new FitViewport(800, 480));
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

        float worldHeightServer = 400f;
        float WORLD_HEIGHT = 400f;

        // 1. Actualizar Jugadores
        JsonValue playersJson = data.get("players");
        if (playersJson != null && playersJson.isArray()) {
            List<DatosJugador> nuevaLista = new ArrayList<>();

            for (JsonValue pJson : playersJson) {
                String id = pJson.getString("id");
                float sX = pJson.getFloat("x");
                float sY = WORLD_HEIGHT - pJson.getFloat("y") - 90;

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

        // 2. Actualizar Llave
        if (data.hasChild("key")) {
            JsonValue key = data.get("key");
            this.keyX = key.getFloat("x", 0);
            this.keyY = WORLD_HEIGHT - key.getFloat("y", 0) - 32;
            this.keyCollected = key.getBoolean("collected", false);

            // Buscamos el hijo "holderId"
            JsonValue holder = key.get("holderId");

            // Si el hijo existe y su valor no es el literal 'null' de JSON
            if (holder != null && !holder.isNull()) {
                this.keyHolderId = holder.asString();
            } else {
                this.keyHolderId = null;
            }
        }

        // 3. Actualizar Puerta
        if (data.has("door")) {
            JsonValue door = data.get("door");
            if (door.has("x") && door.has("y")) {
                this.puertaX = door.getFloat("x");
                this.puertaY = WORLD_HEIGHT - door.getFloat("y") - 80;
            }

            // Comprobamos si el servidor nos dice que está abierta
            if (door.has("opened")) {
                this.puertaAbierta = door.getBoolean("opened", false);
            }
        }
    }

    // --- RENDERIZADO ---

    @Override
    public void render(float delta) {
        enviarInput();

        Gdx.gl.glClearColor(0.03f, 0.04f, 0.06f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camara.position.set(400, 300, 0);
        camara.update();

        batch.setProjectionMatrix(camara.combined);
        batch.begin();

        // CAPA 1: LLAVE EN EL SUELO (Solo si nadie la tiene)
        if (keyHolderId == null && !keyCollected) {
            globalTime += delta;

            // Calculamos un factor de pulsación (va de 0.6 a 1.2)
            float pulse = 0.9f + MathUtils.sin(globalTime * 4f) * 0.3f;

            // --- DIBUJAR EL HALO ---

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1, 0.9f, 0, 0.5f); // Color amarillo con 50% transparencia

            // Dibujamos la textura de la llave un poco más grande y centrada
            float haloSize = keyWidth * 2.5f * pulse;
            batch.draw(texturaKey,
                keyX - (haloSize - keyWidth) / 2,
                keyY - (haloSize - keyHeight) / 2,
                haloSize, haloSize);

            // Restauramos el color y el modo de mezcla normal
            batch.setColor(Color.WHITE);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            // --- DIBUJAR LA LLAVE REAL ---
            batch.draw(texturaKey, keyX, keyY, keyWidth, keyHeight);
        }

        // CAPA 2: EL MAPA (Base del escenario)
        if (mapRender != null) mapRender.render(batch);

        // CAPA 3: LA PUERTA (Sobre el mapa)
        if (puertaAbierta) {
            doorStateTime += delta;
        }
        TextureRegion currentDoorFrame = animacionPuerta.getKeyFrame(doorStateTime);

        float escala = 0.7f;
        float anchoEscalado = puertaWidth * escala;
        float altoEscalado = puertaHeight * escala;
        float offsetPuertaX = -80f;

        batch.draw(currentDoorFrame, puertaX + offsetPuertaX, puertaY, anchoEscalado, altoEscalado);

        // CAPA 4: JUGADORES Y LLAVE "PEGADA"
        for (DatosJugador jugador : jugadoresOnline) {
            jugador.stateTime += delta;

            jugador.x = MathUtils.lerp(jugador.x, jugador.targetX, 0.20f);
            jugador.y = MathUtils.lerp(jugador.y, jugador.targetY, 0.30f);

            String animKey = (jugador.enAire) ? "jump" : (jugador.moviendose ? "run" : "idle");
            Animation<TextureRegion> anim = animacionesPorColor.getOrDefault(jugador.color, animacionesPorColor.get("blanco")).get(animKey);
            TextureRegion frame = anim.getKeyFrame(jugador.stateTime, true);

            // --- CORRECCIÓN DE POSICIÓN ---
            // Esto evita que el personaje parezca "hundido" en el suelo.
            float correccionHitbox = 26f;

            // 2. Ajuste visual para la animación de correr
            float ajusteAnimacion = (animKey.equals("run")) ? -40f : 0f;

            float finalOffsetY = correccionHitbox + ajusteAnimacion;

            // Dibujar esqueleto
            batch.draw(frame,
                jugador.mirandoIzquierda ? jugador.x + 112 : jugador.x,
                jugador.y + finalOffsetY,
                jugador.mirandoIzquierda ? -112 : 112,
                186);

            // Dibujar llave sobre la cabeza
            if (keyHolderId != null && jugador.id.equals(keyHolderId)) {
                float llaveX = jugador.x + 40;
                // La llave también debe subir para compensar el hundimiento del cuerpo
                float llaveY = jugador.y + 140 + finalOffsetY;
                if (animKey.equals("run")) {
                    llaveY += 35f;
                }
                batch.draw(texturaKey, llaveX, llaveY, keyWidth, keyHeight);
            }
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
        // --- ANIMACIÓN DE LA PUERTA ---
        texturaPuertaSheet = new Texture(Gdx.files.internal("media/door.png"));
        texturasCargadas.add(texturaPuertaSheet); // Lo añadimos a la lista para el dispose

        // Cortamos el sprite de la puerta. Tiene 6 columnas (frames) en 1 fila
        TextureRegion[][] doorFrames = TextureRegion.split(texturaPuertaSheet, 266, 310);

        // El tiempo es 0.15f por frame (puedes ajustarlo para que abra más rápido o más lento)
        animacionPuerta = new Animation<>(0.15f, doorFrames[0]);

        // Le indicamos que solo se reproduzca una vez y se quede en el final (la puerta abierta)
        animacionPuerta.setPlayMode(Animation.PlayMode.NORMAL);

        // --- ANIMACIÓN DE LOS ESQUELETOS ---
        String[] colores = {"blanco", "negro", "amarillo", "azul", "verde", "rojo", "turquesa", "violeta"};
        for (int i = 0; i < colores.length; i++) {
            Texture tex = new Texture(Gdx.files.internal("media/skeleton_color" + (i + 1) + ".png"));
            texturasCargadas.add(tex);
            TextureRegion[][] frames = TextureRegion.split(tex, 112, 186);

            Map<String, Animation<TextureRegion>> anims = new HashMap<>();

            // IDLE
            anims.put("idle", new Animation<>(0.2f, frames[0][0], frames[0][1], frames[0][2], frames[0][3]));
            // JUMP
            anims.put("jump", new Animation<>(0.12f, frames[1][0], frames[1][1], frames[1][2], frames[1][3], frames[1][4]));
            // RUN
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

        Color colorTransparente = new Color(0.2f, 0.2f, 0.2f, 0.5f);
        Color colorPulsado = new Color(0.4f, 0.4f, 0.4f, 0.7f);

        estilo.up = skin.newDrawable("white", colorTransparente);
        estilo.down = skin.newDrawable("white", colorPulsado);
        estilo.font = skin.getFont("default");
        estilo.fontColor = new Color(1, 1, 1, 0.8f);

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

        tabla.add(btnL).size(60);
        tabla.add(btnR).size(60).padLeft(20).expandX().left();
        tabla.add(btnJ).size(60).right().padRight(20);
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
        for (Texture t : texturasCargadas) t.dispose(); // Esto borra tanto esqueletos como la puerta
        texturaKey.dispose();
    }
}
