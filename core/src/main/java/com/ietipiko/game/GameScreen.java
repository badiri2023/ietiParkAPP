package com.ietipiko.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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

    // INTERFAZ Y CONTROLES
    private Stage stage;
    private Skin skin;
    private boolean isLeftPressed = false;
    private boolean isRightPressed = false;
    private boolean isJumpPressed = false;
    private float puertaX, puertaY, puertaWidth, puertaHeight;
    private Texture texturaPuerta; // No olvides cargarla en cargarAnimaciones()
    // MAPA
    private MapRender mapRender;

    // SISTEMA DE COLORES
    private List<Texture> texturasCargadas = new ArrayList<>();
    private Map<String, TextureRegion> animacionesPorColor = new HashMap<>();

    private List<DatosJugador> jugadoresOnline = new ArrayList<>();

    // CLASE DE DATOS
    private class DatosJugador {
        String id;
        String nickname;
        float x;
        float y;
        String color;
    }
    private JsonValue initialData; // Guardamos los datos de inicio

    public GameScreen(Game game, GameClient cliente,JsonValue initialData) {
        this.game = game;
        this.cliente = cliente;
        this.initialData = initialData; // <--- GUARDAMOS EL MUNDO


        if (this.cliente != null) {
            this.cliente.setPantallaJuego(this);
        }

        this.batch = new SpriteBatch();

        this.camara = new OrthographicCamera();
        this.gameViewport = new FitViewport(800, 480, camara);

        this.stage = new Stage(new FitViewport(800, 480));
        Gdx.input.setInputProcessor(this.stage);

        cargarAnimaciones();
        construirUI();

        // Cargamos el mapa
        mapRender = new MapRender();
    }

    private void cargarAnimaciones() {
        // CAMBIO: Todo en minúsculas para coincidir con "colors.js" del servidor
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
            Texture tex = new Texture(Gdx.files.internal(archivosPng[i]));
            texturasCargadas.add(tex);

            TextureRegion[][] frames = TextureRegion.split(tex, 112, 186);
            animacionesPorColor.put(nombresColores[i], frames[0][0]);
        }
    }

    // =========================================================
    // Crea un botón transparente y con hitbox circular
    // =========================================================
    private TextButton crearBotonCircular(String texto, Skin skin) {
        TextButton boton = new TextButton(texto, skin) {
            @Override
            public Actor hit(float x, float y, boolean touchable) {
                // Calculamos si el toque del usuario está dentro del círculo
                float radio = getWidth() / 2f;
                float centroX = radio;
                float centroY = getHeight() / 2f;

                float distancia = (float) Math.sqrt(Math.pow(x - centroX, 2) + Math.pow(y - centroY, 2));

                if (distancia <= radio) {
                    return super.hit(x, y, touchable); // Tocó el círculo
                }
                return null; // Tocó la esquina vacía (ignorar)
            }
        };

        // Transparencia al 50%
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

        // Usamos nuestro nuevo método para crear los botones
        TextButton btnIzquierda = crearBotonCircular("<", skin);
        TextButton btnDerecha = crearBotonCircular(">", skin);
        TextButton btnSalto = crearBotonCircular("SALTO", skin);

        btnIzquierda.addListener(crearListenerBoton("left"));
        btnDerecha.addListener(crearListenerBoton("right"));
        btnSalto.addListener(crearListenerBoton("jump"));

        // Ajustamos las medidas para que sean cuadradas (90x90), así el radio hace un círculo perfecto
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

    // MÉTODO CORREGIDO: Recibe el objeto "data" completo del GameClient
    public void actualizarEstado(JsonValue data) {
        if (data == null) return;

        // --- PARTE 1: JUGADORES ---
        JsonValue playersJson = data.get("players");
        if (playersJson != null && playersJson.isArray()) {
            jugadoresOnline.clear();
            for (JsonValue pJson : playersJson) {
                // SEGURIDAD: Solo procesamos si existe el campo "id"
                if (pJson.has("id")) {
                    DatosJugador dj = new DatosJugador();
                    dj.id = pJson.getString("id");
                    dj.nickname = pJson.getString("nickname", "Anon");
                    dj.x = pJson.getFloat("x", 0);
                    dj.y = pJson.getFloat("y", 0);
                    dj.color = pJson.getString("color", "blanco").toLowerCase();
                    jugadoresOnline.add(dj);
                }
            }
        }

        // --- PARTE 2: MUNDO (PUERTA) ---
        JsonValue world = data.get("world");
        if (world != null && world.has("door")) {
            JsonValue door = world.get("door");
            this.puertaX = door.getFloat("x", 0);
            this.puertaY = door.getFloat("y", 0);
            this.puertaWidth = door.getFloat("width", 50);
            this.puertaHeight = door.getFloat("height", 50);
        }
    }
    @Override
    public void render(float delta) {
        enviarInput();

        Gdx.gl.glClearColor(0.03f, 0.04f, 0.06f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // CAMBIO DE CÁMARA: Empujamos la cámara hacia abajo (-80 px)
        // para que el mapa se vea más arriba en la pantalla.
        camara.position.set(camara.viewportWidth / 2f, (camara.viewportHeight / 2f) - 80, 0);
        camara.update();

        batch.setProjectionMatrix(camara.combined);

        batch.begin();

        if (mapRender != null) {
            mapRender.render(batch);
        }

        for (DatosJugador jugador : jugadoresOnline) {
            TextureRegion texturaJugador = animacionesPorColor.get(jugador.color);
            if (texturaJugador == null) {
                // CAMBIO: Valor por defecto en minúscula
                texturaJugador = animacionesPorColor.get("blanco");
            }
            batch.draw(texturaJugador, jugador.x, jugador.y);
        }

        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true); // El 'true' centra la cámara temporalmente
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        stage.dispose();
        if (skin != null) skin.dispose();
        if (mapRender != null) mapRender.dispose();

        for (Texture tex : texturasCargadas) {
            tex.dispose();
        }
    }
}
