package com.ietipiko.game;

import com.badlogic.gdx.Gdx;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class GameClient extends WebSocketClient {

    private LoginScreen pantallaLogin;
    private GraveyardScreen pantallaGraveyard;

    public GameClient(URI serverUri, LoginScreen pantallaLogin) {
        super(serverUri);
        this.pantallaLogin = pantallaLogin;
    }

    public void setPantallaGraveyard(GraveyardScreen pantalla) {
        this.pantallaGraveyard = pantalla;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Gdx.app.postRunnable(() -> {
            pantallaLogin.irAlGraveyard(this);
        });
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Mensaje: " + message);
        if (message.startsWith("PLAYERS:")) {
            String contenido = message.substring(8);
            final String[] nombres = contenido.split(",");

            Gdx.app.postRunnable(() -> {
                if (pantallaGraveyard != null) {
                    pantallaGraveyard.actualizarLista(nombres);
                }
            });
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Conexión cerrada.");
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
