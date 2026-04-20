package com.ietipiko.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class GameClient extends WebSocketClient {

    private LoginScreen pantallaLogin;
    // NUEVO: Referencia a la pantalla de juego
    private GameScreen pantallaJuego;

    private JsonReader jsonReader = new JsonReader();

    public GameClient(URI serverUri, LoginScreen pantallaLogin) {
        super(serverUri);
        this.pantallaLogin = pantallaLogin;
    }

    // NUEVO: Método para vincular la pantalla de juego cuando entramos a la partida
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
            // Convertimos el mensaje a JSON
            JsonValue json = jsonReader.parse(message);
            String type = json.getString("type");

            if (type.equals("WELCOME")) {
                // El servidor nos aceptó, pasamos a GameScreen
                Gdx.app.postRunnable(() -> {
                    if (pantallaLogin != null) pantallaLogin.irAlJuego();
                });
            }
            else if (type.equals("PLAYER_LIST")) {
                JsonValue data = json.get("data");
                String[] nombres = new String[data.size];
                for (int i = 0; i < data.size; i++) {
                    nombres[i] = data.get(i).getString("nickname");
                }
                Gdx.app.postRunnable(() -> {
                    if (pantallaLogin != null) pantallaLogin.actualizarLista(nombres);
                });
            }
            // ==========================================
            // NUEVO: LEER POSICIONES DURANTE LA PARTIDA
            // ==========================================
            else if (type.equals("STATE_UPDATE")) {
                JsonValue players = json.get("players");
                // Le pasamos los datos a la pantalla de juego (si ya está creada)
                if (pantallaJuego != null) {
                    pantallaJuego.actualizarEstado(players);
                }
            }

        } catch (Exception e) {
            System.out.println("Error procesando mensaje: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Conexión cerrada.");
    }

    @Override
    public void onError(Exception ex) {
        Gdx.app.postRunnable(() -> {
            if (pantallaLogin != null) pantallaLogin.mostrarDialogo("Error", "Servidor offline");
        });
    }
}
