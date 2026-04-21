package com.ietipiko.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class GraveyardScreen extends ScreenAdapter {

    private Game game;
    private GameClient cliente;
    private Stage stage;
    private Skin skin;
    private Table tablaJugadores;

    public GraveyardScreen(Game game, GameClient cliente) {
        this.game = game;
        this.cliente = cliente;
        this.stage = new Stage(new FitViewport(640, 360));

        prepararSkin();
        construirInterfaz();
    }

    private void prepararSkin() {
        skin = new Skin();
        skin.add("default", new BitmapFont());

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("lineaBlanca", new Texture(pixmap));

        skin.add("titulo", new Label.LabelStyle(skin.getFont("default"), Color.WHITE));
        skin.add("lista", new Label.LabelStyle(skin.getFont("default"), Color.LIGHT_GRAY));
    }

    private void construirInterfaz() {
        Table tablaPrincipal = new Table();
        tablaPrincipal.setFillParent(true);
        tablaPrincipal.top();

        Label titulo = new Label("GRAVEYARD", skin, "titulo");
        titulo.setFontScale(2.2f);

        Image linea = new Image(skin.getDrawable("lineaBlanca"));

        tablaJugadores = new Table();
        tablaJugadores.top();

        tablaPrincipal.add(titulo).padTop(30).row();
        tablaPrincipal.add(linea).width(450).height(2).padTop(10).padBottom(20).row();
        tablaPrincipal.add(tablaJugadores).expand().fill();

        stage.addActor(tablaPrincipal);
    }

    public void actualizarLista(String[] nombres) {
        tablaJugadores.clearChildren();
        for (String nombre : nombres) {
            Label lbl = new Label(nombre, skin, "lista");
            lbl.setFontScale(1.3f);
            tablaJugadores.add(lbl).padBottom(8).row();
        }
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
