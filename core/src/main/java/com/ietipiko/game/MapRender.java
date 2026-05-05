package com.ietipiko.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

public class MapRender {
    // Listas para guardar las imágenes y matrices de cada capa
    private List<Texture> tilesets = new ArrayList<>();
    private List<TextureRegion[][]> tileRegionsList = new ArrayList<>();
    private List<int[][]> tileMaps = new ArrayList<>();

    // Dimensiones
    private int mapWidth;
    private int mapHeight;
    private int tileWidth;
    private int tileHeight;

    public MapRender(int levelIndex) {
        FileHandle file = Gdx.files.internal("game_data.json");
        JsonReader reader = new JsonReader();
        JsonValue base = reader.parse(file);

        // Ahora lee el nivel que le pidamos
        JsonValue level = base.get("levels").get(levelIndex);
        JsonValue layers = level.get("layers");

        for (int i = 0; i < layers.size; i++) {
            JsonValue layer = layers.get(i);

            String tilesSheetFile = layer.getString("tilesSheetFile");
            Texture tileset = new Texture(Gdx.files.internal(tilesSheetFile));
            tilesets.add(tileset);


            tileWidth = layer.getInt("tilesWidth");
            tileHeight = layer.getInt("tilesHeight");
            TextureRegion[][] regions = TextureRegion.split(tileset, tileWidth, tileHeight);
            tileRegionsList.add(regions);


            String tileMapFileName = layer.getString("tileMapFile");
            FileHandle mapFile = Gdx.files.internal(tileMapFileName);

            // Leemos el archivo JSON del mapa (ej. level_000_layer_000.json)
            JsonValue archivoCompleto = reader.parse(mapFile);


            JsonValue tileMapJson = archivoCompleto.get("tileMap");

            // Calculamos el alto y ancho de la matriz
            int height = tileMapJson.size;
            int width = tileMapJson.get(0).size;
            int[][] tileMap = new int[height][width];

            // Rellenamos nuestra matriz de Java con los datos del JSON
            for (int y = 0; y < height; y++) {
                JsonValue fila = tileMapJson.get(y);
                for (int x = 0; x < width; x++) {
                    tileMap[y][x] = fila.getInt(x);
                }
            }
            tileMaps.add(tileMap);

            if (i == 0) {
                mapWidth = width;
                mapHeight = height;
            }
        }
    }

    public void render(SpriteBatch batch) {
        // Dibujamos una capa detrás de otra (primero el suelo, luego los muros)
        for (int layerIndex = 0; layerIndex < tileMaps.size(); layerIndex++) {
            int[][] tileMap = tileMaps.get(layerIndex);
            TextureRegion[][] tileRegions = tileRegionsList.get(layerIndex);

            // Calculamos cuántos "cuadraditos" tiene la imagen original de ancho
            int tilesetCols = tilesets.get(layerIndex).getWidth() / tileWidth;

            // Recorremos la matriz dibujando cada cuadradito
            for (int y = 0; y < mapHeight; y++) {
                for (int x = 0; x < mapWidth; x++) {
                    int tileIndex = tileMap[y][x];

                    // Si el índice es negativo, significa que ahí no hay nada que dibujar (transparente)
                    if (tileIndex < 0) continue;

                    // Calculamos qué trozo de la imagen corresponde a este número
                    int row = tileIndex / tilesetCols;
                    int col = tileIndex % tilesetCols;
                    TextureRegion region = tileRegions[row][col];

                    // Dibujamos el cuadradito.
                    // NOTA: Invertimos la 'y' porque LibGDX dibuja de abajo hacia arriba.
                    batch.draw(region, x * tileWidth, (mapHeight - 1 - y) * tileHeight);
                }
            }
        }
    }

    public void dispose() {
        for (Texture tileset : tilesets) {
            if (tileset != null) {
                tileset.dispose();
            }
        }
    }

    public float getMapWidthPixels() {
        return this.mapWidth * tileWidth;
    }
    public float getMapHeightPixels() {
        return this.mapHeight * tileHeight;
    }
}
