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

    public static float WORLD_HEIGHT = 600f;

    private Game game;
    private GameClient cliente;
    private SpriteBatch batch;

    private OrthographicCamera camara;
    private Viewport gameViewport;

    private Stage stage;
    private Skin skin;
    // --- CONSTANTES PARA CLEAN CODE ---
    private static final float CAMERA_LERP_SPEED = 0.1f;
    private static final float PUERTA_ESCALA_X = 0.7f;
    private static final float PUERTA_ESCALA_Y = 1.2f; // Antes tenías 1.2 en render y 0.7 en el server
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

    // --- VARIABLES DE LA PALANCA ---
    private Texture dungeonAssets;
    private TextureRegion palancaOff;
    private TextureRegion palancaOn;
    private boolean palancaVisible = false;
    private float palancaX, palancaY, palancaWidth, palancaHeight;
    private boolean isPalancaActivated = false;

    private MapRender mapRender;
    private int levelIndex;
    private List<Texture> texturasCargadas = new ArrayList<>();
    private Map<String, Map<String, Animation<TextureRegion>>> animacionesPorColor = new HashMap<>();
    private List<DatosJugador> jugadoresOnline = new ArrayList<>();

    private class DatosJugador {
        String id;
        String nickname;
        String color;
        float x, y;
        float targetX, targetY;
        float stateTime = 0f;
        boolean moviendose = false;
        boolean enAire = false;
        boolean mirandoIzquierda = false;
    }

    // CONSTRUCTOR SECUNDARIO: Usado por LoginScreen para iniciar en el Nivel 0
    public GameScreen(Game game, GameClient cliente) {
        this(game, cliente, null, 0);
    }

    // CONSTRUCTOR PRINCIPAL
    public GameScreen(Game game, GameClient cliente, JsonValue initialData, int levelIndex) {
        this.game = game;
        this.cliente = cliente;
        this.levelIndex = levelIndex;

        // 1. Configuración de Cámara y Viewport
        this.camara = new OrthographicCamera();
        this.gameViewport = new FitViewport(800, WORLD_HEIGHT, camara);
        this.batch = new SpriteBatch();

        // 2. Carga de Texturas
        texturaKey = new Texture(Gdx.files.internal("media/skeleton_key.png"));
        cargarAnimaciones();

        // Cargar palanca
        dungeonAssets = new Texture(Gdx.files.internal("media/assets_dungeon.png"));
        TextureRegion[][] regions = TextureRegion.split(dungeonAssets, 32, 32);
        palancaOff = regions[1][0];
        palancaOn = regions[1][1];

        // 3. Interfaz de Usuario (Controles)
        this.stage = new Stage(new FitViewport(800, 480));
        Gdx.input.setInputProcessor(this.stage);
        construirUI();

        // 4. Inicializar Mapa y Datos
        mapRender = new MapRender(this.levelIndex);

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

        // Corregido: Usamos WORLD_HEIGHT (600) para calcular las posiciones, no '200'
        if (data.has("door")) {
            JsonValue door = data.get("door");
            this.puertaX = door.getFloat("x", 0);
            this.puertaWidth = door.getFloat("width", 266);
            this.puertaHeight = door.getFloat("height", 310);
            this.puertaY = WORLD_HEIGHT - door.getFloat("y", 0) - this.puertaHeight;
        }

        if (data.has("key")) {
            JsonValue key = data.get("key");
            this.keyX = key.getFloat("x", 0);
            this.keyY = WORLD_HEIGHT - key.getFloat("y", 0) - 32;
        }
    }

    public void actualizarEstado(JsonValue data) {
        if (data == null) return;
// --- NUEVO: Sincronización de altura dinámica ---
        if (data.has("worldHeight")) {
            float nuevaAltura = data.getFloat("worldHeight");
            if (nuevaAltura != WORLD_HEIGHT) {
                WORLD_HEIGHT = nuevaAltura;
                // Actualizamos el viewport y la cámara para el nuevo tamaño
                gameViewport.setWorldSize(800, WORLD_HEIGHT);
                camara.position.set(400, WORLD_HEIGHT / 2f, 0);
            }
        }
        // 1. Actualizar Jugadores
        JsonValue playersJson = data.get("players");
        if (playersJson != null && playersJson.isArray()) {
            List<DatosJugador> nuevaLista = new ArrayList<>();
            for (JsonValue pJson : playersJson) {
                String id = pJson.getString("id");
                float sX = pJson.getFloat("x");

                float sY = convertirY(pJson.getFloat("y"), 90f);
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
                    // Detectar hacia dónde mira
                    if (sX < dj.targetX - 0.5f) dj.mirandoIzquierda = true;
                    else if (sX > dj.targetX + 0.5f) dj.mirandoIzquierda = false;

                    // Detectar si se mueve o está en el aire para las animaciones
                    dj.moviendose = Math.abs(sX - dj.targetX) > 0.5f;
                    dj.enAire = Math.abs(sY - dj.targetY) > 0.8f;

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
        if (data.has("key")) {
            JsonValue key = data.get("key");
            this.keyX = key.getFloat("x", 0);

            // CORRECCIÓN: Usar la altura de la hitbox que manda el servidor
            float hitboxLlaveServer = key.getFloat("height", 32f);
            this.keyY = convertirY(key.getFloat("y", 0), hitboxLlaveServer);

            this.keyCollected = key.getBoolean("collected", false);

            JsonValue holder = key.get("holderId");
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

                // CORRECCIÓN: Usar la altura de colisión del servidor, NO la visual escalada
                float hitboxPuertaServer = door.getFloat("height", 310f);
                this.puertaY = convertirY(door.getFloat("y"), hitboxPuertaServer);
            }

            if (door.has("opened")) {
                this.puertaAbierta = door.getBoolean("opened", false);
            }
        }

        // 4. Actualizar Palanca
        if (data.has("palanca")) {
            JsonValue palancaData = data.get("palanca");
            if (palancaData != null && !palancaData.isNull()) {
                palancaVisible = true;
                palancaX = palancaData.getFloat("x");
                palancaWidth = palancaData.getFloat("width", 32f);
                palancaHeight = palancaData.getFloat("height", 32f);
                isPalancaActivated = palancaData.getBoolean("activated", false);

                // Invertimos la Y de la palanca
                this.palancaY = convertirY(palancaData.getFloat("y"), palancaHeight);
            }
        } else {
            palancaVisible = false;
        }
    }

    /**
     * Convierte la coordenada Y del servidor a la Y de LibGDX usando la altura REAL del TiledMap.
     */
    private float convertirY(float serverY, float alturaVisualElemento) {
        if (mapRender == null) return 0f;
        float mapHeight = mapRender.getMapHeightPixels();
        return mapHeight - serverY - alturaVisualElemento;
    }


    // --- RENDERIZADO ---

    @Override
    public void render(float delta) {
        enviarInput();

        // --- 0. ACTUALIZAR POSICIONES (LERP) ANTES DE LA CÁMARA ---
        for (DatosJugador jugador : jugadoresOnline) {
            jugador.stateTime += delta;
            jugador.x = MathUtils.lerp(jugador.x, jugador.targetX, 0.20f);
            jugador.y = MathUtils.lerp(jugador.y, jugador.targetY, 0.30f);
        }

        // --- 1. LÓGICA DE SEGUIMIENTO DE CÁMARA ---
        float targetCamX = 400;
        float targetCamY = 240;

        for (DatosJugador p : jugadoresOnline) {
            if (cliente != null && cliente.getMyId() != null && cliente.getMyId().equals(p.id)) {
                targetCamX = p.x + (30f / 2f);
                targetCamY = p.y + (90f / 2f);
                break;
            }
        }

        if (mapRender != null) {
            float mapW = mapRender.getMapWidthPixels();
            float mapH = mapRender.getMapHeightPixels();
            float halfViewW = camara.viewportWidth / 2f;
            float halfViewH = camara.viewportHeight / 2f;

            targetCamX = MathUtils.clamp(targetCamX, halfViewW, mapW - halfViewW);
            targetCamY = MathUtils.clamp(targetCamY, halfViewH, mapH - halfViewH);
        }

        camara.position.set(targetCamX, targetCamY, 0);
        camara.update();
        // ------------------------------------------

        Gdx.gl.glClearColor(0.03f, 0.04f, 0.06f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camara.combined);
        batch.begin();

        // CAPA 1: EL MAPA (Se dibuja primero para que quede de fondo)
        if (mapRender != null) mapRender.render(batch);

        // CAPA 2: LLAVE EN EL SUELO
        if (keyHolderId == null && !keyCollected) {
            globalTime += delta;
            float pulse = 0.9f + MathUtils.sin(globalTime * 4f) * 0.3f;

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1, 0.9f, 0, 0.5f);
            float haloSize = keyWidth * 2.5f * pulse;
            batch.draw(texturaKey,
                keyX - (haloSize - keyWidth) / 2,
                keyY - (haloSize - keyHeight) / 2,
                haloSize, haloSize);

            batch.setColor(Color.WHITE);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.draw(texturaKey, keyX, keyY, keyWidth, keyHeight);
        }

        // CAPA 3: LA PALANCA
        if (palancaVisible) {
            TextureRegion palancaActual = isPalancaActivated ? palancaOn : palancaOff;
            batch.draw(palancaActual, palancaX, palancaY, palancaWidth, palancaHeight);
        }

// CAPA 4: LA PUERTA
        if (puertaAbierta) {
            doorStateTime += delta;
        }
        TextureRegion currentDoorFrame = animacionPuerta.getKeyFrame(doorStateTime);
        float anchoEscalado = puertaWidth * PUERTA_ESCALA_X;
        float altoEscalado = puertaHeight * PUERTA_ESCALA_Y;
        float offsetPuertaX = -40f;

        batch.draw(currentDoorFrame, puertaX + offsetPuertaX, puertaY, anchoEscalado, altoEscalado);


// CAPA 5: JUGADORES
// CAPA 5: JUGADORES
        for (DatosJugador jugador : jugadoresOnline) {
            String animKey = (jugador.enAire) ? "jump" : (jugador.moviendose ? "run" : "idle");
            Animation<TextureRegion> anim = animacionesPorColor.getOrDefault(jugador.color, animacionesPorColor.get("blanco")).get(animKey);
            TextureRegion frame = anim.getKeyFrame(jugador.stateTime, true);

            // --- 1. AJUSTE EJE X (Corregir el desplazamiento a la derecha) ---
            // Tu imagen mide 112 pero tu colisión real (hitbox) mide 30.
            // Restamos 41 para centrar el dibujo sobre la colisión invisible.
            float offsetX = 41f;
            float drawX = jugador.x - offsetX;

            if (jugador.mirandoIzquierda) {
                // Al invertir el dibujo (ancho negativo), ajustamos el punto de anclaje
                drawX = jugador.x + 112 - offsetX;
            }

            // --- 2. AJUSTE EJE Y (Quitar la levitación base y la levitación al correr) ---
            // Valor negativo para empujar el dibujo hacia abajo y que los pies pisen la línea de la colisión
            float offsetY = -20f;

            if (animKey.equals("run")) {
                // El frame de correr del artista levita un poco más, así que lo hundimos unos píxeles extra
                offsetY -= 0f;
            }

            batch.draw(frame,
                drawX,
                jugador.y + offsetY,
                jugador.mirandoIzquierda ? -112 : 112,
                186);

            // --- 3. DIBUJAR LLAVE SOBRE LA CABEZA ---
            if (keyHolderId != null && jugador.id.equals(keyHolderId)) {
                // Centramos la llave respecto a la colisión real (30px), no a la imagen de 112px
                float llaveX = jugador.x - 1f;

                // Colocamos la llave justo encima de la cabeza real (altura 90) + un pequeño margen de 15px
                float llaveY = jugador.y + 90f + 15f;

                // Si el personaje está corriendo (se agacha visualmente), bajamos la llave para que le siga
                if (animKey.equals("run")) {
                    llaveY -= 5f;
                }

                batch.draw(texturaKey, llaveX, llaveY, keyWidth, keyHeight);
            }
        }
        batch.end();

        // UI
        stage.act(delta);
        stage.draw();
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

    private void cargarAnimaciones() {
        texturaPuertaSheet = new Texture(Gdx.files.internal("media/door.png"));
        texturasCargadas.add(texturaPuertaSheet);
        TextureRegion[][] doorFrames = TextureRegion.split(texturaPuertaSheet, 266, 310);
        animacionPuerta = new Animation<>(0.15f, doorFrames[0]);
        animacionPuerta.setPlayMode(Animation.PlayMode.NORMAL);

        String[] colores = {"blanco", "negro", "amarillo", "azul", "verde", "rojo", "turquesa", "violeta"};
        for (int i = 0; i < colores.length; i++) {
            Texture tex = new Texture(Gdx.files.internal("media/skeleton_color" + (i + 1) + ".png"));
            texturasCargadas.add(tex);
            TextureRegion[][] frames = TextureRegion.split(tex, 112, 186);

            Map<String, Animation<TextureRegion>> anims = new HashMap<>();
            anims.put("idle", new Animation<>(0.2f, frames[0][0], frames[0][1], frames[0][2], frames[0][3]));
            anims.put("jump", new Animation<>(0.12f, frames[1][0], frames[1][1], frames[1][2], frames[1][3], frames[1][4]));
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
        estilo.up = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.5f));
        estilo.down = skin.newDrawable("white", new Color(0.4f, 0.4f, 0.4f, 0.7f));
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
        if (dungeonAssets != null) dungeonAssets.dispose();
    }
}
