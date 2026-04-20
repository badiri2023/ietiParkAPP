package com.ietipiko.game;

import com.badlogic.gdx.Gdx;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class GameClient extends WebSocketClient {

    private LoginScreen pantallaLogin;
    private GraveyardScreen pantallaGraveyard;
    private String nickname; // Guardamos el nickname

    public GameClient(URI serverUri, LoginScreen pantallaLogin, String nickname) {
        super(serverUri);
        this.pantallaLogin = pantallaLogin;
        this.nickname = nickname;
    }

    public void setPantallaGraveyard(GraveyardScreen pantalla) {
        this.pantallaGraveyard = pantalla;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Conexión abierta. Enviando JOIN...");

        // Enviamos el mensaje JOIN en formato JSON como espera el servidor Node
        String joinJson = "{\"type\":\"JOIN\", \"nickname\":\"" + nickname + "\"}";
        this.send(joinJson);

        Gdx.app.postRunnable(() -> {
            pantallaLogin.irAlGraveyard(this);
        });
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Mensaje del server: " + message);

        // Forma sencilla de detectar la lista de jugadores sin usar una librería JSON compleja todavía.
        // Tu servidor Node hace: sala.broadcast("PLAYER_LIST", sala.getPlayerList());
        // Dependiendo de cómo tengas hecho el "broadcast", llegará un JSON.
        // Asumiendo que el JSON contiene "PLAYER_LIST":

        if (message.contains("PLAYER_LIST")) {
            // Nota: Aquí lo ideal es usar la clase JsonReader de libGDX para leer el JSON real.
            // Esto es un parche rápido si tu servidor enviase algo plano.
            // Para leer JSON real en libGDX:
            // JsonReader reader = new JsonReader();
            // JsonValue root = reader.parse(message);
            // Si quieres parsear la lista exacta, adaptaremos esto al JSON de tu server.

            Gdx.app.postRunnable(() -> {
                if (pantallaGraveyard != null) {
                    // pantallaGraveyard.actualizarLista(...);
                }
            });
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Conexión cerrada. Código: " + code + " Razón: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        Gdx.app.postRunnable(() -> {
            if (pantallaLogin != null) {
                pantallaLogin.mostrarDialogo("Error", "No se pudo conectar: " + ex.getMessage());
            }
        });
    }
}
