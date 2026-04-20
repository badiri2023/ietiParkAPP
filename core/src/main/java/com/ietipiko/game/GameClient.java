package com.ietipiko.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class GameClient extends WebSocketClient {

    private LoginScreen pantallaLogin;
    private JsonReader jsonReader;

    public GameClient(URI serverUri, LoginScreen pantallaLogin) {
        super(serverUri);
        this.pantallaLogin = pantallaLogin;
        this.jsonReader = new JsonReader(); // El traductor de LibGDX
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("¡Conectado al servidor!");
    }

    @Override
    public void onMessage(String message) {
        try {
            // 1. Convertimos el texto que llega en un objeto JSON
            JsonValue json = jsonReader.parse(message);
            String tipo = json.getString("type");

            // 2. Si el servidor nos manda la lista de jugadores...
            if (tipo.equals("PLAYER_LIST")) {
                JsonValue data = json.get("data");
                String[] nombres = new String[data.size];

                for (int i = 0; i < data.size; i++) {
                    // Extraemos el nickname de cada jugador en la lista
                    nombres[i] = data.get(i).getString("nickname");
                }

                // 3. Actualizamos la interfaz de Android
                Gdx.app.postRunnable(() -> {
                    if (pantallaLogin != null) {
                        pantallaLogin.actualizarLista(nombres);
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("Error al leer el mensaje del servidor: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Conexión cerrada.");
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Error de red: " + ex.getMessage());
    }
}
