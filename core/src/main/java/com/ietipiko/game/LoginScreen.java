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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
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

    public LoginScreen(Game game) {
        this.game = game;
        stage = new Stage(new FitViewport(640, 360));
        Gdx.input.setInputProcessor(stage);

        crearEstilos();
        construirInterfaz();
    }

    private void crearEstilos() {
        skin = new Skin();
        skin.add("default", new BitmapFont());

        // Fondos personalizados
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

        // Estilos de UI
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
        Table tabla = new Table();
        tabla.setFillParent(true);

        Label titulo = new Label("Picko Skull", skin);
        titulo.setFontScale(1.8f);

        final TextField campoNombre = new TextField("", skin);
        campoNombre.setMessageText("Introduce your name");
        campoNombre.setAlignment(Align.center);

        TextButton btnEntrar = new TextButton("WAKE UP", skin);

        btnEntrar.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String nickname = campoNombre.getText();
                if (!nickname.trim().isEmpty()) {
                    conectarAlServidor(nickname);
                } else {
                    mostrarDialogo("Aviso", "¡Tu esqueleto necesita un nombre!");
                }
            }
        });

        tabla.add(titulo).expandY().top().padTop(40).row();
        tabla.add(campoNombre).width(300).height(40).expandY().center().row();
        tabla.add(btnEntrar).width(200).height(50).expandY().bottom().padBottom(40);

        stage.addActor(tabla);
    }

    // Añadimos 'String nickname' entre los paréntesis
    private void conectarAlServidor(String nickname) {
        try {
            // Recuerda usar ws://10.0.2.2:3000 si estás en el emulador
            URI uri = new URI("ws://10.0.2.2:3000");

            // ¡Aquí está la magia! Le pasamos el nickname como tercer parámetro
            GameClient cliente = new GameClient(uri, this, nickname);

            cliente.connect();
            mostrarDialogoCarga("Conectando...", "Merging souls...");
        } catch (Exception e) {
            mostrarDialogo("Error Fatal", "La dirección del servidor no es válida.");
        }
    }

    // --- GESTIÓN DE DIÁLOGOS (POP-UPS) ---

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

    // --- NAVEGACIÓN ---

    public void irAlGraveyard(GameClient cliente) {
        if (dialogoActual != null) dialogoActual.remove();
        // Saltamos a la pantalla del cementerio pasando el cliente activo
        game.setScreen(new GraveyardScreen(game, cliente));
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
    }
}
