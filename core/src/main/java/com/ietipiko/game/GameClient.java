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

    public GameClient(URI serverUri, LoginScreen pantallaLogin, Game game) {
        super(serverUri);
        this.pantallaLogin = pantallaLogin;
        this.game = game;
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

            // 1. RECIBIR EL ESTADO DEL JUEGO (Posiciones y Colores)
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
                        GameScreen nuevaPantalla = new GameScreen(game, this, initData);
                        this.pantallaJuego = nuevaPantalla;
                        game.setScreen(nuevaPantalla);
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
                    System.out.println("Servidor: Bienvenido.");
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
