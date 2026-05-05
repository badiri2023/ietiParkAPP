package com.ietipiko.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class GameClient extends WebSocketClient {

    private Game game;
    private LoginScreen pantallaLogin;
    private GameScreen pantallaJuego;
    private JsonReader jsonReader = new JsonReader();
    private int currentLevel = 0;
    private String myId = null;

    public GameClient(URI serverUri, LoginScreen pantallaLogin, Game game) {
        super(serverUri);
        this.pantallaLogin = pantallaLogin;
        this.game = game;
    }

    public String getMyId() {
        return this.myId;
    }

    public void setPantallaJuego(GameScreen pantallaJuego) {
        this.pantallaJuego = pantallaJuego;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Conectado al servidor.");
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonValue json = jsonReader.parse(message);
            String type = json.getString("type");

            switch (type) {
                case "STATE_UPDATE":
                    final JsonValue stateData = json.get("data");
                    Gdx.app.postRunnable(() -> {
                        if (pantallaJuego != null) pantallaJuego.actualizarEstado(stateData);
                    });
                    break;

                case "WORLD_INIT":
                    final JsonValue initData = json.get("data");
                    Gdx.app.postRunnable(() -> {
                        GameScreen nuevaPantalla = new GameScreen(game, this, initData, currentLevel);
                        this.pantallaJuego = nuevaPantalla;
                        game.setScreen(nuevaPantalla);
                    });
                    break;

                case "CHANGE_LEVEL":
                    currentLevel++;

                    final JsonValue changeData = json.get("data");
                    final JsonValue newWorldData = changeData != null && changeData.has("world") ? changeData.get("world") : null;

                    Gdx.app.postRunnable(() -> {
                        if (pantallaJuego != null) {
                            pantallaJuego.dispose();
                        }

                        GameScreen nuevaPantalla = new GameScreen(game, GameClient.this, newWorldData, currentLevel);

                        this.pantallaJuego = nuevaPantalla;
                        game.setScreen(nuevaPantalla);

                        System.out.println("¡Pasando al nivel " + currentLevel + "!");
                    });
                    break;

                case "PLAYER_LIST":
                    final JsonValue listData = json.get("data");
                    final String[] nombres = new String[listData.size];
                    for (int i = 0; i < listData.size; i++) {
                        nombres[i] = listData.get(i).getString("nickname");
                    }
                    Gdx.app.postRunnable(() -> {
                        if (pantallaLogin != null) pantallaLogin.actualizarLista(nombres);
                    });
                    break;

                case "WELCOME":
                    this.myId = json.getString("id");
                    System.out.println("Servidor: Bienvenido. Mi ID asignada es: " + this.myId);
                    break;

                default:
                    System.out.println("Mensaje recibido no reconocido: " + type);
                    break;
            }

        } catch (Exception e) {
            System.out.println("Error procesando mensaje: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Conexión cerrada: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Error en la conexión: " + ex.getMessage());
        Gdx.app.postRunnable(() -> {
            if (pantallaLogin != null) pantallaLogin.mostrarDialogo("Error", "Servidor offline o error de red");
        });
    }
}
