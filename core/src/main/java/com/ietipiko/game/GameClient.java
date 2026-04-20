package com.ietipiko.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class GameClient extends WebSocketClient {

    private LoginScreen pantallaLogin;
    private JsonReader jsonReader = new JsonReader();

    public GameClient(URI serverUri, LoginScreen pantallaLogin) {
        super(serverUri);
        this.pantallaLogin = pantallaLogin;
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
                // ¡ESTA ES LA CLAVE! El servidor nos aceptó.
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
        } catch (Exception e) {
            System.out.println("Error procesando mensaje: " + message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) { }

    @Override
    public void onError(Exception ex) {
        Gdx.app.postRunnable(() -> {
            if (pantallaLogin != null) pantallaLogin.mostrarDialogo("Error", "Servidor offline");
        });
    }
}
