package server.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import server.Game;
import server.Server;
import server.Socket;
import server.observer.Observer;
import server.utility.JSONBuilder;

public class GameHandler implements MessageHandler {
    private static final int MAX_GAMES = 5;

    private final Observer observer = Observer.getInstance();

    private static Server server = Server.getInstance();
    private static Map<String, Game> games = new HashMap<>();

    public void handle(JSONObject object) throws JSONException, IOException {
        JSONObject data = object.getJSONObject("data");
        String action = data.getString("action");
        switch (action) {
        case "getList":
            sendGameList();
            break;
        case "create":
            createGame(data.getString("userName"), data.getString("gameName"));
            break;
        case "join":
            joinGame(data.getString("userName"), data.getString("gameName"));
            break;
        case "leave":
            leaveGame(data.getString("userName"), data.getString("gameName"));
            break;
        case "delete":
            break;
        default:
            throw new IllegalArgumentException("Invalid game action");
        }
    }

    /**
     * Create a game with specified name
     * 
     * @param gameName
     *            game name
     * @throws IOException
     */
    public void createGame(String userName, String gameName) throws IOException {
        if (gameName.equals("") || games.containsKey(gameName) || games.size() >= MAX_GAMES) {
            return;
        }
        games.put(gameName, new Game(userName, gameName));
        observer.update();
    }

    /**
     * Send game list to all connected users
     * 
     * @throws IOException
     */
    public void sendGameList() throws IOException {
        server.sendAll(JSONBuilder.build("gamelist", getGameList()));
    }

    /**
     * Get list of all games
     * 
     * @return
     *         list of game names
     * @throws IOException
     */
    public Map<String, List<String>> getGameList() throws IOException {
        Map<String, List<String>> gamelist = new HashMap<>();
        Map<String, Socket> users = server.getUsers();
        games.entrySet().stream().forEach(e -> {
            List<String> players = e.getValue().getPlayerNames();
            players.removeIf(x -> {
                if (!users.containsKey(x)) {
                    try {
                        e.getValue().leaveGame(x);
                    } catch (Exception ex) {
                        // TODO:
                    }
                    return true;
                }
                return false;
            });
            gamelist.put(e.getKey(), players);
        });
        return gamelist;
    }

    /**
     * Add player to game
     * 
     * @param userName
     * @param gameName
     * @throws IOException
     */
    public void joinGame(String userName, String gameName) throws IOException {
        Game game = games.get(gameName);
        Socket player = server.getUsers().get(userName);
        game.joinGame(player);
    }

    /**
     * Remove player from game
     * 
     * @param userName
     * @param gameName
     * @throws IOException
     */
    public void leaveGame(String userName, String gameName) throws IOException {
        Game game = games.get(gameName);
        game.leaveGame(userName);
    }
}