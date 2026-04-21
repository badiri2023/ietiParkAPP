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
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.net.URI;

public class LoginScreen extends ScreenAdapter {

    private Game game;
    private Stage stage;
    private Skin skin;
    private Dialog dialogoActual;

    private Table tablaJugadores;
    private GameClient cliente;

    public LoginScreen(Game game) {
        this.game = game;
        stage = new Stage(new FitViewport(800, 480));
        Gdx.input.setInputProcessor(stage);

        crearEstilos();
        construirInterfaz();

        conectarAlServidorInicial();
    }

    private void crearEstilos() {
        skin = new Skin();
        skin.add("default", new BitmapFont());

        Pixmap pixmapGris = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmapGris.setColor(new Color(0.12f, 0.12f, 0.15f, 1f));
        pixmapGris.fill();
        skin.add("fondoGris", new Texture(pixmapGris));

        Pixmap pixmapAzul = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmapAzul.setColor(new Color(0.15f, 0.25f, 0.35f, 1f));
        pixmapAzul.fill();
        skin.add("fondoAzul", new Texture(pixmapAzul));

        Pixmap pixmapBlanco = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmapBlanco.setColor(Color.WHITE);
        pixmapBlanco.fill();
        skin.add("blanco", new Texture(pixmapBlanco));

        Label.LabelStyle labelStyle = new Label.LabelStyle(skin.getFont("default"), Color.LIGHT_GRAY);
        skin.add("default", labelStyle);

        TextField.TextFieldStyle textStyle = new TextField.TextFieldStyle();
        textStyle.font = skin.getFont("default");
        textStyle.fontColor = Color.WHITE;
        textStyle.background = skin.getDrawable("fondoGris");
        textStyle.cursor = skin.getDrawable("blanco");
        skin.add("default", textStyle);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = skin.getFont("default");
        btnStyle.fontColor = Color.WHITE;
        btnStyle.up = skin.getDrawable("fondoAzul");
        skin.add("default", btnStyle);

        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = skin.getFont("default");
        windowStyle.titleFontColor = Color.RED;
        windowStyle.background = skin.getDrawable("fondoGris");
        skin.add("default", windowStyle);
    }

    private void construirInterfaz() {
        Table root = new Table();
        root.setFillParent(true);

        Table ladoLogin = new Table();
        Label titulo = new Label("Picko Skull", skin);
        titulo.setFontScale(1.8f);

        final TextField campoNombre = new TextField("", skin);
        campoNombre.setMessageText("Introduce your name");
        campoNombre.setAlignment(Align.center);

        TextButton btnEntrar = new TextButton("WAKE UP", skin);

        btnEntrar.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String nickname = campoNombre.getText().trim();
                if (!nickname.isEmpty()) {
                    if (cliente != null && cliente.isOpen()) {
                        mostrarDialogoCarga("Conectando...", "Merging souls...");
                        // Enviamos el JSON correcto al servidor
                        cliente.send("{\"type\":\"JOIN\", \"nickname\":\"" + nickname + "\"}");
                    } else {
                        conectarAlServidorInicial();
                    }
                } else {
                    mostrarDialogo("Aviso", "¡Tu esqueleto necesita un nombre!");
                }
            }
        });

        ladoLogin.add(titulo).expandY().top().padTop(40).row();
        ladoLogin.add(campoNombre).width(300).height(40).expandY().center().row();
        ladoLogin.add(btnEntrar).width(200).height(50).expandY().bottom().padBottom(40);


        Image lineaSeparadora = new Image(skin.getDrawable("blanco"));

        Table ladoGraveyard = new Table();
        ladoGraveyard.top();

        Label subTitulo = new Label("GRAVEYARD", skin);
        subTitulo.setColor(Color.GRAY);
        subTitulo.setFontScale(1.3f);

        tablaJugadores = new Table();
        tablaJugadores.top();

        ladoGraveyard.add(subTitulo).padTop(40).padBottom(20).row();
        ladoGraveyard.add(new ScrollPane(tablaJugadores)).expand().fill();

        root.add(ladoGraveyard).expand().fill();
        root.add(lineaSeparadora).width(2).fillY().padTop(20).padBottom(20);
        root.add(ladoLogin).expand().fill();

        stage.addActor(root);
    }

    private void conectarAlServidorInicial() {
        try {

            String ipServidor = "pico4.ieti.site";
            String puerto = "443";

            URI uri = new URI("wss://" + ipServidor + ":" + puerto);
            cliente = new GameClient(uri, this);
            cliente.connect();
        } catch (Exception e) {
            System.out.println("No se pudo iniciar la conexión automática: " + e.getMessage());
        }
    }

    // Este método lo llama el GameClient cuando recibe la lista en JSON
    public void actualizarLista(String[] nombres) {
        tablaJugadores.clearChildren();
        for (String nombre : nombres) {
            Label lblJugador = new Label(nombre, skin);
            lblJugador.setFontScale(1.1f);
            tablaJugadores.add(lblJugador).pad(5).row();
        }
    }

    public void irAlJuego() {
        Gdx.app.postRunnable(() -> {
            if (dialogoActual != null) dialogoActual.remove();
            game.setScreen(new GameScreen(game, cliente));
        });
    }

    public void mostrarDialogoCarga(String titulo, String mensaje) {
        if (dialogoActual != null) dialogoActual.remove();

        dialogoActual = new Dialog(titulo, skin);
        dialogoActual.text(mensaje);
        dialogoActual.getContentTable().pad(50);
        dialogoActual.show(stage);
    }

    public void mostrarDialogo(String titulo, String mensaje) {
        if (dialogoActual != null) dialogoActual.remove();

        dialogoActual = new Dialog(titulo, skin) {
            @Override
            protected void result(Object object) {
                dialogoActual = null;
            }
        };
        dialogoActual.text(mensaje);
        dialogoActual.button("Aceptar", true);
        dialogoActual.getContentTable().padTop(40).padBottom(20).padLeft(60).padRight(60);
        dialogoActual.getButtonTable().padBottom(20);
        dialogoActual.show(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.04f, 0.06f, 1);
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
        if (cliente != null) cliente.close();
    }
}
