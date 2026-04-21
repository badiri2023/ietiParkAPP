package com.ietipiko.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class GameClient extends WebSocketClient {

    private LoginScreen pantallaLogin;
    private GameScreen pantallaJuego;
    private JsonReader jsonReader = new JsonReader();

    public GameClient(URI serverUri, LoginScreen pantallaLogin) {
        super(serverUri);
        this.pantallaLogin = pantallaLogin;
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
            if (type.equals("STATE_UPDATE")) {
                final JsonValue data = json.get("data"); // El servidor envía "data"
                Gdx.app.postRunnable(() -> {
                    if (pantallaJuego != null) {
                        // Pasamos el objeto 'data' que contiene 'players' y 'world'
                        pantallaJuego.actualizarEstado(data);
                    }
                });
            }

            // 2. RECIBIR BIENVENIDA
            else if (type.equals("WELCOME")) {
                Gdx.app.postRunnable(() -> {
                    if (pantallaLogin != null) pantallaLogin.irAlJuego();
                });
            }

            // 3. RECIBIR LISTA DE JUGADORES (Para el Lobby/Login)
            else if (type.equals("PLAYER_LIST")) {
                final JsonValue data = json.get("data");
                final String[] nombres = new String[data.size];
                for (int i = 0; i < data.size; i++) {
                    nombres[i] = data.get(i).getString("nickname");
                }
                Gdx.app.postRunnable(() -> {
                    if (pantallaLogin != null) pantallaLogin.actualizarLista(nombres);
                });
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
